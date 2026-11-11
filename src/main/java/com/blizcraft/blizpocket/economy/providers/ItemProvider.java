package com.blizcraft.blizpocket.economy.providers;

import com.blizcraft.blizpocket.economy.CurrencyProvider;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.Map;

public final class ItemProvider implements CurrencyProvider {
    private final Material material;
    private final String display;
    private final double rate;

    public ItemProvider(Material material, String display, double rate) {
        this.material = material;
        this.display = display;
        this.rate = rate;
    }

    @Override
    public String id() {
        return "item:" + material.name();
    }

    @Override
    public String displayName() {
        return display;
    }

    @Override
    public Material icon() {
        return material;
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
        long count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.getType() == material) count += item.getAmount();
        }
        return count;
    }

    @Override
    public boolean withdraw(Player player, long amount) {
        if (amount <= 0) return false;
        PlayerInventory inventory = player.getInventory();
        if (balance(player) < amount) return false;
        long remaining = amount;
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length && remaining > 0; i++) {
            ItemStack item = contents[i];
            if (item == null || item.getType() != material) continue;
            int take = (int) Math.min(item.getAmount(), remaining);
            int left = item.getAmount() - take;
            remaining -= take;
            if (left <= 0) {
                inventory.setItem(i, null);
            } else {
                item.setAmount(left);
                inventory.setItem(i, item);
            }
        }
        return remaining == 0;
    }

    @Override
    public boolean deposit(Player player, long amount) {
        if (amount <= 0) return false;
        long remaining = amount;
        int maxStack = Math.max(1, material.getMaxStackSize());
        while (remaining > 0) {
            int stack = (int) Math.min(maxStack, remaining);
            ItemStack item = new ItemStack(material, stack);
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item);
            for (ItemStack drop : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
            remaining -= stack;
        }
        return true;
    }

    @Override
    public String note() {
        return material.name();
    }
}
