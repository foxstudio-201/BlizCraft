package com.blizcraft.blizpocket.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Settings {
    public record SoundDef(String key, float volume, float pitch) {
    }

    private static final Map<String, SoundDef> SOUND_DEFAULTS = new LinkedHashMap<>();

    static {
        SOUND_DEFAULTS.put("bet", new SoundDef("ui.button.click", 0.8f, 1.2f));
        SOUND_DEFAULTS.put("click", new SoundDef("ui.button.click", 0.5f, 1.0f));
        SOUND_DEFAULTS.put("tick", new SoundDef("block.note_block.hat", 0.6f, 1.5f));
        SOUND_DEFAULTS.put("lock", new SoundDef("block.anvil.land", 0.7f, 1.0f));
        SOUND_DEFAULTS.put("roll", new SoundDef("block.wooden_button.click_on", 0.4f, 1.6f));
        SOUND_DEFAULTS.put("session-start", new SoundDef("block.note_block.bell", 0.8f, 1.4f));
        SOUND_DEFAULTS.put("result", new SoundDef("block.note_block.chime", 0.8f, 1.0f));
        SOUND_DEFAULTS.put("win", new SoundDef("entity.player.levelup", 0.9f, 1.2f));
        SOUND_DEFAULTS.put("lose", new SoundDef("entity.villager.no", 0.8f, 0.8f));
        SOUND_DEFAULTS.put("draw", new SoundDef("block.note_block.bass", 0.7f, 1.0f));
        SOUND_DEFAULTS.put("jackpot", new SoundDef("entity.ender_dragon.growl", 1.0f, 1.4f));
        SOUND_DEFAULTS.put("exchange", new SoundDef("entity.experience_orb.pickup", 0.7f, 1.3f));
        SOUND_DEFAULTS.put("error", new SoundDef("entity.villager.no", 0.6f, 1.0f));
        SOUND_DEFAULTS.put("leave", new SoundDef("ui.button.click", 0.5f, 0.8f));
    }

    public int saveIntervalSeconds;
    public int maxSessions;
    public boolean logTransactions;
    public boolean debug;
    public boolean updateCheckerEnabled;
    public String updateGitHubRepo;
    public boolean updateNotifyAdmins;
    public int updateTimeoutSeconds;

    public long startingBalance;
    public boolean startingBalanceEnabled;
    public int maxHistory;

    public boolean requireRoom;
    public int roomMaxPlayers;
    public boolean announceRoom;
    public String roomAnnounceScope;
    public boolean removeOnQuit;

    public int bettingSeconds;
    public int lockSeconds;
    public int rollSeconds;
    public int resultSeconds;
    public int nextRoundDelaySeconds;
    public boolean forfeitBetOnLeave;
    public int diceSides;
    public int rollIntervalTicks;
    public List<String> diceSymbols;
    public boolean titlesEnabled;
    public int titleFadeInMs;
    public int titleStayMs;
    public int titleFadeOutMs;

    public long minBet;
    public long maxBet;
    public long maxPerSession;
    public long betStep;
    public long defaultBet;
    public List<Long> betPresets;
    public List<Long> exchangePresets;

    public double jackpotContributionPercent;
    public boolean rakeEnabled;
    public double rakePercent;
    public boolean rakeToJackpot;
    public boolean houseWinOnTriple;
    public double houseRandomChancePercent;
    public double minimumWinProfitPercent;
    public boolean houseCoversShortfall;

    public boolean jackpotEnabled;
    public long jackpotStart;
    public double jackpotExplodeChancePercent;
    public long jackpotMinExplodeAmount;
    public boolean jackpotAnnounce;

    public boolean bossbarEnabled;
    public boolean bossbarCompactNumbers;
    public String bossbarOverlay;
    public Map<String, String> bossbarColors;

    public boolean soundsEnabled;
    public Map<String, SoundDef> sounds;

    public String numberFormat;
    public String dateFormat;
    public String timezone;

    public String baseName;
    public String baseSymbol;
    public boolean exchangeEnabled;
    public boolean exchangeAllowWithdraw;
    public double exchangeFeePercent;

    public void reload(FileConfiguration c) {
        saveIntervalSeconds = Math.max(10, c.getInt("storage.save-interval-seconds", 300));
        maxSessions = Math.max(20, c.getInt("storage.max-sessions", 300));
        logTransactions = c.getBoolean("storage.log-transactions", true);
        debug = c.getBoolean("storage.debug", false);
        updateCheckerEnabled = c.getBoolean("update-checker.enabled", true);
        updateGitHubRepo = c.getString("update-checker.github-repo", "foxstudio-201/BlizCraft");
        updateNotifyAdmins = c.getBoolean("update-checker.notify-admins", true);
        updateTimeoutSeconds = Math.max(3, c.getInt("update-checker.timeout-seconds", 10));

        startingBalance = Math.max(0, c.getLong("account.starting-balance", 1000));
        startingBalanceEnabled = c.getBoolean("account.starting-balance-enabled", true);
        maxHistory = Math.max(10, c.getInt("account.max-history", 100));

        requireRoom = c.getBoolean("room.require", true);
        roomMaxPlayers = Math.max(0, c.getInt("room.max-players", 0));
        announceRoom = c.getBoolean("room.announce-join-leave", true);
        roomAnnounceScope = c.getString("room.announce-scope", "room").toLowerCase(Locale.ROOT);
        if (!roomAnnounceScope.equals("server")) roomAnnounceScope = "room";
        removeOnQuit = c.getBoolean("room.remove-on-quit", true);

        bettingSeconds = Math.max(10, c.getInt("session.betting-seconds", 45));
        lockSeconds = Math.max(1, Math.min(bettingSeconds - 1, c.getInt("session.lock-seconds", 10)));
        rollSeconds = Math.max(1, c.getInt("session.roll-seconds", 4));
        resultSeconds = Math.max(1, c.getInt("session.result-seconds", 6));
        nextRoundDelaySeconds = Math.max(1, c.getInt("session.next-round-delay-seconds", 5));
        forfeitBetOnLeave = c.getBoolean("room.forfeit-bet-on-leave", true);

        diceSides = Math.max(2, Math.min(12, c.getInt("session.dice.sides", 6)));
        rollIntervalTicks = Math.max(1, c.getInt("session.dice.roll-interval-ticks", 2));
        diceSymbols = new ArrayList<>(c.getStringList("session.dice.symbols"));
        while (diceSymbols.size() < diceSides) diceSymbols.add(String.valueOf(diceSymbols.size() + 1));
        if (diceSymbols.size() > diceSides) diceSymbols = new ArrayList<>(diceSymbols.subList(0, diceSides));

        titlesEnabled = c.getBoolean("session.titles.enabled", true);
        titleFadeInMs = Math.max(0, c.getInt("session.titles.fade-in-ms", 0));
        titleStayMs = Math.max(100, c.getInt("session.titles.stay-ms", 900));
        titleFadeOutMs = Math.max(0, c.getInt("session.titles.fade-out-ms", 200));

        minBet = Math.max(1, c.getLong("betting.min", 100));
        maxBet = Math.max(minBet, c.getLong("betting.max", 1_000_000));
        maxPerSession = Math.max(0, c.getLong("betting.max-per-session", 0));
        betStep = Math.max(1, c.getLong("betting.step", 100));
        betPresets = new ArrayList<>();
        for (long preset : c.getLongList("betting.presets")) {
            if (preset > 0) betPresets.add(preset);
        }
        betPresets.sort(Comparator.naturalOrder());
        if (betPresets.size() > 9) betPresets = new ArrayList<>(betPresets.subList(0, 9));
        if (betPresets.isEmpty()) {
            betPresets.add(minBet);
            betPresets.add(Math.min(maxBet, minBet * 10));
        }
        defaultBet = Math.max(minBet, Math.min(maxBet, c.getLong("betting.default-amount", 1000)));
        exchangePresets = new ArrayList<>();
        for (long preset : c.getLongList("currencies.exchange.presets")) {
            if (preset > 0 && !exchangePresets.contains(preset)) exchangePresets.add(preset);
        }
        if (exchangePresets.size() > 4) exchangePresets = new ArrayList<>(exchangePresets.subList(0, 4));
        if (exchangePresets.isEmpty()) exchangePresets = new ArrayList<>(List.of(100L, 1_000L, 10_000L, 100_000L));

        jackpotContributionPercent = clampPercent(c.getDouble("payout.jackpot-contribution-percent", 25.0));
        rakeEnabled = c.getBoolean("payout.house-rake-enabled", true);
        rakePercent = clampPercent(c.getDouble("payout.house-rake-percent", 5.0));
        rakeToJackpot = c.getBoolean("payout.rake-to-jackpot", false);
        houseWinOnTriple = c.getBoolean("payout.house-win-on-triple", true);
        houseRandomChancePercent = clampPercent(c.getDouble("payout.house-random-chance-percent", 0.0));
        minimumWinProfitPercent = Math.max(0, c.getDouble("payout.minimum-win-profit-percent", 100.0));
        houseCoversShortfall = c.getBoolean("payout.house-covers-shortfall", true);

        jackpotEnabled = c.getBoolean("jackpot.enabled", true);
        jackpotStart = Math.max(0, c.getLong("jackpot.starting-amount", 0));
        jackpotExplodeChancePercent = clampPercent(c.getDouble("jackpot.explode-chance-percent", 3.0));
        jackpotMinExplodeAmount = Math.max(1, c.getLong("jackpot.min-explode-amount", 10_000));
        jackpotAnnounce = c.getBoolean("jackpot.announce-to-room", true);

        bossbarEnabled = c.getBoolean("bossbar.enabled", true);
        bossbarCompactNumbers = c.getBoolean("bossbar.compact-numbers", true);
        bossbarOverlay = c.getString("bossbar.overlay", "PROGRESS").toUpperCase(Locale.ROOT);
        bossbarColors = new LinkedHashMap<>();
        bossbarColors.put("betting", c.getString("bossbar.color.betting", "BLUE"));
        bossbarColors.put("locked", c.getString("bossbar.color.locked", "YELLOW"));
        bossbarColors.put("rolling", c.getString("bossbar.color.rolling", "PURPLE"));
        bossbarColors.put("result", c.getString("bossbar.color.result", "GREEN"));
        bossbarColors.put("waiting", c.getString("bossbar.color.waiting", "WHITE"));
        bossbarColors.put("idle", c.getString("bossbar.color.idle", "RED"));

        soundsEnabled = c.getBoolean("sounds.enabled", true);
        sounds = new LinkedHashMap<>();
        for (Map.Entry<String, SoundDef> e : SOUND_DEFAULTS.entrySet()) {
            String base = "sounds." + e.getKey();
            ConfigurationSection section = c.getConfigurationSection(base);
            if (section == null) {
                sounds.put(e.getKey(), e.getValue());
            } else {
                sounds.put(e.getKey(), new SoundDef(
                        section.getString("key", e.getValue().key()),
                        (float) section.getDouble("volume", e.getValue().volume()),
                        (float) section.getDouble("pitch", e.getValue().pitch())
                ));
            }
        }

        numberFormat = c.getString("language.number-format", "#,##0");
        dateFormat = c.getString("language.date-format", "dd/MM/yyyy HH:mm:ss");
        timezone = c.getString("language.timezone", "");

        baseName = c.getString("currencies.base.name", "BlizCoin");
        baseSymbol = c.getString("currencies.base.symbol", "⛁");
        exchangeEnabled = c.getBoolean("currencies.exchange.enabled", true);
        exchangeAllowWithdraw = c.getBoolean("currencies.exchange.allow-withdraw", true);
        exchangeFeePercent = clampPercent(c.getDouble("currencies.exchange.fee-percent", 0.0));
    }

    public SoundDef sound(String name) {
        return sounds.getOrDefault(name, SOUND_DEFAULTS.get(name));
    }

    private static double clampPercent(double value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
    }
}
