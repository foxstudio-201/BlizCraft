package com.blizcraft.blizpocket.listener;

import com.blizcraft.blizpocket.gui.BetGui;
import com.blizcraft.blizpocket.gui.CollateralGui;
import com.blizcraft.blizpocket.gui.HistoryGui;
import com.blizcraft.blizpocket.gui.RoadGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class GuiListener implements Listener {
    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        InventoryHolder holder = top.getHolder();
        if (!(holder instanceof BetGui) && !(holder instanceof HistoryGui) && !(holder instanceof RoadGui) && !(holder instanceof CollateralGui)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        Inventory clicked = event.getClickedInventory();
        if (clicked == null || clicked != top) return;
        int slot = event.getRawSlot();
        if (holder instanceof BetGui betGui) {
            betGui.handleClick(slot, event.getClick());
        } else if (holder instanceof HistoryGui historyGui) {
            historyGui.handleClick(slot);
        } else if (holder instanceof RoadGui roadGui) {
            roadGui.handleClick(slot);
        } else if (holder instanceof CollateralGui collateralGui) {
            collateralGui.handleClick(slot);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (holder instanceof BetGui || holder instanceof HistoryGui || holder instanceof RoadGui || holder instanceof CollateralGui) {
            event.setCancelled(true);
        }
    }
}
