package com.blizcraft.blizpocket.data;

import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.game.BetSide;
import com.blizcraft.blizpocket.game.GameResult;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class DataStore {
    private final JavaPlugin plugin;
    private final File accountsFile;
    private final File sessionsFile;
    private final File transactionsFile;
    private final Map<UUID, Account> accounts = new LinkedHashMap<>();
    private final List<SessionRecord> sessions = new ArrayList<>();
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private final AtomicBoolean saveQueued = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicLong version = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong writtenVersion = new java.util.concurrent.atomic.AtomicLong(0);
    private final Object ioLock = new Object();
    private long jackpot;
    private long nextSessionId = 1;
    private int maxSessions = 300;
    private int[] forcedDice;

    public DataStore(JavaPlugin plugin) {
        this.plugin = plugin;
        this.accountsFile = new File(plugin.getDataFolder(), "accounts.yml");
        this.sessionsFile = new File(plugin.getDataFolder(), "sessions.yml");
        this.transactionsFile = new File(plugin.getDataFolder(), "transactions.log");
    }

    public void load(Settings settings) {
        accounts.clear();
        sessions.clear();
        maxSessions = settings.maxSessions;
        jackpot = settings.jackpotStart;
        nextSessionId = 1;
        forcedDice = null;

        if (accountsFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(accountsFile);
            ConfigurationSection root = yaml.getConfigurationSection("accounts");
            if (root != null) {
                for (String key : root.getKeys(false)) {
                    UUID uuid;
                    try {
                        uuid = UUID.fromString(key);
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Bỏ qua tài khoản có UUID không hợp lệ: " + key);
                        continue;
                    }
                    ConfigurationSection sec = root.getConfigurationSection(key);
                    if (sec == null) continue;
                    String name = sec.getString("name", "Unknown");
                    long balance = sec.getLong("balance", 0);
                    Account account = new Account(uuid, name, balance, sec.getLong("joined-at", System.currentTimeMillis()));
                    account.titleEnabled(sec.getBoolean("title-enabled", true));
                    account.loadStats(
                            sec.getLong("total-bet", 0),
                            sec.getLong("total-won", 0),
                            sec.getLong("biggest-win", 0),
                            sec.getInt("wins", 0),
                            sec.getInt("losses", 0),
                            sec.getInt("draws", 0),
                            sec.getInt("streak", 0),
                            sec.getInt("best-streak", 0),
                            BetSide.parse(sec.getString("last-side", "")),
                            sec.getLong("last-amount", 0)
                    );
                    List<Map<?, ?>> history = sec.getMapList("history");
                    for (Map<?, ?> entry : history) {
                        try {
                            account.history().addLast(new PlayerSessionRecord(
                                    asLong(entry.get("session"), 0),
                                    asLong(entry.get("at"), 0),
                                    BetSide.parse(asString(entry.get("side"), "tai")),
                                    asLong(entry.get("amount"), 0),
                                    asString(entry.get("result"), "HOUSE"),
                                    asLong(entry.get("payout"), 0),
                                    asLong(entry.get("jackpot"), 0),
                                    asLong(entry.get("profit"), 0)
                            ));
                        } catch (RuntimeException ignored) {
                        }
                    }
                    accounts.put(uuid, account);
                }
            }
        }

        if (sessionsFile.exists()) {
            YamlConfiguration yaml = YamlConfiguration.loadConfiguration(sessionsFile);
            jackpot = Math.max(0, yaml.getLong("jackpot", settings.jackpotStart));
            nextSessionId = Math.max(1, yaml.getLong("next-id", 1));
            List<Integer> forced = yaml.getIntegerList("forced-dice");
            forcedDice = forced.size() == 3
                    ? new int[]{forced.get(0), forced.get(1), forced.get(2)}
                    : null;
            List<Map<?, ?>> rawSessions = yaml.getMapList("sessions");
            for (Map<?, ?> map : rawSessions) {
                try {
                    List<?> rawDice = (List<?>) map.get("dice");
                    int[] dice = new int[rawDice == null ? 0 : rawDice.size()];
                    for (int i = 0; i < dice.length; i++) dice[i] = (int) asLong(rawDice.get(i), 0);
                    List<SessionRecord.WinnerEntry> top = new ArrayList<>();
                    Object rawTop = map.get("top-winners");
                    if (rawTop instanceof List<?> topList) {
                        for (Object item : topList) {
                            if (!(item instanceof Map<?, ?> entry)) continue;
                            top.add(new SessionRecord.WinnerEntry(
                                    asString(entry.get("name"), "?"),
                                    BetSide.parse(asString(entry.get("side"), "tai")),
                                    asLong(entry.get("stake"), 0),
                                    asLong(entry.get("payout"), 0)
                            ));
                        }
                    }
                    SessionRecord record = new SessionRecord(
                            asLong(map.get("id"), 0),
                            asLong(map.get("at"), 0),
                            dice,
                            (int) asLong(map.get("total"), 0),
                            GameResult.parse(asString(map.get("result"), "HOUSE")),
                            asLong(map.get("tai-total"), 0),
                            asLong(map.get("xiu-total"), 0),
                            (int) asLong(map.get("bettors"), 0),
                            (int) asLong(map.get("winners"), 0),
                            asLong(map.get("jackpot-added"), 0),
                            asLong(map.get("jackpot-won"), 0),
                            asLong(map.get("house-cut"), 0),
                            top
                    );
                    sessions.add(record);
                    nextSessionId = Math.max(nextSessionId, record.id() + 1);
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Bỏ qua một bản ghi phiên bị lỗi: " + e.getMessage());
                }
            }
        }
        plugin.getLogger().info("Đã tải " + accounts.size() + " tài khoản, " + sessions.size() + " phiên, hũ " + jackpot + ".");
    }

    public void setMaxSessions(int maxSessions) {
        this.maxSessions = Math.max(20, maxSessions);
        while (sessions.size() > this.maxSessions) sessions.remove(sessions.size() - 1);
    }

    public Account get(UUID uuid) {
        return accounts.get(uuid);
    }

    public Account getByName(String name) {
        if (name == null) return null;
        for (Account account : accounts.values()) {
            if (account.name().equalsIgnoreCase(name)) return account;
        }
        return null;
    }

    public Account create(Player player, long startingBalance) {
        Account existing = accounts.get(player.getUniqueId());
        if (existing != null) {
            existing.name(player.getName());
            return existing;
        }
        Account account = new Account(player.getUniqueId(), player.getName(), startingBalance, System.currentTimeMillis());
        accounts.put(player.getUniqueId(), account);
        saveAsync();
        return account;
    }

    public Collection<Account> all() {
        return accounts.values();
    }

    public List<Account> top(int limit) {
        return accounts.values().stream()
                .sorted(Comparator.comparingLong((Account account) -> account.balance()).reversed())
                .limit(limit)
                .toList();
    }

    public List<SessionRecord> sessions() {
        return sessions;
    }

    public SessionRecord latest() {
        return sessions.isEmpty() ? null : sessions.get(0);
    }

    public long nextSessionId() {
        return nextSessionId++;
    }

    public long peekNextSessionId() {
        return nextSessionId;
    }

    public int[] forcedDice() {
        return forcedDice == null ? null : forcedDice.clone();
    }

    public void forcedDice(int[] dice) {
        this.forcedDice = dice == null ? null : dice.clone();
        saveAsync();
    }

    public int clearSessions() {
        int count = sessions.size();
        sessions.clear();
        saveAsync();
        return count;
    }

    public long jackpot() {
        return jackpot;
    }

    public void jackpot(long value) {
        jackpot = Math.max(0, value);
    }

    public void addJackpot(long amount) {
        jackpot = Math.max(0, jackpot + amount);
    }

    public void addSession(SessionRecord record) {
        sessions.add(0, record);
        while (sessions.size() > maxSessions) sessions.remove(sessions.size() - 1);
    }

    public void saveAsync() {
        if (!plugin.isEnabled()) {
            saveNow();
            return;
        }
        if (!saving.compareAndSet(false, true)) {
            saveQueued.set(true);
            return;
        }
        long snapshotVersion = version.incrementAndGet();
        YamlConfiguration accountsSnapshot = snapshotAccounts();
        YamlConfiguration sessionsSnapshot = snapshotSessions();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                synchronized (ioLock) {
                    if (snapshotVersion < writtenVersion.get()) return;
                    writeYaml(accountsFile, accountsSnapshot);
                    writeYaml(sessionsFile, sessionsSnapshot);
                    writtenVersion.set(snapshotVersion);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Không ghi được dữ liệu BlizPocket", e);
            } finally {
                saving.set(false);
                if (saveQueued.getAndSet(false) && plugin.isEnabled()) {
                    Bukkit.getScheduler().runTask(plugin, this::saveAsync);
                }
            }
        });
    }

    public void saveNow() {
        long snapshotVersion = version.incrementAndGet();
        YamlConfiguration accountsSnapshot = snapshotAccounts();
        YamlConfiguration sessionsSnapshot = snapshotSessions();
        try {
            synchronized (ioLock) {
                if (snapshotVersion < writtenVersion.get()) return;
                writeYaml(accountsFile, accountsSnapshot);
                writeYaml(sessionsFile, sessionsSnapshot);
                writtenVersion.set(snapshotVersion);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Không ghi được dữ liệu BlizPocket", e);
            return;
        }
        transactionsFile.getParentFile().mkdirs();
    }

    private YamlConfiguration snapshotAccounts() {
        YamlConfiguration yaml = new YamlConfiguration();
        for (Account account : accounts.values()) {
            String path = "accounts." + account.uuid();
            yaml.set(path + ".name", account.name());
            yaml.set(path + ".balance", account.balance());
            yaml.set(path + ".total-bet", account.totalBet());
            yaml.set(path + ".total-won", account.totalWon());
            yaml.set(path + ".biggest-win", account.biggestWin());
            yaml.set(path + ".wins", account.wins());
            yaml.set(path + ".losses", account.losses());
            yaml.set(path + ".draws", account.draws());
            yaml.set(path + ".streak", account.streak());
            yaml.set(path + ".best-streak", account.bestStreak());
            yaml.set(path + ".joined-at", account.joinedAt());
            yaml.set(path + ".last-side", account.lastSide() == null ? null : account.lastSide().name());
            yaml.set(path + ".last-amount", account.lastAmount());
            yaml.set(path + ".title-enabled", account.titleEnabled());
            List<Map<String, Object>> history = new ArrayList<>();
            for (PlayerSessionRecord record : account.history()) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("session", record.sessionId());
                entry.put("at", record.at());
                entry.put("side", record.side() == null ? "TAI" : record.side().name());
                entry.put("amount", record.amount());
                entry.put("result", record.result());
                entry.put("payout", record.payout());
                entry.put("jackpot", record.jackpotWin());
                entry.put("profit", record.profit());
                history.add(entry);
            }
            yaml.set(path + ".history", history);
        }
        return yaml;
    }

    private YamlConfiguration snapshotSessions() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-id", nextSessionId);
        yaml.set("jackpot", jackpot);
        if (forcedDice != null) {
            List<Integer> forced = new ArrayList<>();
            for (int value : forcedDice) forced.add(value);
            yaml.set("forced-dice", forced);
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for (SessionRecord record : sessions) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", record.id());
            entry.put("at", record.at());
            List<Integer> dice = new ArrayList<>();
            for (int value : record.dice()) dice.add(value);
            entry.put("dice", dice);
            entry.put("total", record.total());
            entry.put("result", record.result().name());
            entry.put("tai-total", record.taiTotal());
            entry.put("xiu-total", record.xiuTotal());
            entry.put("bettors", record.bettors());
            entry.put("winners", record.winners());
            entry.put("jackpot-added", record.jackpotAdded());
            entry.put("jackpot-won", record.jackpotWon());
            entry.put("house-cut", record.houseCut());
            List<Map<String, Object>> winners = new ArrayList<>();
            for (SessionRecord.WinnerEntry winner : record.topWinners()) {
                Map<String, Object> winnerMap = new LinkedHashMap<>();
                winnerMap.put("name", winner.name());
                winnerMap.put("side", winner.side() == null ? "TAI" : winner.side().name());
                winnerMap.put("stake", winner.stake());
                winnerMap.put("payout", winner.payout());
                winners.add(winnerMap);
            }
            entry.put("top-winners", winners);
            list.add(entry);
        }
        yaml.set("sessions", list);
        return yaml;
    }

    private void writeYaml(File file, YamlConfiguration yaml) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        String data = yaml.saveToString();
        synchronized (ioLock) {
            Path target = file.toPath();
            Path temp = target.resolveSibling(file.getName() + ".tmp");
            Files.writeString(temp, data, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    public void logTransaction(String message) {
        if (!plugin.isEnabled()) {
            appendTransaction(message);
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> appendTransaction(message));
    }

    private void appendTransaction(String message) {
        try {
            File parent = transactionsFile.getParentFile();
            if (parent != null) parent.mkdirs();
            String line = "[" + java.time.Instant.now() + "] " + message + System.lineSeparator();
            synchronized (ioLock) {
                Files.writeString(transactionsFile.toPath(), line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Không ghi được transactions.log: " + e.getMessage());
        }
    }

    private static long asLong(Object value, long def) {
        if (value instanceof Number number) return number.longValue();
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    private static String asString(Object value, String def) {
        return value == null ? def : String.valueOf(value);
    }

    public int accountCount() {
        return accounts.size();
    }

    public String debugName() {
        return "accounts=" + accounts.size() + ", sessions=" + sessions.size() + ", jackpot=" + jackpot;
    }

    public List<UUID> uuidsByName(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        List<UUID> result = new ArrayList<>();
        for (Account account : accounts.values()) {
            if (account.name().toLowerCase(Locale.ROOT).contains(lower)) result.add(account.uuid());
        }
        return result;
    }
}
