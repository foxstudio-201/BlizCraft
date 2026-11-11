package com.blizcraft.blizpocket.collateral;

import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.data.DataStore;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.TimeFmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class CollateralManager {
    private final JavaPlugin plugin;
    private final Settings settings;
    private final Messages messages;
    private final DataStore data;
    private final File file;
    private final Map<Material, AllowedItem> allowed = new LinkedHashMap<>();
    private final Map<UUID, LinkedHashMap<Long, CollateralContract>> contracts = new LinkedHashMap<>();
    private final AtomicBoolean saving = new AtomicBoolean(false);
    private final AtomicBoolean saveQueued = new AtomicBoolean(false);
    private final java.util.concurrent.atomic.AtomicLong version = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong writtenVersion = new java.util.concurrent.atomic.AtomicLong(0);
    private final Object ioLock = new Object();
    private boolean enabled;
    private double durationHours;
    private double interestPercent;
    private double enchantBonusPercentPerLevel;
    private long minPayout;
    private int maxContracts;
    private int expireCheckSeconds;
    private long nextId = 1;
    private BukkitTask task;

    public CollateralManager(JavaPlugin plugin, Settings settings, Messages messages, DataStore data) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        this.data = data;
        this.file = new File(plugin.getDataFolder(), "collateral.yml");
        load();
        reloadConfig();
        expireDue();
    }

    public void reloadConfig() {
        FileConfiguration config = plugin.getConfig();
        ConfigurationSection root = config.getConfigurationSection("collateral");
        enabled = root != null && root.getBoolean("enabled", true);
        durationHours = Math.max(0.001, root == null ? 24.0 : root.getDouble("duration-hours", 24.0));
        interestPercent = Math.max(0, root == null ? 10.0 : root.getDouble("interest-percent", 10.0));
        enchantBonusPercentPerLevel = Math.max(0, root == null ? 10.0 : root.getDouble("enchant-bonus-percent-per-level", 10.0));
        minPayout = Math.max(1, root == null ? 100 : root.getLong("min-payout", 100));
        maxContracts = Math.max(1, root == null ? 3 : root.getInt("max-contracts-per-player", 3));
        expireCheckSeconds = Math.max(5, root == null ? 60 : root.getInt("expire-check-seconds", 60));
        allowed.clear();
        if (root != null) {
            ConfigurationSection items = root.getConfigurationSection("allowed-items");
            if (items != null) {
                for (String key : items.getKeys(false)) {
                    ConfigurationSection entry = items.getConfigurationSection(key);
                    if (entry == null) continue;
                    Material material = Material.matchMaterial(key);
                    if (material == null) {
                        plugin.getLogger().warning("Bỏ qua vật phẩm thế chấp không hợp lệ: " + key);
                        continue;
                    }
                    double rate = entry.getDouble("rate", 0);
                    if (rate <= 0) {
                        plugin.getLogger().warning("Bỏ qua vật phẩm thế chấp có rate <= 0: " + key);
                        continue;
                    }
                    Material icon = Material.matchMaterial(entry.getString("icon", key));
                    allowed.put(material, new AllowedItem(
                            material,
                            entry.getString("display", key),
                            icon == null ? material : icon,
                            rate
                    ));
                }
            }
        }
        startTask();
        plugin.getLogger().info("Thế chấp: " + (enabled ? "bật" : "tắt") + ", " + allowed.size() + " loại item, " + contracts.size() + " người có hợp đồng.");
    }

    private void startTask() {
        if (task != null) task.cancel();
        if (!enabled && contracts.isEmpty()) return;
        long ticks = Math.max(20L, expireCheckSeconds * 20L);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::expireDue, ticks, ticks);
    }

    public boolean enabled() {
        return enabled;
    }

    public List<AllowedItem> allowedItems() {
        return new ArrayList<>(allowed.values());
    }

    public AllowedItem allowed(String key) {
        if (key == null) return null;
        Material material = Material.matchMaterial(key);
        if (material == null) material = Material.matchMaterial(key.toUpperCase(Locale.ROOT));
        if (material != null && allowed.containsKey(material)) return allowed.get(material);
        String lower = key.toLowerCase(Locale.ROOT);
        for (AllowedItem item : allowed.values()) {
            if (item.display().toLowerCase(Locale.ROOT).equals(lower)) return item;
        }
        return null;
    }

    public AllowedItem allowed(Material material) {
        return allowed.get(material);
    }

    public List<CollateralContract> contracts(Player player) {
        return contracts(player.getUniqueId());
    }

    public List<CollateralContract> contracts(UUID uuid) {
        expireDue();
        Map<Long, CollateralContract> map = contracts.get(uuid);
        return map == null ? List.of() : new ArrayList<>(map.values());
    }

    public CollateralContract contract(UUID uuid, long id) {
        Map<Long, CollateralContract> map = contracts.get(uuid);
        return map == null ? null : map.get(id);
    }

    public int contractCount(UUID uuid) {
        Map<Long, CollateralContract> map = contracts.get(uuid);
        return map == null ? 0 : map.size();
    }

    public long estimateValue(Player player, AllowedItem allowedItem) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == allowedItem.material()) stacks.add(item);
        }
        return valueOf(stacks, allowedItem.rate());
    }

    public long valueOf(List<ItemStack> stacks, double rate) {
        long total = 0;
        for (ItemStack item : stacks) {
            if (item == null) continue;
            int levels = enchantLevels(item);
            double multiplier = 1.0 + (levels * enchantBonusPercentPerLevel / 100.0);
            total += (long) Math.floor(item.getAmount() * rate * multiplier);
        }
        return total;
    }

    public int enchantLevels(ItemStack item) {
        if (item == null) return 0;
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasEnchants()) return 0;
        int levels = 0;
        for (int level : meta.getEnchants().values()) levels += level;
        return levels;
    }

    public long countItem(Player player, Material material) {
        long count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count;
    }

    public CollateralContract pledge(Player player, AllowedItem allowedItem, long amount) {
        if (!player.hasPermission("bliztaixiu.collateral")) {
            messages.send(player, "general.no-permission");
            return null;
        }
        if (!enabled) {
            messages.send(player, "collateral.disabled");
            return null;
        }
        Account account = data.get(player.getUniqueId());
        if (account == null) {
            messages.send(player, "account.required");
            return null;
        }
        if (allowedItem == null) {
            messages.send(player, "collateral.not-allowed", Messages.ph("item", "?"));
            return null;
        }
        if (amount <= 0 || countItem(player, allowedItem.material()) < amount) {
            messages.send(player, "collateral.not-enough-items", Messages.ph(
                    "amount", Numbers.format(Math.max(0, amount)),
                    "item", allowedItem.display()
            ));
            return null;
        }
        if (contractCount(player.getUniqueId()) >= maxContracts) {
            messages.send(player, "collateral.max-contracts", Messages.ph("max", maxContracts));
            return null;
        }
        List<ItemStack> taken = takeItems(player, allowedItem.material(), amount);
        if (taken == null) {
            messages.send(player, "collateral.not-enough-items", Messages.ph(
                    "amount", Numbers.format(amount),
                    "item", allowedItem.display()
            ));
            return null;
        }
        long principal = valueOf(taken, allowedItem.rate());
        if (principal < minPayout) {
            giveItems(player, taken);
            messages.send(player, "collateral.min-payout", Messages.ph(
                    "min", Numbers.format(minPayout),
                    "symbol", settings.baseSymbol
            ));
            return null;
        }
        long now = System.currentTimeMillis();
        long expiresAt = now + Math.max(1000L, (long) (durationHours * 3_600_000.0));
        long repayCost = principal + (long) Math.ceil(principal * interestPercent / 100.0);
        CollateralContract contract = new CollateralContract(
                nextId++, player.getUniqueId(), player.getName(), allowedItem.material(),
                allowedItem.display(), amount, taken, principal, repayCost, now, expiresAt
        );
        contracts.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedHashMap<>()).put(contract.id(), contract);
        account.addBalance(principal);
        data.saveAsync();
        saveAsync();
        messages.send(player, "collateral.pledged", Messages.ph(
                "amount", Numbers.format(amount),
                "item", allowedItem.display(),
                "coins", Numbers.format(principal),
                "repay", Numbers.format(repayCost),
                "expires", TimeFmt.format(expiresAt, settings.dateFormat, settings.timezone),
                "symbol", settings.baseSymbol
        ));
        play(player, "exchange");
        if (settings.logTransactions) {
            data.logTransaction(player.getName() + " collateral pledge id=" + contract.id()
                    + " " + amount + "x " + allowedItem.material().name() + " principal=" + principal);
        }
        return contract;
    }

    public boolean repay(Player player, long id) {
        if (!player.hasPermission("bliztaixiu.collateral")) {
            messages.send(player, "general.no-permission");
            return false;
        }
        CollateralContract contract = contract(player.getUniqueId(), id);
        if (contract == null) {
            messages.send(player, "collateral.not-found", Messages.ph("id", id));
            return false;
        }
        Account account = data.get(player.getUniqueId());
        if (account == null) {
            messages.send(player, "account.required");
            return false;
        }
        if (account.balance() < contract.repayCost()) {
            messages.send(player, "collateral.insufficient-repay", Messages.ph(
                    "cost", Numbers.format(contract.repayCost()),
                    "balance", Numbers.format(account.balance()),
                    "symbol", settings.baseSymbol
            ));
            return false;
        }
        account.addBalance(-contract.repayCost());
        boolean full = giveItems(player, contract.items());
        Map<Long, CollateralContract> map = contracts.get(player.getUniqueId());
        if (map != null) {
            map.remove(id);
            if (map.isEmpty()) contracts.remove(player.getUniqueId());
        }
        data.saveAsync();
        saveAsync();
        messages.send(player, "collateral.repaid", Messages.ph(
                "cost", Numbers.format(contract.repayCost()),
                "amount", Numbers.format(contract.amount()),
                "item", contract.itemDisplay(),
                "symbol", settings.baseSymbol
        ));
        if (!full) messages.send(player, "collateral.inventory-full");
        play(player, "exchange");
        if (settings.logTransactions) {
            data.logTransaction(player.getName() + " collateral repay id=" + id
                    + " cost=" + contract.repayCost() + " items=" + contract.amount() + "x " + contract.material().name());
        }
        return true;
    }

    public int clear(UUID owner) {
        Map<Long, CollateralContract> map = contracts.remove(owner);
        if (map == null || map.isEmpty()) return 0;
        saveAsync();
        return map.size();
    }

    public void expireDue() {
        long now = System.currentTimeMillis();
        boolean changed = false;
        List<CollateralContract> expired = new ArrayList<>();
        for (Map.Entry<UUID, LinkedHashMap<Long, CollateralContract>> entry : new ArrayList<>(contracts.entrySet())) {
            LinkedHashMap<Long, CollateralContract> map = entry.getValue();
            for (CollateralContract contract : new ArrayList<>(map.values())) {
                if (contract.expired(now)) {
                    map.remove(contract.id());
                    expired.add(contract);
                    changed = true;
                }
            }
            if (map.isEmpty()) contracts.remove(entry.getKey());
        }
        for (CollateralContract contract : expired) {
            Player player = Bukkit.getPlayer(contract.owner());
            if (player != null) {
                messages.send(player, "collateral.expired", Messages.ph(
                        "id", contract.id(),
                        "amount", Numbers.format(contract.amount()),
                        "item", contract.itemDisplay()
                ));
            }
            if (settings.logTransactions) {
                data.logTransaction("EXPIRED collateral id=" + contract.id() + " owner=" + contract.ownerName()
                        + " items=" + contract.amount() + "x " + contract.material().name());
            }
        }
        if (changed) saveAsync();
    }

    private List<ItemStack> takeItems(Player player, Material material, long amount) {
        if (countItem(player, material) < amount) return null;
        List<ItemStack> taken = new ArrayList<>();
        long remaining = amount;
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            int take = (int) Math.min(item.getAmount(), remaining);
            ItemStack copy = item.clone();
            copy.setAmount(take);
            taken.add(copy);
            remaining -= take;
            int left = item.getAmount() - take;
            if (left <= 0) {
                player.getInventory().setItem(i, null);
            } else {
                item.setAmount(left);
                player.getInventory().setItem(i, item);
            }
        }
        return remaining == 0 ? taken : null;
    }

    private boolean giveItems(Player player, List<ItemStack> items) {
        boolean full = true;
        for (ItemStack item : items) {
            if (item == null) continue;
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
                full = false;
            }
        }
        return full;
    }

    private void play(Player player, String sound) {
        if (!settings.soundsEnabled) return;
        Settings.SoundDef def = settings.sound(sound);
        if (def == null || def.key() == null || def.key().isBlank()) return;
        try {
            player.playSound(player.getLocation(), def.key(), def.volume(), def.pitch());
        } catch (Throwable ignored) {
        }
    }

    public long durationHoursRounded() {
        return Math.max(1, Math.round(durationHours));
    }

    public double durationHours() {
        return durationHours;
    }

    public double interestPercent() {
        return interestPercent;
    }

    public double enchantBonusPercentPerLevel() {
        return enchantBonusPercentPerLevel;
    }

    public long minPayout() {
        return minPayout;
    }

    public int maxContracts() {
        return maxContracts;
    }

    public String formatExpires(long expiresAt) {
        return TimeFmt.format(expiresAt, settings.dateFormat, settings.timezone);
    }

    private void load() {
        if (!file.exists()) return;
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        nextId = Math.max(1, yaml.getLong("next-id", 1));
        ConfigurationSection root = yaml.getConfigurationSection("contracts");
        if (root == null) return;
        for (String uuidKey : root.getKeys(false)) {
            UUID owner;
            try {
                owner = UUID.fromString(uuidKey);
            } catch (IllegalArgumentException e) {
                continue;
            }
            ConfigurationSection ownerSection = root.getConfigurationSection(uuidKey);
            if (ownerSection == null) continue;
            for (String idKey : ownerSection.getKeys(false)) {
                ConfigurationSection section = ownerSection.getConfigurationSection(idKey);
                if (section == null) continue;
                try {
                    long id = section.getLong("id", Long.parseLong(idKey));
                    Material material = Material.matchMaterial(section.getString("material", ""));
                    if (material == null) continue;
                    long amount = section.getLong("amount", 0);
                    long principal = section.getLong("principal", 0);
                    long repay = section.getLong("repay-cost", principal);
                    long created = section.getLong("created-at", System.currentTimeMillis());
                    long expires = section.getLong("expires-at", created);
                    String ownerName = section.getString("owner-name", "?");
                    String display = section.getString("item-display", material.name());
                    List<ItemStack> items = new ArrayList<>();
                    List<?> raw = section.getList("items");
                    if (raw != null) {
                        for (Object value : raw) {
                            if (value instanceof ItemStack stack) items.add(stack);
                        }
                    }
                    if (items.isEmpty()) {
                        long remaining = Math.max(1, amount);
                        int maxStack = Math.max(1, material.getMaxStackSize());
                        while (remaining > 0) {
                            int stack = (int) Math.min(maxStack, remaining);
                            items.add(new ItemStack(material, stack));
                            remaining -= stack;
                        }
                    }
                    CollateralContract contract = new CollateralContract(id, owner, ownerName, material, display,
                            amount, items, principal, repay, created, expires);
                    contracts.computeIfAbsent(owner, ignored -> new LinkedHashMap<>()).put(id, contract);
                    nextId = Math.max(nextId, id + 1);
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("Bỏ qua hợp đồng thế chấp lỗi: " + e.getMessage());
                }
            }
        }
    }

    private YamlConfiguration snapshot() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-id", nextId);
        for (Map.Entry<UUID, LinkedHashMap<Long, CollateralContract>> entry : contracts.entrySet()) {
            for (CollateralContract contract : entry.getValue().values()) {
                String path = "contracts." + entry.getKey() + "." + contract.id();
                yaml.set(path + ".id", contract.id());
                yaml.set(path + ".owner-name", contract.ownerName());
                yaml.set(path + ".material", contract.material().name());
                yaml.set(path + ".item-display", contract.itemDisplay());
                yaml.set(path + ".amount", contract.amount());
                yaml.set(path + ".principal", contract.principal());
                yaml.set(path + ".repay-cost", contract.repayCost());
                yaml.set(path + ".created-at", contract.createdAt());
                yaml.set(path + ".expires-at", contract.expiresAt());
                yaml.set(path + ".items", contract.items());
            }
        }
        return yaml;
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
        YamlConfiguration yaml = snapshot();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                synchronized (ioLock) {
                    if (snapshotVersion < writtenVersion.get()) return;
                    write(yaml);
                    writtenVersion.set(snapshotVersion);
                }
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Không ghi được collateral.yml", e);
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
        try {
            synchronized (ioLock) {
                if (snapshotVersion < writtenVersion.get()) return;
                write(snapshot());
                writtenVersion.set(snapshotVersion);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Không ghi được collateral.yml", e);
        }
    }

    private void write(YamlConfiguration yaml) throws IOException {
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
}
