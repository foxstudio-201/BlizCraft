package com.blizcraft.blizpocket.economy;

import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface CurrencyProvider {
    String id();

    String displayName();

    Material icon();

    double rate();

    boolean available();

    long balance(Player player);

    boolean withdraw(Player player, long amount);

    boolean deposit(Player player, long amount);

    default void refresh() {
    }

    default String note() {
        return "";
    }
}
