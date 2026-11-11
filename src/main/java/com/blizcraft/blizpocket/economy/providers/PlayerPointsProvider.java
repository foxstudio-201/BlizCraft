package com.blizcraft.blizpocket.economy.providers;

import com.blizcraft.blizpocket.economy.CurrencyProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.UUID;

public final class PlayerPointsProvider implements CurrencyProvider {
    private final String display;
    private final Material icon;
    private final double rate;
    private Object api;
    private Method getPoints;
    private Method takePoints;
    private Method givePoints;
    private boolean initialized;

    public PlayerPointsProvider(String display, Material icon, double rate) {
        this.display = display;
        this.icon = icon;
        this.rate = rate;
    }

    private boolean init() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PlayerPoints");
        if (plugin == null || !plugin.isEnabled()) {
            initialized = false;
            api = null;
            return false;
        }
        if (initialized && api != null && getPoints != null && takePoints != null && givePoints != null) return true;
        initialized = true;
        try {
            Method getApi = plugin.getClass().getMethod("getAPI");
            api = getApi.invoke(plugin);
            if (api == null) return false;
            for (Method method : api.getClass().getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length == 1 && params[0] == UUID.class && (method.getName().equals("getPoints") || method.getName().equals("look") || method.getName().equals("getBalance"))) {
                    getPoints = method;
                } else if (params.length == 2 && params[0] == UUID.class && params[1] == int.class) {
                    if (method.getName().startsWith("take")) takePoints = method;
                    if (method.getName().startsWith("give")) givePoints = method;
                }
            }
        } catch (Throwable ignored) {
            api = null;
            getPoints = null;
            takePoints = null;
            givePoints = null;
        }
        return api != null && getPoints != null && takePoints != null && givePoints != null;
    }

    @Override
    public String id() {
        return "playerpoints";
    }

    @Override
    public String displayName() {
        return display;
    }

    @Override
    public Material icon() {
        return icon;
    }

    @Override
    public double rate() {
        return rate;
    }

    @Override
    public boolean available() {
        return init();
    }

    @Override
    public long balance(Player player) {
        if (!init()) return 0;
        try {
            Object value = getPoints.invoke(api, player.getUniqueId());
            return value instanceof Number number ? number.longValue() : 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        return change(player, amount, true);
    }

    @Override
    public boolean deposit(Player player, long amount) {
        return change(player, amount, false);
    }

    private boolean change(Player player, long amount, boolean take) {
        if (!init() || amount <= 0 || amount > Integer.MAX_VALUE) return false;
        try {
            Method method = take ? takePoints : givePoints;
            Object result = method.invoke(api, player.getUniqueId(), (int) amount);
            return !(result instanceof Boolean bool) || bool;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String note() {
        return "PlayerPoints";
    }
}
