package com.blizcraft.blizpocket.gui;

import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.data.Bet;
import com.blizcraft.blizpocket.data.SessionRecord;
import com.blizcraft.blizpocket.game.GameManager;
import com.blizcraft.blizpocket.game.Session;
import com.blizcraft.blizpocket.util.Items;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.TimeFmt;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class BetGui implements InventoryHolder {
    private static final int[] TAI_SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    private static final int[] XIU_SLOTS = {14, 15, 16, 23, 24, 25, 32, 33, 34};

    private final GuiManager gui;
    private final Player player;
    private final Inventory inventory;
    private long amount;

    public BetGui(GuiManager gui, Player player) {
        this.gui = gui;
        this.player = player;
        this.inventory = Bukkit.createInventory(this, 54, gui.messages().comp("gui.bet.title"));
        this.amount = gui.game().settings().defaultBet;
        Account account = gui.game().data().get(player.getUniqueId());
        if (account != null && account.lastAmount() >= gui.game().settings().minBet) {
            this.amount = clamp(account.lastAmount());
        }
    }

    public void open() {
        update();
        player.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    private long clamp(long value) {
        Settings settings = gui.game().settings();
        return Numbers.clamp(value, settings.minBet, settings.maxBet);
    }

    public void update() {
        Settings settings = gui.game().settings();
        Messages messages = gui.messages();
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        Session session = game.session();
        long now = System.currentTimeMillis();
        long balance = account == null ? 0 : account.balance();
        long seconds = session == null ? 0 : session.secondsLeft(now);
        String phase = session == null ? messages.raw("phase.idle") : messages.raw("phase." + session.phase().key());
        String symbol = settings.baseSymbol;
        String diceSymbols = String.join("", settings.diceSymbols);
        Map<String, String> base = Messages.ph(
                "session", session == null ? 0 : session.id(),
                "time", seconds,
                "phase", phase,
                "room", game.roomSize(),
                "balance", Numbers.format(balance),
                "amount", Numbers.format(amount),
                "symbol", symbol,
                "jackpot", Numbers.format(game.data().jackpot()),
                "chance", formatDouble(settings.jackpotExplodeChancePercent),
                "min", Numbers.format(settings.jackpotMinExplodeAmount),
                "dice_symbols", diceSymbols
        );

        Material fillerMat = Material.BLACK_STAINED_GLASS_PANE;
        for (int i = 0; i < 54; i++) inventory.setItem(i, Items.filler(fillerMat));

        inventory.setItem(4, Items.make(Material.CLOCK,
                session == null ? messages.raw("gui.bet.no-session-name") : messages.raw("gui.bet.session-name", base),
                session == null ? List.of(messages.raw("gui.bet.no-session-name")) : messages.rawLore("gui.bet.session-lore"),
                base));
        inventory.setItem(0, Items.make(Material.CHEST, messages.raw("gui.bet.jackpot-name"),
                messages.rawLore("gui.bet.jackpot-lore"), base));
        inventory.setItem(8, Items.make(Material.KNOWLEDGE_BOOK, messages.raw("gui.bet.help-name"),
                messages.rawLore("gui.bet.help-lore"), base));

        Bet ownBet = session == null ? null : session.bet(player.getUniqueId());
        String ownSide = ownBet == null ? "-" : messages.raw(ownBet.side().messageKey());
        long ownAmount = ownBet == null ? 0 : ownBet.amount();
        Map<String, String> betBase = Messages.ph(
                "side", ownSide,
                "amount", Numbers.format(ownAmount),
                "phase", phase,
                "symbol", symbol
        );

        long taiTotal = session == null ? 0 : session.sideTotal(com.blizcraft.blizpocket.game.BetSide.TAI);
        long xiuTotal = session == null ? 0 : session.sideTotal(com.blizcraft.blizpocket.game.BetSide.XIU);
        Map<String, String> taiPh = Messages.ph(
                "total", Numbers.format(taiTotal),
                "your", Numbers.format(ownBet != null && ownBet.side() == com.blizcraft.blizpocket.game.BetSide.TAI ? ownAmount : 0),
                "amount", Numbers.format(amount),
                "symbol", symbol
        );
        Map<String, String> xiuPh = Messages.ph(
                "total", Numbers.format(xiuTotal),
                "your", Numbers.format(ownBet != null && ownBet.side() == com.blizcraft.blizpocket.game.BetSide.XIU ? ownAmount : 0),
                "amount", Numbers.format(amount),
                "symbol", symbol
        );
        List<String> taiLore = messages.rawLore("gui.bet.tai-lore");
        List<String> xiuLore = messages.rawLore("gui.bet.xiu-lore");
        for (int slot : TAI_SLOTS) {
            boolean center = slot == 20;
            inventory.setItem(slot, Items.make(center ? Material.RED_CONCRETE : Material.RED_STAINED_GLASS_PANE,
                    messages.raw("gui.bet.tai-name"), center ? taiLore : null, taiPh));
        }
        for (int slot : XIU_SLOTS) {
            boolean center = slot == 24;
            inventory.setItem(slot, Items.make(center ? Material.BLUE_CONCRETE : Material.BLUE_STAINED_GLASS_PANE,
                    messages.raw("gui.bet.xiu-name"), center ? xiuLore : null, xiuPh));
        }

        inventory.setItem(13, Items.make(Material.GOLD_INGOT, messages.raw("gui.bet.balance-name"),
                messages.rawLore("gui.bet.balance-lore"), base));
        if (!game.isInRoom(player)) {
            inventory.setItem(22, Items.make(Material.BARRIER, messages.raw("gui.bet.not-in-room-name"),
                    messages.rawLore("gui.bet.not-in-room-lore"), base));
        } else if (ownBet == null) {
            inventory.setItem(22, Items.make(Material.PAPER, messages.raw("gui.bet.your-bet-name"),
                    List.of(messages.raw("gui.bet.your-bet-none")), base));
        } else {
            inventory.setItem(22, Items.make(Material.PAPER, messages.raw("gui.bet.your-bet-name"),
                    messages.rawLore("gui.bet.your-bet-lore"), betBase));
        }

        SessionRecord latest = game.data().latest();
        if (latest == null) {
            inventory.setItem(31, Items.make(Material.GRAY_DYE, messages.raw("gui.bet.last-result-name"),
                    List.of(messages.raw("gui.bet.last-result-none")), base));
        } else {
            Map<String, String> lastPh = Messages.ph(
                    "session", latest.id(),
                    "result", messages.raw(latest.result().messageKey()),
                    "total", latest.total(),
                    "dice", game.formatDice(latest.dice()),
                    "jackpot", Numbers.format(latest.jackpotWon()),
                    "when", TimeFmt.format(latest.at(), settings.dateFormat, settings.timezone)
            );
            Material material = switch (latest.result()) {
                case TAI -> Material.RED_TERRACOTTA;
                case XIU -> Material.BLUE_TERRACOTTA;
                default -> Material.BLACK_TERRACOTTA;
            };
            inventory.setItem(31, Items.make(material, messages.raw("gui.bet.last-result-name"),
                    messages.rawLore("gui.bet.last-result-lore"), lastPh));
        }

        List<Long> presets = settings.betPresets;
        for (int i = 0; i < 9; i++) {
            int slot = 36 + i;
            if (i >= presets.size()) continue;
            long preset = presets.get(i);
            boolean selected = preset == amount;
            Map<String, String> presetPh = Messages.ph(
                    "amount", Numbers.format(preset),
                    "current", Numbers.format(amount),
                    "symbol", symbol
            );
            inventory.setItem(slot, Items.make(selected ? Material.LIME_DYE : Material.GOLD_NUGGET,
                    messages.raw("gui.bet.preset-name", presetPh), messages.rawLore("gui.bet.preset-lore"), presetPh));
        }

        Map<String, String> step = Messages.ph("step", Numbers.format(settings.betStep), "symbol", symbol);
        inventory.setItem(45, Items.make(Material.REDSTONE, messages.raw("gui.bet.decrease-name"),
                messages.rawLore("gui.bet.decrease-lore"), step));
        inventory.setItem(46, Items.make(Material.EMERALD, messages.raw("gui.bet.increase-name"),
                messages.rawLore("gui.bet.increase-lore"), step));
        inventory.setItem(47, Items.make(Material.GOLD_BLOCK, messages.raw("gui.bet.double-name"),
                messages.rawLore("gui.bet.double-lore"), base));
        Map<String, String> repeatPh = Messages.ph(
                "side", account == null || account.lastSide() == null ? "-" : messages.raw(account.lastSide().messageKey()),
                "amount", Numbers.format(account == null ? 0 : account.lastAmount())
        );
        inventory.setItem(48, Items.make(Material.CLOCK, messages.raw("gui.bet.repeat-name"),
                messages.rawLore("gui.bet.repeat-lore"), repeatPh));
        inventory.setItem(49, Items.make(Material.NAME_TAG, messages.raw("gui.bet.custom-name"),
                messages.rawLore("gui.bet.custom-lore"), base));
        inventory.setItem(50, Items.make(Material.PLAYER_HEAD, messages.raw("gui.bet.account-name"),
                messages.rawLore("gui.bet.account-lore"), base));
        inventory.setItem(51, Items.make(Material.BOOK, messages.raw("gui.bet.history-name"),
                messages.rawLore("gui.bet.history-lore"), base));
        inventory.setItem(52, Items.make(Material.EMERALD, messages.raw("gui.bet.exchange-name"),
                messages.rawLore("gui.bet.exchange-lore"), base));
        if (game.isInRoom(player)) {
            inventory.setItem(53, Items.make(Material.BARRIER, messages.raw("gui.bet.leave-name"),
                    messages.rawLore("gui.bet.leave-lore"), base));
        } else {
            inventory.setItem(53, Items.make(Material.BARRIER, messages.raw("gui.bet.close-name"),
                    messages.rawLore("gui.bet.close-lore"), base));
        }
    }

    private static String formatDouble(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value);
    }

    public void handleClick(int slot, ClickType clickType) {
        gui.game().play(player, "click");
        if (slot == 4) {
            gui.openJackpot(player);
            return;
        }
        if (slot == 8) {
            gui.openHelp(player);
            return;
        }
        if (slot == 22 && !gui.game().isInRoom(player)) {
            gui.game().joinRoom(player);
            update();
            return;
        }
        if (isIn(slot, TAI_SLOTS)) {
            gui.game().placeBet(player, com.blizcraft.blizpocket.game.BetSide.TAI, amount);
            update();
            return;
        }
        if (isIn(slot, XIU_SLOTS)) {
            gui.game().placeBet(player, com.blizcraft.blizpocket.game.BetSide.XIU, amount);
            update();
            return;
        }
        Settings settings = gui.game().settings();
        if (slot >= 36 && slot <= 44) {
            int index = slot - 36;
            if (index < settings.betPresets.size()) {
                amount = clamp(settings.betPresets.get(index));
                gui.game().play(player, "click");
                update();
            }
            return;
        }
        switch (slot) {
            case 45 -> {
                amount = clamp(amount - settings.betStep);
                gui.game().play(player, "click");
                update();
            }
            case 46 -> {
                amount = clamp(amount + settings.betStep);
                gui.game().play(player, "click");
                update();
            }
            case 47 -> {
                amount = amount > settings.maxBet / 2 ? settings.maxBet : clamp(amount * 2);
                gui.game().play(player, "click");
                update();
            }
            case 48 -> {
                Account account = gui.game().data().get(player.getUniqueId());
                if (account == null || account.lastSide() == null || account.lastAmount() <= 0) {
                    gui.messages().send(player, "bet.repeat-none");
                    gui.game().play(player, "error");
                } else {
                    amount = clamp(account.lastAmount());
                    gui.game().placeBet(player, account.lastSide(), amount);
                }
                update();
            }
            case 49 -> gui.openCustomBet(player);
            case 50 -> gui.openAccount(player);
            case 51 -> gui.openHistory(player, 0);
            case 52 -> gui.openExchange(player);
            case 53 -> {
                if (clickType.isShiftClick() && gui.game().isInRoom(player)) {
                    gui.game().leaveRoom(player);
                }
                player.closeInventory();
            }
            default -> {
            }
        }
    }

    private static boolean isIn(int slot, int[] slots) {
        for (int value : slots) if (value == slot) return true;
        return false;
    }
}
