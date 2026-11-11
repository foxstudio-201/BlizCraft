package com.blizcraft.blizpocket.collateral;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CollateralContract {
    private final long id;
    private final UUID owner;
    private final String ownerName;
    private final Material material;
    private final String itemDisplay;
    private final long amount;
    private final List<ItemStack> items;
    private final long principal;
    private final long repayCost;
    private final long createdAt;
    private final long expiresAt;

    public CollateralContract(long id, UUID owner, String ownerName, Material material, String itemDisplay,
                              long amount, List<ItemStack> items, long principal, long repayCost,
                              long createdAt, long expiresAt) {
        this.id = id;
        this.owner = owner;
        this.ownerName = ownerName;
        this.material = material;
        this.itemDisplay = itemDisplay;
        this.amount = amount;
        this.items = new ArrayList<>(items.size());
        for (ItemStack item : items) this.items.add(item == null ? null : item.clone());
        this.principal = principal;
        this.repayCost = repayCost;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long id() {
        return id;
    }

    public UUID owner() {
        return owner;
    }

    public String ownerName() {
        return ownerName;
    }

    public Material material() {
        return material;
    }

    public String itemDisplay() {
        return itemDisplay;
    }

    public long amount() {
        return amount;
    }

    public List<ItemStack> items() {
        List<ItemStack> copy = new ArrayList<>(items.size());
        for (ItemStack item : items) copy.add(item == null ? null : item.clone());
        return copy;
    }

    public long principal() {
        return principal;
    }

    public long repayCost() {
        return repayCost;
    }

    public long createdAt() {
        return createdAt;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public boolean expired(long now) {
        return now >= expiresAt;
    }

    public long millisLeft(long now) {
        return Math.max(0, expiresAt - now);
    }
}
