package com.blizcraft.blizpocket.collateral;

import org.bukkit.Material;

public record AllowedItem(Material material, String display, Material icon, double rate) {
    public String id() {
        return material.name();
    }
}
