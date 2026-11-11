package com.blizcraft.blizpocket.economy.providers;

import com.blizcraft.blizpocket.economy.CurrencyProvider;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class VaultProvider implements CurrencyProvider {
    private final String display;
    private final Material icon;
    private final double rate;
    private Economy economy;

    public VaultProvider(String display, Material icon, double rate) {
        this.display = display;
        this.icon = icon;
        this.rate = rate;
        refresh();
    }

    public void refresh() {
        try {
            RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
            economy = registration == null ? null : registration.getProvider();
        } catch (Throwable ignored) {
            economy = null;
        }
    }

    private Economy economy() {
        if (economy == null) refresh();
        return economy;
    }

    @Override
    public String id() {
        return "vault";
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
        return economy() != null;
    }

    @Override
    public long balance(Player player) {
        Economy eco = economy();
        if (eco == null) return 0;
        return (long) Math.floor(eco.getBalance(player));
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        Economy eco = economy();
        if (eco == null || amount <= 0) return false;
        EconomyResponse response = eco.withdrawPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public boolean deposit(Player player, long amount) {
        Economy eco = economy();
        if (eco == null || amount <= 0) return false;
        EconomyResponse response = eco.depositPlayer(player, amount);
        return response != null && response.transactionSuccess();
    }

    @Override
    public String note() {
        return "Vault";
    }
}
