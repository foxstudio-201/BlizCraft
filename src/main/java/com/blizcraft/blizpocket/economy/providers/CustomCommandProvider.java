package com.blizcraft.blizpocket.economy.providers;

import com.blizcraft.blizpocket.economy.CurrencyProvider;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.Locale;

public final class CustomCommandProvider implements CurrencyProvider {
    private final String key;
    private final String display;
    private final Material icon;
    private final double rate;
    private final String balancePlaceholder;
    private final String withdrawCommand;
    private final String depositCommand;
    private Method setPlaceholders;
    private boolean papiChecked;

    public CustomCommandProvider(String key, String display, Material icon, double rate,
                                 String balancePlaceholder, String withdrawCommand, String depositCommand) {
        this.key = key;
        this.display = display;
        this.icon = icon;
        this.rate = rate;
        this.balancePlaceholder = balancePlaceholder;
        this.withdrawCommand = withdrawCommand;
        this.depositCommand = depositCommand;
    }

    private boolean initPapi() {
        if (papiChecked && setPlaceholders != null) return true;
        papiChecked = true;
        Plugin papi = Bukkit.getPluginManager().getPlugin("PlaceholderAPI");
        if (papi == null || !papi.isEnabled()) return false;
        try {
            Class<?> api = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            setPlaceholders = api.getMethod("setPlaceholders", Player.class, String.class);
            return true;
        } catch (Throwable ignored) {
            setPlaceholders = null;
            return false;
        }
    }

    @Override
    public String id() {
        return "custom:" + key;
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
        return initPapi()
                && balancePlaceholder != null && !balancePlaceholder.isBlank()
                && withdrawCommand != null && !withdrawCommand.isBlank()
                && depositCommand != null && !depositCommand.isBlank();
    }

    @Override
    public long balance(Player player) {
        if (!initPapi()) return 0;
        try {
            Object result = setPlaceholders.invoke(null, player, balancePlaceholder);
            if (result == null) return 0;
            String cleaned = String.valueOf(result).replaceAll("[^0-9.-]", "");
            if (cleaned.isBlank() || cleaned.equals("-") || cleaned.equals(".")) return 0;
            return (long) Math.floor(Double.parseDouble(cleaned));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        if (amount <= 0 || balance(player) < amount) return false;
        return dispatch(withdrawCommand, player, amount);
    }

    @Override
    public boolean deposit(Player player, long amount) {
        if (amount <= 0) return false;
        return dispatch(depositCommand, player, amount);
    }

    private boolean dispatch(String template, Player player, long amount) {
        if (template == null || template.isBlank()) return false;
        String command = template
                .replace("{player}", player.getName())
                .replace("%player%", player.getName())
                .replace("{amount}", Long.toString(amount))
                .replace("%amount%", Long.toString(amount));
        try {
            return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String note() {
        return "custom:" + key.toLowerCase(Locale.ROOT);
    }
}
