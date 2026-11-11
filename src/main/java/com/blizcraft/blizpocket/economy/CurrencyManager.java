package com.blizcraft.blizpocket.economy;

import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.data.DataStore;
import com.blizcraft.blizpocket.economy.providers.CustomCommandProvider;
import com.blizcraft.blizpocket.economy.providers.ExperienceProvider;
import com.blizcraft.blizpocket.economy.providers.ItemProvider;
import com.blizcraft.blizpocket.economy.providers.PlayerPointsProvider;
import com.blizcraft.blizpocket.economy.providers.VaultProvider;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CurrencyManager {
    private final JavaPlugin plugin;
    private final Settings settings;
    private final DataStore data;
    private final List<CurrencyProvider> providers = new ArrayList<>();
    private final Map<String, CurrencyProvider> lookup = new LinkedHashMap<>();

    public CurrencyManager(JavaPlugin plugin, Settings settings, DataStore data) {
        this.plugin = plugin;
        this.settings = settings;
        this.data = data;
        reload();
    }

    public void reload() {
        providers.clear();
        lookup.clear();
        FileConfiguration config = plugin.getConfig();

        ConfigurationSection vault = config.getConfigurationSection("currencies.vault");
        if (vault != null && vault.getBoolean("enabled", true)) {
            register(new VaultProvider(
                    vault.getString("display", "Xu (Vault)"),
                    material(vault.getString("icon", "GOLD_INGOT"), Material.GOLD_INGOT),
                    rate(vault.getDouble("rate", 1.0), "vault")
            ));
            aliases("vault", "eco", "economy", "essentials", "xu");
        }

        ConfigurationSection points = config.getConfigurationSection("currencies.playerpoints");
        if (points != null && points.getBoolean("enabled", true)) {
            register(new PlayerPointsProvider(
                    points.getString("display", "PlayerPoints"),
                    material(points.getString("icon", "AMETHYST_SHARD"), Material.AMETHYST_SHARD),
                    rate(points.getDouble("rate", 100.0), "playerpoints")
            ));
            aliases("playerpoints", "points", "point", "pp", "playerpoint");
        }

        ConfigurationSection xp = config.getConfigurationSection("currencies.experience");
        if (xp != null && xp.getBoolean("enabled", false)) {
            register(new ExperienceProvider(
                    xp.getString("display", "Cấp kinh nghiệm"),
                    material(xp.getString("icon", "EXPERIENCE_BOTTLE"), Material.EXPERIENCE_BOTTLE),
                    rate(xp.getDouble("rate", 500.0), "experience")
            ));
            aliases("experience", "exp", "xp", "level", "levels", "cap");
        }

        ConfigurationSection items = config.getConfigurationSection("currencies.items");
        if (items != null && items.getBoolean("enabled", true)) {
            ConfigurationSection entries = items.getConfigurationSection("entries");
            if (entries != null) {
                for (String key : entries.getKeys(false)) {
                    ConfigurationSection entry = entries.getConfigurationSection(key);
                    if (entry == null) continue;
                    Material material = Material.matchMaterial(key);
                    if (material == null) {
                        plugin.getLogger().warning("Bỏ qua item currency không hợp lệ: " + key);
                        continue;
                    }
                    ItemProvider provider = new ItemProvider(
                            material,
                            entry.getString("display", key),
                            rate(entry.getDouble("rate", 1.0), key)
                    );
                    register(provider);
                    lookup.put(provider.id().toLowerCase(Locale.ROOT), provider);
                    lookup.put(key.toLowerCase(Locale.ROOT), provider);
                    lookup.put(material.name().toLowerCase(Locale.ROOT), provider);
                }
            }
        }

        ConfigurationSection custom = config.getConfigurationSection("currencies.custom");
        if (custom != null && custom.getBoolean("enabled", false)) {
            ConfigurationSection entries = custom.getConfigurationSection("entries");
            if (entries != null) {
                for (String key : entries.getKeys(false)) {
                    ConfigurationSection entry = entries.getConfigurationSection(key);
                    if (entry == null) continue;
                    CustomCommandProvider provider = new CustomCommandProvider(
                            key,
                            entry.getString("display", key),
                            material(entry.getString("icon", "EMERALD"), Material.EMERALD),
                            rate(entry.getDouble("rate", 1.0), key),
                            entry.getString("balance-placeholder", ""),
                            entry.getString("withdraw-command", ""),
                            entry.getString("deposit-command", "")
                    );
                    register(provider);
                    lookup.put(provider.id().toLowerCase(Locale.ROOT), provider);
                    lookup.put(key.toLowerCase(Locale.ROOT), provider);
                }
            }
        }

        long available = providers.stream().filter(CurrencyProvider::available).count();
        plugin.getLogger().info("Đã nạp " + providers.size() + " loại tiền, khả dụng: " + available + ".");
    }

    private void aliases(String id, String... names) {
        CurrencyProvider provider = lookup.get(id);
        if (provider == null) return;
        for (String name : names) lookup.put(name, provider);
    }

    private void register(CurrencyProvider provider) {
        providers.add(provider);
        lookup.put(provider.id().toLowerCase(Locale.ROOT), provider);
    }

    private static Material material(String raw, Material fallback) {
        Material material = raw == null ? null : Material.matchMaterial(raw);
        return material == null ? fallback : material;
    }

    private double rate(double value, String id) {
        if (value <= 0) {
            plugin.getLogger().warning("Tỉ giá của " + id + " <= 0, dùng 1.0.");
            return 1.0;
        }
        return value;
    }

    public CurrencyProvider get(String id) {
        if (id == null) return null;
        String key = id.trim().toLowerCase(Locale.ROOT);
        CurrencyProvider provider = lookup.get(key);
        if (provider != null) return provider;
        if (key.startsWith("item:")) {
            Material material = Material.matchMaterial(key.substring(5).toUpperCase(Locale.ROOT));
            if (material != null) return lookup.get("item:" + material.name().toLowerCase(Locale.ROOT));
        }
        return null;
    }

    public List<CurrencyProvider> providers() {
        return List.copyOf(providers);
    }

    public List<CurrencyProvider> availableProviders() {
        List<CurrencyProvider> result = new ArrayList<>();
        for (CurrencyProvider provider : providers) {
            if (provider.available()) result.add(provider);
        }
        return result;
    }

    public ExchangeResult depositToBase(Player player, Account account, String id, long amount) {
        if (!settings.exchangeEnabled) return ExchangeResult.fail("exchange.disabled");
        CurrencyProvider provider = get(id);
        if (provider == null) return ExchangeResult.fail("general.invalid-currency");
        if (!provider.available()) return ExchangeResult.fail("exchange.unavailable");
        if (amount <= 0) return ExchangeResult.fail("exchange.invalid-amount");
        long coins = (long) Math.floor(amount * provider.rate() * feeFactor());
        if (coins <= 0) return ExchangeResult.fail("exchange.invalid-amount");
        if (provider.balance(player) < amount) return ExchangeResult.fail("exchange.insufficient");
        if (!provider.withdraw(player, amount)) return ExchangeResult.fail("exchange.insufficient");

        account.addBalance(coins);
        log(player, "NAP " + amount + " " + provider.id() + " -> " + coins + " BlizCoin");
        return ExchangeResult.ok(coins, amount, provider.displayName());
    }

    public ExchangeResult withdrawFromBase(Player player, Account account, String id, long amount) {
        if (!settings.exchangeEnabled) return ExchangeResult.fail("exchange.disabled");
        if (!settings.exchangeAllowWithdraw) return ExchangeResult.fail("exchange.withdraw-disabled");
        CurrencyProvider provider = get(id);
        if (provider == null) return ExchangeResult.fail("general.invalid-currency");
        if (!provider.available()) return ExchangeResult.fail("exchange.unavailable");
        if (amount <= 0) return ExchangeResult.fail("exchange.invalid-amount");
        double factor = feeFactor();
        if (provider.rate() <= 0 || factor <= 0) return ExchangeResult.fail("exchange.unavailable");
        long cost = (long) Math.ceil(amount * provider.rate() / factor - 1.0e-9);
        if (cost <= 0) return ExchangeResult.fail("exchange.invalid-amount");
        if (account.balance() < cost) return ExchangeResult.fail("exchange.insufficient");

        account.addBalance(-cost);
        if (!provider.deposit(player, amount)) {
            account.addBalance(cost);
            return ExchangeResult.fail("exchange.insufficient");
        }
        log(player, "RUT " + cost + " BlizCoin -> " + amount + " " + provider.id());
        return ExchangeResult.ok(cost, amount, provider.displayName());
    }

    public long maxForeignFromBase(Account account, CurrencyProvider provider) {
        return (long) Math.floor(account.balance() * feeFactor() / provider.rate());
    }

    public double feeFactor() {
        return 1.0 - (settings.exchangeFeePercent / 100.0);
    }

    public List<String> availableIds() {
        List<String> ids = new ArrayList<>();
        for (CurrencyProvider provider : providers) {
            if (provider.available()) ids.add(provider.id());
        }
        return ids;
    }

    public void refresh() {
        for (CurrencyProvider provider : providers) {
            if (provider instanceof VaultProvider vault) vault.refresh();
        }
    }

    private void log(Player player, String message) {
        if (settings.logTransactions) data.logTransaction(player.getName() + ": " + message);
    }
}
