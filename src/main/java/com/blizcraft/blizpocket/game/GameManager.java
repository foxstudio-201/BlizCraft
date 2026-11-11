package com.blizcraft.blizpocket.game;

import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.data.Bet;
import com.blizcraft.blizpocket.data.DataStore;
import com.blizcraft.blizpocket.data.SessionRecord;
import com.blizcraft.blizpocket.gui.GuiManager;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.Text;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class GameManager {
    private final JavaPlugin plugin;
    private final Settings settings;
    private final Messages messages;
    private final DataStore data;
    private final Random random = new Random();
    private final Set<UUID> room = new LinkedHashSet<>();
    private final Set<UUID> bossbarShown = new HashSet<>();
    private final BossBar bossbar;

    private GuiManager guiManager;
    private Session session;
    private BukkitTask tickTask;
    private BukkitTask saveTask;
    private long lastRollAt;
    private int[] pendingDice;
    private boolean stopped;
    private float lastProgress = -1;

    public GameManager(JavaPlugin plugin, Settings settings, Messages messages, DataStore data) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.data = data;
        this.bossbar = BossBar.bossBar(messages.comp("bossbar.idle"), 0f, color("idle"), overlay());
    }

    public void setGuiManager(GuiManager guiManager) {
        this.guiManager = guiManager;
    }

    public void start() {
        stopped = false;
        lastProgress = -1;
        bossbar.overlay(overlay());
        startTasks();
        startSession();
    }

    private void startTasks() {
        if (tickTask != null) tickTask.cancel();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 2L);
        if (saveTask != null) saveTask.cancel();
        long interval = Math.max(10, settings.saveIntervalSeconds) * 20L;
        saveTask = Bukkit.getScheduler().runTaskTimer(plugin, data::saveAsync, interval, interval);
    }

    public void reloadSettings() {
        data.setMaxSessions(settings.maxSessions);
        bossbar.overlay(overlay());
        startTasks();
        updateBossbars(System.currentTimeMillis());
    }

    private void tick() {
        if (stopped) return;
        long now = System.currentTimeMillis();
        if (session == null) {
            hideAllBossbars();
            return;
        }
        if (now >= session.phaseEndsAt()) advance(now);
        if (session == null) return;

        long seconds = session.secondsLeft(now);
        boolean locked = session.phase() == Session.Phase.BETTING
                && seconds <= settings.lockSeconds && seconds > 0;
        if (locked && !session.lockAnnounced()) {
            session.lockAnnounced(true);
            session.lastAnnouncedSecond(Long.MIN_VALUE);
            sendRoom("session.locked", ph("session", session.id()));
            playRoom("lock");
            updateBossbars(now);
            if (guiManager != null) guiManager.tickOpen();
        }
        if (seconds != session.lastBossbarSecond()) {
            session.lastBossbarSecond(seconds);
            updateBossbars(now);
            if (guiManager != null) guiManager.tickOpen();
        }
        handleCountdown(seconds, locked);
        if (session.phase() == Session.Phase.ROLLING) animateRoll(now);
    }

    private void advance(long now) {
        if (session == null) return;
        switch (session.phase()) {
            case BETTING -> {
                pendingDice = Dice.roll(settings.diceSides, 3);
                session.setPhase(Session.Phase.ROLLING, now, settings.rollSeconds * 1000L);
                session.dice(Dice.roll(settings.diceSides, 3));
                lastRollAt = 0;
                sendRoom("session.rolling", ph("session", session.id()));
                updateBossbars(now);
                animateRoll(now);
            }
            case ROLLING -> {
                int[] finalDice = pendingDice == null ? Dice.roll(settings.diceSides, 3) : pendingDice;
                session.dice(finalDice);
                session.setPhase(Session.Phase.RESULT, now, settings.resultSeconds * 1000L);
                resolve(now);
                updateBossbars(now);
            }
            case RESULT -> {
                session.setPhase(Session.Phase.WAITING, now, settings.nextRoundDelaySeconds * 1000L);
                updateBossbars(now);
            }
            case WAITING, IDLE -> startSession();
        }
    }

    private void handleCountdown(long seconds, boolean locked) {
        if (session == null || seconds <= 0) return;
        Session.Phase phase = session.phase();
        if (locked) {
            if (seconds <= settings.lockSeconds && session.lastAnnouncedSecond() != seconds) {
                session.lastAnnouncedSecond(seconds);
                Component main = messages.comp("title.lock-main", ph("time", seconds));
                Component sub = messages.comp("title.lock-sub");
                for (Player player : eligiblePlayers()) {
                    sendTitle(player, main, sub, settings.titleStayMs);
                    play(player, "tick");
                }
            }
        } else if (phase == Session.Phase.WAITING) {
            long safeSeconds = Math.min(seconds, settings.nextRoundDelaySeconds);
            if (safeSeconds <= settings.lockSeconds && session.lastAnnouncedSecond() != safeSeconds) {
                session.lastAnnouncedSecond(safeSeconds);
                Component main = messages.comp("title.waiting-main", ph("time", safeSeconds));
                Component sub = messages.comp("title.waiting-sub");
                for (Player player : eligiblePlayers()) {
                    sendTitle(player, main, sub, settings.titleStayMs);
                    play(player, "tick");
                }
            }
        }
    }

    private void animateRoll(long now) {
        if (session == null || session.phase() != Session.Phase.ROLLING) return;
        long interval = Math.max(50L, settings.rollIntervalTicks * 50L);
        if (now - lastRollAt < interval) return;
        lastRollAt = now;
        session.dice(Dice.roll(settings.diceSides, 3));
        Component main = Text.mm("<white>" + session.diceText(settings.diceSymbols.toArray(new String[0])) + "</white>");
        Component sub = messages.comp("title.rolling-sub");
        for (Player player : eligiblePlayers()) {
            sendTitle(player, main, sub, 250);
            play(player, "roll");
        }
    }

    private void startSession() {
        long now = System.currentTimeMillis();
        pendingDice = null;
        lastRollAt = 0;
        session = new Session(data.nextSessionId());
        session.setPhase(Session.Phase.BETTING, now, settings.bettingSeconds * 1000L);
        data.saveAsync();
        data.saveAsync();
        session.dice(new int[]{1, 1, 1});
        if (!room.isEmpty()) {
            sendRoom("session.start", ph("session", session.id(), "time", settings.bettingSeconds));
            playRoom("session-start");
        }
        updateBossbars(now);
        if (guiManager != null) guiManager.tickOpen();
    }

    private void resolve(long now) {
        if (session == null || session.resolved()) return;
        Session current = session;
        int[] dice = current.dice();
        PayoutCalculator.Outcome outcome = PayoutCalculator.calculate(current.bets(), dice, settings, random);
        current.result(outcome.result());
        current.winnersCount(outcome.winnersCount());
        current.jackpotAdded(outcome.jackpotAdded());
        current.houseCut(outcome.houseCut());
        current.resolved(true);
        current.dice(dice);

        long pot = data.jackpot() + outcome.jackpotAdded()
                + (settings.rakeToJackpot ? outcome.houseCut() : 0);
        Map<UUID, Long> jackpotWins = Map.of();
        long jackpotWon = 0;
        if (settings.jackpotEnabled
                && outcome.winnersCount() > 0
                && pot >= settings.jackpotMinExplodeAmount
                && settings.jackpotExplodeChancePercent > 0
                && random.nextDouble() * 100.0 < settings.jackpotExplodeChancePercent) {
            jackpotWins = PayoutCalculator.distribute(pot, outcome.winningStakes());
            jackpotWon = pot;
            data.jackpot(0);
        } else {
            data.jackpot(pot);
        }
        current.jackpotWon(jackpotWon);
        if (settings.logTransactions && outcome.houseTopUp() > 0) {
            data.logTransaction("HOUSE TOP-UP session=" + current.id() + " amount=" + outcome.houseTopUp());
        }

        for (Bet bet : current.bets().values()) {
            Account account = data.get(bet.playerId());
            if (account == null) continue;
            long payout = outcome.payouts().getOrDefault(bet.playerId(), 0L);
            long jackpotWin = jackpotWins.getOrDefault(bet.playerId(), 0L);
            account.addBalance(payout + jackpotWin);
            account.recordOutcome(current.id(), now, bet.side(), bet.amount(), outcome.result(),
                    payout, jackpotWin, settings.maxHistory);
            if (settings.logTransactions) {
                data.logTransaction(bet.playerName() + " session=" + current.id()
                        + " side=" + bet.side().name() + " stake=" + bet.amount()
                        + " result=" + outcome.result().name() + " payout=" + payout
                        + " jackpot=" + jackpotWin);
            }
        }

        List<SessionRecord.WinnerEntry> topWinners = new ArrayList<>();
        for (Bet bet : current.bets().values()) {
            long payout = outcome.payouts().getOrDefault(bet.playerId(), 0L)
                    + jackpotWins.getOrDefault(bet.playerId(), 0L);
            if (payout <= 0) continue;
            topWinners.add(new SessionRecord.WinnerEntry(bet.playerName(), bet.side(), bet.amount(), payout));
        }
        topWinners.sort((a, b) -> Long.compare(b.payout(), a.payout()));
        if (topWinners.size() > 10) topWinners = new ArrayList<>(topWinners.subList(0, 10));

        SessionRecord record = new SessionRecord(
                current.id(), now, dice, current.total(), outcome.result(),
                current.sideTotal(BetSide.TAI), current.sideTotal(BetSide.XIU),
                current.bettorCount(), outcome.winnersCount(),
                outcome.jackpotAdded(), jackpotWon, outcome.houseCut(), topWinners
        );
        data.addSession(record);
        data.saveAsync();

        announceResult(current, outcome, jackpotWins, jackpotWon, now);
    }

    private void announceResult(Session current, PayoutCalculator.Outcome outcome,
                                Map<UUID, Long> jackpotWins, long jackpotWon, long now) {
        String result = messages.raw(outcome.result().messageKey());
        String dice = current.diceText(settings.diceSymbols.toArray(new String[0]));
        Map<String, String> base = ph(
                "session", current.id(),
                "result", result,
                "dice", dice,
                "total", current.total(),
                "tai", Numbers.format(current.sideTotal(BetSide.TAI)),
                "xiu", Numbers.format(current.sideTotal(BetSide.XIU)),
                "winners", outcome.winnersCount(),
                "jackpot", Numbers.format(jackpotWon > 0 ? jackpotWon : data.jackpot()),
                "symbol", settings.baseSymbol
        );
        if (outcome.houseWin()) {
            sendRoom("result.room-house", base);
        } else {
            sendRoom("result.room-summary", base);
            sendRoom("result.room-summary-detail", base);
        }
        if (jackpotWon > 0) {
            sendRoom("jackpot.exploded", base);
            if (settings.jackpotAnnounce) playRoom("jackpot");
        }
        playRoom("result");

        Component resultMain = messages.comp(outcome.result().messageKey());
        Component genericSub = messages.comp("title.result-sub", ph("total", current.total(), "dice", dice));
        int stay = (int) Math.max(1000, Math.min(5000, settings.resultSeconds * 1000L));

        for (Player player : eligiblePlayers()) {
            Bet bet = current.bets().get(player.getUniqueId());
            if (bet == null) {
                sendTitle(player, resultMain, genericSub, stay);
                continue;
            }
            long payout = outcome.payouts().getOrDefault(player.getUniqueId(), 0L);
            long jackpotWin = jackpotWins.getOrDefault(player.getUniqueId(), 0L);
            long received = payout + jackpotWin;
            long loss = Math.max(0, bet.amount() - received);
            Map<String, String> personal = ph(
                    "amount", Numbers.format(bet.amount()),
                    "payout", Numbers.format(received),
                    "profit", Numbers.format(Math.max(0, received - bet.amount())),
                    "loss", Numbers.format(loss),
                    "symbol", settings.baseSymbol,
                    "jackpot", Numbers.format(jackpotWin),
                    "side", messages.raw(bet.side().messageKey())
            );
            String key;
            String sound;
            if (outcome.houseWin()) {
                key = "result.house-lose";
                sound = "lose";
            } else if (received > bet.amount()) {
                key = "result.win";
                sound = "win";
            } else if (received == bet.amount()) {
                key = "result.draw";
                sound = "draw";
            } else {
                key = "result.lose";
                sound = "lose";
            }
            Component personalSub = messages.comp(key, personal);
            messages.send(player, key, personal);
            if (jackpotWin > 0) {
                messages.send(player, "jackpot.won", ph("amount", Numbers.format(jackpotWin), "symbol", settings.baseSymbol));
                sendTitle(player, messages.comp("title.jackpot-main"), messages.comp("title.jackpot-sub",
                        ph("amount", Numbers.format(jackpotWin), "symbol", settings.baseSymbol)), 3500);
                play(player, "jackpot");
            } else {
                sendTitle(player, resultMain, personalSub, stay);
                play(player, sound);
            }
        }
    }

    public boolean placeBet(Player player, BetSide side, long amount) {
        if (!player.hasPermission("bliztaixiu.play")) {
            messages.send(player, "general.no-permission");
            return false;
        }
        if (session == null || session.phase() != Session.Phase.BETTING
                || session.secondsLeft(System.currentTimeMillis()) <= settings.lockSeconds) {
            messages.send(player, "bet.locked");
            play(player, "error");
            return false;
        }
        Account account = data.get(player.getUniqueId());
        if (account == null) {
            messages.send(player, "bet.no-account");
            play(player, "error");
            return false;
        }
        if (settings.requireRoom && !room.contains(player.getUniqueId())) {
            messages.send(player, "bet.no-room");
            play(player, "error");
            return false;
        }
        UUID uuid = player.getUniqueId();
        if (session.hasParticipated(uuid)) {
            messages.send(player, "bet.already", ph("session", session.id()));
            play(player, "error");
            return false;
        }
        if (amount < settings.minBet) {
            messages.send(player, "bet.below-min", ph("min", Numbers.format(settings.minBet), "symbol", settings.baseSymbol));
            play(player, "error");
            return false;
        }
        boolean bypass = player.hasPermission("bliztaixiu.bypass.bet-limit");
        if (!bypass && amount > settings.maxBet) {
            messages.send(player, "bet.above-max", ph("max", Numbers.format(settings.maxBet), "symbol", settings.baseSymbol));
            play(player, "error");
            return false;
        }
        if (!bypass && settings.maxPerSession > 0 && amount > settings.maxPerSession) {
            messages.send(player, "bet.session-max", ph("max", Numbers.format(settings.maxPerSession), "symbol", settings.baseSymbol));
            play(player, "error");
            return false;
        }
        if (account.balance() < amount) {
            messages.send(player, "bet.insufficient", ph(
                    "amount", Numbers.format(amount),
                    "balance", Numbers.format(account.balance()),
                    "symbol", settings.baseSymbol
            ));
            play(player, "error");
            return false;
        }

        String sideText = messages.raw(side.messageKey());
        account.addBalance(-amount);
        account.addTotalBet(amount);
        session.addBet(new Bet(uuid, player.getName(), side, amount));
        account.lastBet(side, amount);
        messages.send(player, "bet.placed", ph(
                "amount", Numbers.format(amount),
                "side", sideText,
                "symbol", settings.baseSymbol
        ));

        if (settings.logTransactions) {
            data.logTransaction(player.getName() + " bet session=" + session.id()
                    + " side=" + side.name() + " amount=" + amount
                    + " balance=" + account.balance());
        }
        data.saveAsync();
        play(player, "bet");
        updateBossbars(System.currentTimeMillis());
        if (guiManager != null) guiManager.tickOpen();
        return true;
    }

    public Account createAccount(Player player) {
        if (!player.hasPermission("bliztaixiu.account")) {
            messages.send(player, "general.no-permission");
            return null;
        }
        Account existing = data.get(player.getUniqueId());
        if (existing != null) {
            existing.name(player.getName());
            messages.send(player, "account.exists");
            return existing;
        }
        long bonus = settings.startingBalanceEnabled ? settings.startingBalance : 0;
        Account account = data.create(player, bonus);
        if (bonus > 0) {
            messages.send(player, "account.created", ph(
                    "balance", Numbers.format(account.balance()),
                    "symbol", settings.baseSymbol
            ));
        } else {
            messages.send(player, "account.created-no-bonus");
        }
        if (settings.logTransactions) data.logTransaction(player.getName() + " created account, bonus=" + bonus);
        play(player, "exchange");
        return account;
    }

    public boolean joinRoom(Player player) {
        if (!player.hasPermission("bliztaixiu.play")) {
            messages.send(player, "general.no-permission");
            return false;
        }
        Account account = data.get(player.getUniqueId());
        if (account == null) {
            messages.send(player, "account.required");
            play(player, "error");
            return false;
        }
        if (room.contains(player.getUniqueId())) {
            messages.send(player, "room.already");
            return true;
        }
        if (settings.roomMaxPlayers > 0 && room.size() >= settings.roomMaxPlayers) {
            messages.send(player, "room.full");
            play(player, "error");
            return false;
        }
        room.add(player.getUniqueId());
        account.name(player.getName());
        messages.send(player, "room.joined", ph("count", room.size()));
        play(player, "exchange");
        announceJoin(player, room.size());
        updateBossbars(System.currentTimeMillis());
        return true;
    }

    public void leaveRoom(Player player) {
        if (!room.remove(player.getUniqueId())) {
            messages.send(player, "room.not-in");
            return;
        }
        forfeitCurrentBet(player, true);
        hideBossbar(player.getUniqueId());
        messages.send(player, "room.left", ph("count", room.size()));
        play(player, "leave");
        announceLeave(player, room.size());
    }

    public void handleQuit(Player player) {
        forfeitCurrentBet(player, false);
        hideBossbar(player.getUniqueId());
        UUID uuid = player.getUniqueId();
        Account account = data.get(uuid);
        if (account != null) account.name(player.getName());
        if (settings.removeOnQuit && room.remove(uuid)) {
            announceLeave(player, room.size());
        }
    }

    private void announceJoin(Player player, int count) {
        if (!settings.announceRoom) return;
        Map<String, String> placeholders = ph("player", player.getName(), "count", count);
        if ("server".equals(settings.roomAnnounceScope)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) messages.send(online, "room.join-announce", placeholders);
            }
            return;
        }
        for (Player other : eligiblePlayers()) {
            if (!other.equals(player)) messages.send(other, "room.join-announce", placeholders);
        }
    }

    private void announceLeave(Player player, int count) {
        if (!settings.announceRoom) return;
        Map<String, String> placeholders = ph("player", player.getName(), "count", count);
        if ("server".equals(settings.roomAnnounceScope)) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (!online.equals(player)) messages.send(online, "room.leave-announce", placeholders);
            }
            return;
        }
        for (Player other : eligiblePlayers()) {
            if (!other.equals(player)) messages.send(other, "room.leave-announce", placeholders);
        }
    }

    private void forfeitCurrentBet(Player player, boolean notify) {
        if (!settings.forfeitBetOnLeave || session == null || session.resolved()) return;
        Bet bet = session.forfeit(player.getUniqueId());
        if (bet == null) return;
        data.addJackpot(bet.amount());
        Account account = data.get(player.getUniqueId());
        if (account != null) {
            account.recordForfeit(session.id(), System.currentTimeMillis(), bet.side(), bet.amount(), settings.maxHistory);
        }
        if (notify) {
            messages.send(player, "bet.forfeited", ph(
                    "amount", Numbers.format(bet.amount()),
                    "symbol", settings.baseSymbol
            ));
            play(player, "lose");
        }
        if (settings.logTransactions) {
            data.logTransaction(player.getName() + " forfeited session=" + session.id()
                    + " side=" + bet.side().name() + " amount=" + bet.amount());
        }
        data.saveAsync();
        updateBossbars(System.currentTimeMillis());
        if (guiManager != null) guiManager.tickOpen();
    }

    public void handleJoin(Player player) {
        Account account = data.get(player.getUniqueId());
        if (account != null) {
            account.name(player.getName());
            if (room.contains(player.getUniqueId())) updateBossbars(System.currentTimeMillis());
        }
    }

    public boolean forceEnd() {
        if (session == null) return false;
        long now = System.currentTimeMillis();
        if (session.resolved()) {
            session.setPhase(Session.Phase.WAITING, now, settings.nextRoundDelaySeconds * 1000L);
            updateBossbars(now);
            return true;
        }
        session.dice(Dice.roll(settings.diceSides, 3));
        session.setPhase(Session.Phase.RESULT, now, settings.resultSeconds * 1000L);
        resolve(now);
        updateBossbars(now);
        return true;
    }

    public boolean cancelSession() {
        if (session == null || session.resolved()) return false;
        long now = System.currentTimeMillis();
        for (Bet bet : session.bets().values()) {
            Account account = data.get(bet.playerId());
            if (account == null) continue;
            account.addBalance(bet.amount());
            if (settings.logTransactions) {
                data.logTransaction(bet.playerName() + " refund session=" + session.id() + " amount=" + bet.amount());
            }
            Player player = Bukkit.getPlayer(bet.playerId());
            if (player != null && room.contains(player.getUniqueId())) {
                messages.send(player, "session.refunded", ph(
                        "amount", Numbers.format(bet.amount()),
                        "symbol", settings.baseSymbol
                ));
            }
        }
        session.bets().clear();
        session.resolved(true);
        session.setPhase(Session.Phase.WAITING, now, settings.nextRoundDelaySeconds * 1000L);
        sendRoom("session.cancelled", ph());
        data.saveAsync();
        updateBossbars(now);
        if (guiManager != null) guiManager.tickOpen();
        return true;
    }

    public boolean startNow() {
        if (session != null) {
            Session.Phase phase = session.phase();
            if (phase == Session.Phase.BETTING || phase == Session.Phase.ROLLING) {
                return false;
            }
        }
        startSession();
        return true;
    }

    public void shutdown() {
        stopped = true;
        if (tickTask != null) tickTask.cancel();
        if (saveTask != null) saveTask.cancel();
        if (session != null && !session.resolved() && !session.bets().isEmpty()) {
            for (Bet bet : session.bets().values()) {
                Account account = data.get(bet.playerId());
                if (account == null) continue;
                account.addBalance(bet.amount());
                Player player = Bukkit.getPlayer(bet.playerId());
                if (player != null && room.contains(player.getUniqueId())) {
                    messages.send(player, "session.refunded", ph(
                            "amount", Numbers.format(bet.amount()),
                            "symbol", settings.baseSymbol
                    ));
                }
            }
            data.logTransaction("SERVER STOP: refunded session " + session.id());
            session.bets().clear();
            session.resolved(true);
        }
        hideAllBossbars();
        room.clear();
        data.saveNow();
    }

    private void updateBossbars(long now) {
        if (session == null || !settings.bossbarEnabled) {
            hideAllBossbars();
            return;
        }
        String dice = session.diceText(settings.diceSymbols.toArray(new String[0]));
        String result = session.result() == null ? "" : messages.raw(session.result().messageKey());
        long seconds = session.secondsLeft(now);
        boolean locked = session.phase() == Session.Phase.BETTING && seconds <= settings.lockSeconds && seconds > 0;
        String key = locked ? "bossbar.locked" : switch (session.phase()) {
            case BETTING -> "bossbar.betting";
            case ROLLING -> "bossbar.rolling";
            case RESULT -> "bossbar.result";
            case WAITING -> "bossbar.waiting";
            case IDLE -> "bossbar.idle";
        };
        Map<String, String> placeholders = ph(
                "session", session.id(),
                "time", seconds,
                "jackpot", settings.bossbarCompactNumbers ? Numbers.compact(data.jackpot()) : Numbers.format(data.jackpot()),
                "tai", settings.bossbarCompactNumbers ? Numbers.compact(session.sideTotal(BetSide.TAI)) : Numbers.format(session.sideTotal(BetSide.TAI)),
                "xiu", settings.bossbarCompactNumbers ? Numbers.compact(session.sideTotal(BetSide.XIU)) : Numbers.format(session.sideTotal(BetSide.XIU)),
                "result", result,
                "dice", dice,
                "total", session.total(),
                "symbol", settings.baseSymbol
        );
        bossbar.name(messages.comp(key, placeholders));
        bossbar.color(color(locked ? "locked" : session.phase().key()));
        bossbar.overlay(overlay());
        float progress = session.phase() == Session.Phase.RESULT ? 1f : session.progress(now);
        lastProgress = progress;
        bossbar.progress(progress);

        for (Player player : Bukkit.getOnlinePlayers()) {
            boolean eligible = settings.bossbarEnabled
                    && room.contains(player.getUniqueId())
                    && data.get(player.getUniqueId()) != null;
            if (eligible) showBossbar(player);
            else hideBossbar(player.getUniqueId());
        }
        bossbarShown.removeIf(uuid -> {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline()) return true;
            return !room.contains(uuid) || data.get(uuid) == null;
        });
    }

    private void showBossbar(Player player) {
        if (!settings.bossbarEnabled) return;
        if (bossbarShown.add(player.getUniqueId())) {
            player.showBossBar(bossbar);
        }
    }

    private void hideBossbar(UUID uuid) {
        if (bossbarShown.remove(uuid)) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) player.hideBossBar(bossbar);
        }
    }

    private void hideAllBossbars() {
        for (UUID uuid : new ArrayList<>(bossbarShown)) hideBossbar(uuid);
    }

    private void sendTitle(Player player, Component main, Component sub, int stayMs) {
        if (!settings.titlesEnabled) return;
        Account account = data.get(player.getUniqueId());
        if (account == null || !account.titleEnabled()) return;
        Title title = Title.title(main, sub, Title.Times.times(
                Duration.ofMillis(settings.titleFadeInMs),
                Duration.ofMillis(Math.max(100, stayMs)),
                Duration.ofMillis(settings.titleFadeOutMs)
        ));
        player.showTitle(title);
    }

    private void sendRoom(String key, Map<String, String> placeholders) {
        for (Player player : eligiblePlayers()) messages.send(player, key, placeholders);
    }

    private void playRoom(String sound) {
        for (Player player : eligiblePlayers()) play(player, sound);
    }

    public boolean setTitleEnabled(Player player, boolean enabled) {
        Account account = data.get(player.getUniqueId());
        if (account == null) {
            messages.send(player, "account.required");
            return false;
        }
        account.titleEnabled(enabled);
        data.saveAsync();
        messages.send(player, enabled ? "title.enabled" : "title.disabled");
        play(player, "click");
        if (!enabled) player.clearTitle();
        return true;
    }

    public boolean toggleTitle(Player player) {
        Account account = data.get(player.getUniqueId());
        return account != null && setTitleEnabled(player, !account.titleEnabled());
    }

    public void play(Player player, String sound) {
        if (!settings.soundsEnabled) return;
        Settings.SoundDef def = settings.sound(sound);
        if (def == null || def.key() == null || def.key().isBlank()) return;
        try {
            player.playSound(player.getLocation(), def.key(), def.volume(), def.pitch());
        } catch (Throwable ignored) {
        }
    }

    public List<Player> eligiblePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID uuid : room) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null && player.isOnline() && data.get(uuid) != null) result.add(player);
        }
        return result;
    }

    private BossBar.Color color(String phaseKey) {
        String raw = settings.bossbarColors.getOrDefault(phaseKey, "BLUE");
        try {
            return BossBar.Color.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossBar.Color.BLUE;
        }
    }

    private BossBar.Overlay overlay() {
        try {
            return BossBar.Overlay.valueOf(settings.bossbarOverlay.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return BossBar.Overlay.PROGRESS;
        }
    }

    private Map<String, String> ph(Object... pairs) {
        return Messages.ph(pairs);
    }

    public Session session() {
        return session;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public DataStore data() {
        return data;
    }

    public boolean isInRoom(Player player) {
        return room.contains(player.getUniqueId());
    }

    public int roomSize() {
        return room.size();
    }

    public Set<UUID> roomIds() {
        return Set.copyOf(room);
    }

    public String formatDice(int[] dice) {
        StringBuilder builder = new StringBuilder();
        for (int value : dice) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(value >= 1 && value <= settings.diceSymbols.size()
                    ? settings.diceSymbols.get(value - 1) : String.valueOf(value));
        }
        return builder.toString();
    }
}
