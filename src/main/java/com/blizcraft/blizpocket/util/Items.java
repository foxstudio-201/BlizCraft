package com.blizcraft.blizpocket.util;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Items {
    private Items() {
    }

    public static ItemStack make(Material material, String name, List<String> lore, Map<String, String> placeholders) {
        return make(material, name, lore, placeholders, 1);
    }

    public static ItemStack make(Material material, String name, List<String> lore, Map<String, String> placeholders, int amount) {
        ItemStack item = new ItemStack(material == null ? Material.STONE : material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Text.mm(name == null ? "" : name, placeholders));
            if (lore != null && !lore.isEmpty()) {
                List<Component> lines = new ArrayList<>(lore.size());
                for (String line : lore) lines.add(Text.mm(line, placeholders));
                meta.lore(lines);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack filler(Material material) {
        return make(material, "<black> </black>", null, null);
    }
}
