package com.blizcraft.blizpocket.gui;

import com.blizcraft.blizpocket.collateral.AllowedItem;
import com.blizcraft.blizpocket.collateral.CollateralContract;
import com.blizcraft.blizpocket.collateral.CollateralManager;
import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.util.Items;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.TimeFmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public final class CollateralGui implements InventoryHolder {
    private static final int ALLOWED_START = 0;
    private static final int ALLOWED_LIMIT = 18;
    private static final int CONTRACT_START = 27;
    private static final int CONTRACT_LIMIT = 18;

    private final GuiManager gui;
    private final Player player;
    private final Inventory inventory;

    public CollateralGui(GuiManager gui, Player player) {
        this.gui = gui;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, gui.messages().comp("gui.collateral.title"));
    }

    public void open() {
        update();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void update() {
        Messages messages = gui.messages();
        CollateralManager manager = gui.collateral();
        for (int i = 0; i < 54; i++) inventory.setItem(i, Items.filler(Material.BLACK_STAINED_GLASS_PANE));

        List<AllowedItem> allowed = manager.allowedItems();
        for (int i = 0; i < ALLOWED_LIMIT && i < allowed.size(); i++) {
            AllowedItem item = allowed.get(i);
            long count = manager.countItem(player, item.material());
            long max = manager.estimateValue(player, item);
            var ph = Messages.ph(
                    "item", item.display(),
                    "rate", Numbers.format(item.rate()),
                    "count", Numbers.format(count),
                    "max", Numbers.format(max),
                    "hours", manager.durationHours(),
                    "interest", format(manager.interestPercent()),
                    "enchant", format(manager.enchantBonusPercentPerLevel()),
                    "symbol", gui.settings().baseSymbol
            );
            inventory.setItem(ALLOWED_START + i, Items.make(item.icon(),
                    messages.raw("gui.collateral.allowed-name", ph),
                    messages.rawLore("gui.collateral.allowed-lore"), ph));
        }
        if (allowed.isEmpty()) {
            inventory.setItem(4, Items.make(Material.BARRIER, messages.raw("gui.collateral.empty-allowed"), null, null));
        }

        List<CollateralContract> contracts = manager.contracts(player);
        long now = System.currentTimeMillis();
        for (int i = 0; i < CONTRACT_LIMIT && i < contracts.size(); i++) {
            CollateralContract contract = contracts.get(i);
            var ph = Messages.ph(
                    "id", contract.id(),
                    "amount", Numbers.format(contract.amount()),
                    "item", contract.itemDisplay(),
                    "principal", Numbers.format(contract.principal()),
                    "repay", Numbers.format(contract.repayCost()),
                    "expires", manager.formatExpires(contract.expiresAt()),
                    "time_left", TimeFmt.duration(contract.millisLeft(now)),
                    "symbol", gui.settings().baseSymbol
            );
            inventory.setItem(CONTRACT_START + i, Items.make(contract.material(),
                    messages.raw("gui.collateral.contract-name", ph),
                    messages.rawLore("gui.collateral.contract-lore"), ph));
        }
        if (contracts.isEmpty()) {
            inventory.setItem(31, Items.make(Material.BARRIER, messages.raw("gui.collateral.empty-contracts"), null, null));
        }

        inventory.setItem(45, Items.make(Material.KNOWLEDGE_BOOK,
                messages.raw("gui.collateral.info-name"), messages.rawLore("gui.collateral.info-lore"), null));
        inventory.setItem(48, Items.make(Material.EMERALD, messages.raw("gui.collateral.refresh-name"), null, null));
        inventory.setItem(50, Items.make(Material.ARROW, messages.raw("gui.collateral.back-name"), null, null));
        inventory.setItem(53, Items.make(Material.BARRIER, messages.raw("gui.collateral.close-name"), null, null));
    }

    public void handleClick(int slot) {
        gui.game().play(player, "click");
        int allowedIndex = slot - ALLOWED_START;
        if (allowedIndex >= 0 && allowedIndex < ALLOWED_LIMIT) {
            List<AllowedItem> allowed = gui.collateral().allowedItems();
            if (allowedIndex < allowed.size()) {
                gui.openPledgeDialog(player, allowed.get(allowedIndex));
            }
            return;
        }
        int contractIndex = slot - CONTRACT_START;
        if (contractIndex >= 0 && contractIndex < CONTRACT_LIMIT) {
            List<CollateralContract> contracts = gui.collateral().contracts(player);
            if (contractIndex < contracts.size()) {
                gui.openRepayDialog(player, contracts.get(contractIndex));
            }
            return;
        }
        if (slot == 48) {
            update();
        } else if (slot == 50) {
            gui.openMainMenu(player);
        } else if (slot == 53) {
            player.closeInventory();
        }
    }

    private static String format(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(java.util.Locale.US, "%.2f", value);
    }
}
