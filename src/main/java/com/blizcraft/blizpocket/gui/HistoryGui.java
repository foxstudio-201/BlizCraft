package com.blizcraft.blizpocket.gui;

import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.SessionRecord;
import com.blizcraft.blizpocket.util.Items;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.TimeFmt;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public final class HistoryGui implements InventoryHolder {
    public static final int PAGE_SIZE = 45;
    private final GuiManager gui;
    private final Player player;
    private final Inventory inventory;
    private final int page;
    private final int pages;

    public HistoryGui(GuiManager gui, Player player, int requestedPage) {
        this.gui = gui;
        this.player = player;
        Settings settings = gui.settings();
        int total = gui.game().data().sessions().size();
        int rawPages = Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        this.page = Math.max(0, Math.min(rawPages - 1, requestedPage));
        this.pages = rawPages;
        this.inventory = Bukkit.createInventory(this, 54,
                gui.messages().comp("gui.history.title", Messages.ph("page", page + 1, "pages", pages)));
    }

    public void open() {
        update();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private void update() {
        for (int i = 0; i < 54; i++) inventory.setItem(i, Items.filler(Material.BLACK_STAINED_GLASS_PANE));
        Settings settings = gui.settings();
        List<SessionRecord> sessions = gui.game().data().sessions();
        int start = page * PAGE_SIZE;
        if (sessions.isEmpty()) {
            inventory.setItem(22, Items.make(Material.BARRIER, gui.messages().raw("gui.history.empty-name"),
                    List.of("<gray>Chưa có phiên nào.</gray>"), null));
        }
        for (int i = 0; i < PAGE_SIZE; i++) {
            int index = start + i;
            if (index >= sessions.size()) break;
            SessionRecord record = sessions.get(index);
            Material material = switch (record.result()) {
                case TAI -> Material.RED_WOOL;
                case XIU -> Material.BLUE_WOOL;
                default -> Material.BLACK_WOOL;
            };
            Messages messages = gui.messages();
            var ph = Messages.ph(
                    "session", record.id(),
                    "when", TimeFmt.format(record.at(), settings.dateFormat, settings.timezone),
                    "result", messages.raw(record.result().messageKey()),
                    "total", record.total(),
                    "dice", gui.game().formatDice(record.dice()),
                    "tai", Numbers.format(record.taiTotal()),
                    "xiu", Numbers.format(record.xiuTotal()),
                    "winners", record.winners(),
                    "bettors", record.bettors(),
                    "jackpot_add", Numbers.format(record.jackpotAdded()),
                    "jackpot_won", Numbers.format(record.jackpotWon())
            );
            inventory.setItem(i, Items.make(material, messages.raw("gui.history.item-name", ph),
                    messages.rawLore("gui.history.item-lore"), ph));
        }

        var pagePh = Messages.ph("page", page + 1, "pages", pages);
        inventory.setItem(45, Items.make(Material.ARROW, gui.messages().raw("gui.history.prev-name"), null, pagePh));
        inventory.setItem(46, Items.make(Material.ARROW, gui.messages().raw("gui.history.next-name"), null, pagePh));
        inventory.setItem(48, Items.make(Material.RED_WOOL, gui.messages().raw("gui.history.road-name"), null, pagePh));
        inventory.setItem(49, Items.make(Material.PAPER, gui.messages().raw("gui.history.page-name"), null, pagePh));
        inventory.setItem(51, Items.make(Material.EMERALD, gui.messages().raw("gui.history.back-name"), null, pagePh));
        inventory.setItem(53, Items.make(Material.BARRIER, gui.messages().raw("gui.history.close-name"), null, pagePh));
    }

    public void handleClick(int slot) {
        gui.game().play(player, "click");
        if (slot >= 0 && slot < PAGE_SIZE) {
            int index = page * PAGE_SIZE + slot;
            List<SessionRecord> sessions = gui.game().data().sessions();
            if (index < sessions.size()) {
                gui.openSessionDetail(player, sessions.get(index), () -> gui.openHistory(player, page));
            }
            return;
        }
        switch (slot) {
            case 45 -> {
                if (page > 0) gui.openHistory(player, page - 1);
            }
            case 46 -> {
                if (page + 1 < pages) gui.openHistory(player, page + 1);
            }
            case 48 -> gui.openRoad(player, 0);
            case 51 -> gui.openBet(player);
            case 53 -> player.closeInventory();
            default -> {
            }
        }
    }
}
