package com.blizcraft.blizpocket.economy.providers;

import com.blizcraft.blizpocket.economy.CurrencyProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public final class ExperienceProvider implements CurrencyProvider {
    private final String display;
    private final Material icon;
    private final double rate;

    public ExperienceProvider(String display, Material icon, double rate) {
        this.display = display;
        this.icon = icon;
        this.rate = rate;
    }

    @Override
    public String id() {
        return "experience";
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
        return true;
    }

    @Override
    public long balance(Player player) {
        return player.getLevel();
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        if (amount <= 0 || player.getLevel() < amount) return false;
        player.setLevel((int) Math.max(0, player.getLevel() - amount));
        return true;
    }

    @Override
    public boolean deposit(Player player, long amount) {
        if (amount <= 0 || amount > Integer.MAX_VALUE) return false;
        player.giveExpLevels((int) amount);
        return true;
    }

    @Override
    public String note() {
        return "XP levels";
    }
}
