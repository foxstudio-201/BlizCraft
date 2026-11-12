package com.blizcraft.blizpocket.command;

import com.blizcraft.blizpocket.BlizPocketPlugin;
import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.Text;
import com.blizcraft.blizpocket.util.TimeFmt;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TxaCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "reload", "save", "give", "take", "set", "reset", "hu", "forceend", "cancel", "start", "stats", "room", "collateral", "cau", "help"
    );

    private final BlizPocketPlugin plugin;

    public TxaCommand(BlizPocketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("bliztaixiu.admin")) {
            plugin.getMessages().send(sender, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (!require(sender, "bliztaixiu.admin.reload")) return true;
                plugin.reloadAll();
                plugin.getMessages().send(sender, "general.reloaded");
            }
            case "save" -> {
                if (!require(sender, "bliztaixiu.admin")) return true;
                plugin.getDataStore().saveNow();
                plugin.getCollateral().saveNow();
                plugin.getMessages().send(sender, "general.saved");
            }
            case "give", "take", "set" -> {
                if (!require(sender, "bliztaixiu.admin.economy")) return true;
                if (args.length < 3) {
                    sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-give")));
                    return true;
                }
                Account account = account(args[1]);
                if (account == null) {
                    plugin.getMessages().send(sender, "account.not-found");
                    return true;
                }
                long amount;
                try {
                    amount = Numbers.parseAmount(args[2]);
                } catch (NumberFormatException e) {
                    plugin.getMessages().send(sender, "general.invalid-number", Messages.ph("input", args[2]));
                    return true;
                }
                Settings settings = plugin.getSettings();
                if (sub.equals("give")) {
                    account.addBalance(amount);
                    plugin.getMessages().send(sender, "account.admin-given", Messages.ph(
                            "player", account.name(),
                            "amount", Numbers.format(amount),
                            "symbol", settings.baseSymbol
                    ));
                } else if (sub.equals("take")) {
                    long taken = Math.min(amount, Math.max(0, account.balance()));
                    long newBalance = account.balance() - taken;
                    account.balance(newBalance);
                    plugin.getMessages().send(sender, "account.admin-taken", Messages.ph(
                            "player", account.name(),
                            "amount", Numbers.format(taken),
                            "symbol", settings.baseSymbol
                    ));
                } else {
                    account.balance(amount);
                    plugin.getMessages().send(sender, "account.admin-set", Messages.ph(
                            "player", account.name(),
                            "amount", Numbers.format(amount),
                            "symbol", settings.baseSymbol
                    ));
                }
                plugin.getDataStore().logTransaction("ADMIN " + sender.getName() + " " + sub
                        + " " + account.name() + " " + amount + " -> " + account.balance());
                plugin.getDataStore().saveAsync();
            }
            case "reset" -> {
                if (!require(sender, "bliztaixiu.admin.economy")) return true;
                if (args.length < 2) {
                    sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-reset")));
                    return true;
                }
                Account account = account(args[1]);
                if (account == null) {
                    plugin.getMessages().send(sender, "account.not-found");
                    return true;
                }
                account.reset(plugin.getSettings().startingBalanceEnabled ? plugin.getSettings().startingBalance : 0);
                plugin.getMessages().send(sender, "account.admin-reset", Messages.ph("player", account.name()));
                plugin.getDataStore().logTransaction("ADMIN " + sender.getName() + " reset " + account.name());
                plugin.getDataStore().saveAsync();
            }
            case "hu" -> jackpot(sender, args);
            case "forceend" -> {
                if (!require(sender, "bliztaixiu.admin.session")) return true;
                if (plugin.getGame().forceEnd()) {
                    plugin.getMessages().send(sender, "session.forced-end");
                } else {
                    plugin.getMessages().send(sender, "session.no-session");
                }
            }
            case "cancel" -> {
                if (!require(sender, "bliztaixiu.admin.session")) return true;
                if (plugin.getGame().cancelSession()) {
                    plugin.getMessages().send(sender, "session.cancelled");
                } else {
                    plugin.getMessages().send(sender, "session.no-session");
                }
            }
            case "start" -> {
                if (!require(sender, "bliztaixiu.admin.session")) return true;
                if (plugin.getGame().startNow()) {
                    plugin.getMessages().send(sender, "session.started-admin");
                } else {
                    plugin.getMessages().send(sender, "bet.locked");
                }
            }
            case "stats" -> {
                if (!require(sender, "bliztaixiu.admin.stats")) return true;
                if (args.length < 2) {
                    sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-stats")));
                    return true;
                }
                Account account = account(args[1]);
                if (account == null) {
                    plugin.getMessages().send(sender, "account.not-found");
                    return true;
                }
                sendStats(sender, account);
            }
            case "collateral", "thechap" -> collateralCommand(sender, args);
            case "cau" -> cauCommand(sender, args);
            case "room", "list" -> sendRoom(sender);
            case "help" -> sendHelp(sender);
            default -> plugin.getMessages().send(sender, "general.unknown-command");
        }
        return true;
    }

    private void jackpot(CommandSender sender, String[] args) {
        if (!require(sender, "bliztaixiu.admin.jackpot")) return;
        Settings settings = plugin.getSettings();
        if (args.length < 2) {
            plugin.getMessages().send(sender, "jackpot.info", Messages.ph(
                    "jackpot", Numbers.format(plugin.getDataStore().jackpot()),
                    "symbol", settings.baseSymbol
            ));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("info")) {
            plugin.getMessages().send(sender, "jackpot.info", Messages.ph(
                    "jackpot", Numbers.format(plugin.getDataStore().jackpot()),
                    "symbol", settings.baseSymbol
            ));
            return;
        }
        if (!List.of("set", "add", "take").contains(action) || args.length < 3) {
            sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-jackpot")));
            return;
        }
        long amount;
        try {
            amount = Numbers.parseAmount(args[2]);
        } catch (NumberFormatException e) {
            plugin.getMessages().send(sender, "general.invalid-number", Messages.ph("input", args[2]));
            return;
        }
        if (action.equals("set")) {
            plugin.getDataStore().jackpot(amount);
            plugin.getMessages().send(sender, "jackpot.admin-set", Messages.ph(
                    "amount", Numbers.format(amount), "symbol", settings.baseSymbol));
        } else if (action.equals("add")) {
            plugin.getDataStore().addJackpot(amount);
            plugin.getMessages().send(sender, "jackpot.admin-add", Messages.ph(
                    "amount", Numbers.format(amount), "symbol", settings.baseSymbol));
        } else {
            long taken = Math.min(amount, plugin.getDataStore().jackpot());
            plugin.getDataStore().addJackpot(-taken);
            plugin.getMessages().send(sender, "jackpot.admin-take", Messages.ph(
                    "amount", Numbers.format(taken), "symbol", settings.baseSymbol));
        }
        plugin.getDataStore().logTransaction("ADMIN " + sender.getName() + " jackpot " + action + " " + amount);
        plugin.getDataStore().saveAsync();
    }

    private void sendStats(CommandSender sender, Account account) {
        Settings settings = plugin.getSettings();
        plugin.getMessages().raw(sender, "account.stats-header", Messages.ph("player", account.name()));
        plugin.getMessages().raw(sender, "account.stats-balance", Messages.ph(
                "balance", Numbers.format(account.balance()), "symbol", settings.baseSymbol));
        plugin.getMessages().raw(sender, "account.stats-total-bet", Messages.ph("total_bet", Numbers.format(account.totalBet())));
        plugin.getMessages().raw(sender, "account.stats-total-won", Messages.ph("total_won", Numbers.format(account.totalWon())));
        plugin.getMessages().raw(sender, "account.stats-wl", Messages.ph(
                "wins", account.wins(), "losses", account.losses(), "draws", account.draws()));
        plugin.getMessages().raw(sender, "account.stats-streak", Messages.ph(
                "streak", account.streak(), "best_streak", account.bestStreak()));
        plugin.getMessages().raw(sender, "account.stats-biggest", Messages.ph(
                "biggest_win", Numbers.format(account.biggestWin()), "symbol", settings.baseSymbol));
        plugin.getMessages().raw(sender, "account.stats-joined", Messages.ph(
                "joined", TimeFmt.format(account.joinedAt(), settings.dateFormat, settings.timezone)));
        plugin.getMessages().raw(sender, "account.stats-last", Messages.ph(
                "last_side", account.lastSide() == null ? "-" : plugin.getMessages().raw(account.lastSide().messageKey()),
                "last_amount", Numbers.format(account.lastAmount()),
                "symbol", settings.baseSymbol
        ));
    }

    private void collateralCommand(CommandSender sender, String[] args) {
        if (!require(sender, "bliztaixiu.admin.collateral")) return;
        if (args.length < 2) {
            sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-collateral")));
            return;
        }
        boolean clear = args[1].equalsIgnoreCase("clear");
        String name = clear ? (args.length >= 3 ? args[2] : "") : args[1];
        if (name.isBlank()) {
            sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-collateral")));
            return;
        }
        Account account = account(name);
        if (account == null) {
            plugin.getMessages().send(sender, "account.not-found");
            return;
        }
        if (clear) {
            int count = plugin.getCollateral().clear(account.uuid());
            if (count <= 0) {
                plugin.getMessages().send(sender, "collateral.admin-none");
            } else {
                plugin.getMessages().send(sender, "collateral.admin-cleared", Messages.ph(
                        "player", account.name(), "count", count));
                plugin.getDataStore().logTransaction("ADMIN " + sender.getName() + " cleared collateral "
                        + account.name() + " contracts=" + count);
            }
            return;
        }
        var contracts = plugin.getCollateral().contracts(account.uuid());
        plugin.getMessages().sendNoPrefix(sender, "collateral.list-header", Messages.ph("player", account.name()));
        if (contracts.isEmpty()) {
            plugin.getMessages().raw(sender, "collateral.no-contracts", Messages.ph());
            return;
        }
        for (var contract : contracts) {
            plugin.getMessages().raw(sender, "collateral.list-line", Messages.ph(
                    "id", contract.id(),
                    "amount", Numbers.format(contract.amount()),
                    "item", contract.itemDisplay(),
                    "principal", Numbers.format(contract.principal()),
                    "repay", Numbers.format(contract.repayCost()),
                    "expires", plugin.getCollateral().formatExpires(contract.expiresAt())
            ));
        }
    }

    private void cauCommand(CommandSender sender, String[] args) {
        if (!require(sender, "bliztaixiu.admin.cau")) return;
        if (args.length < 2) {
            sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-cau")));
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        switch (action) {
            case "set", "dat" -> {
                if (args.length < 5) {
                    sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-cau")));
                    return;
                }
                int sides = plugin.getSettings().diceSides;
                int[] dice = new int[3];
                for (int i = 0; i < 3; i++) {
                    try {
                        dice[i] = Integer.parseInt(args[i + 2]);
                    } catch (NumberFormatException e) {
                        plugin.getMessages().send(sender, "cau.invalid-dice", Messages.ph("sides", sides));
                        return;
                    }
                    if (dice[i] < 1 || dice[i] > sides) {
                        plugin.getMessages().send(sender, "cau.invalid-dice", Messages.ph("sides", sides));
                        return;
                    }
                }
                plugin.getGame().forcedDice(dice);
                plugin.getMessages().send(sender, "cau.admin-set", Messages.ph(
                        "session", plugin.getGame().forcedDiceSessionId(),
                        "dice", diceText(dice)
                ));
                plugin.getDataStore().logTransaction("ADMIN " + sender.getName() + " cau set " + diceText(dice));
            }
            case "info" -> {
                int[] dice = plugin.getGame().forcedDice();
                if (dice == null) {
                    plugin.getMessages().send(sender, "cau.admin-none");
                } else {
                    plugin.getMessages().send(sender, "cau.admin-info", Messages.ph(
                            "session", plugin.getGame().forcedDiceSessionId(),
                            "dice", diceText(dice)
                    ));
                }
            }
            case "off", "huy" -> {
                plugin.getGame().forcedDice(null);
                plugin.getMessages().send(sender, "cau.admin-off");
                plugin.getDataStore().logTransaction("ADMIN " + sender.getName() + " cau off");
            }
            case "clear", "reset", "xoa" -> {
                int count = plugin.getDataStore().clearSessions();
                if (count > 0) {
                    plugin.getMessages().send(sender, "cau.admin-cleared", Messages.ph("count", count));
                    plugin.getDataStore().logTransaction("ADMIN " + sender.getName()
                            + " cleared road history sessions=" + count);
                } else {
                    plugin.getMessages().send(sender, "cau.admin-empty");
                }
            }
            default -> sender.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-admin-cau")));
        }
    }

    private static String diceText(int[] dice) {
        return dice[0] + " " + dice[1] + " " + dice[2];
    }

    private void sendRoom(CommandSender sender) {
        var ids = plugin.getGame().roomIds();
        plugin.getMessages().raw(sender, "room.member-list-header", Messages.ph("count", ids.size()));
        if (ids.isEmpty()) {
            plugin.getMessages().raw(sender, "room.empty", Messages.ph());
            return;
        }
        for (UUID uuid : ids) {
            Account account = plugin.getDataStore().get(uuid);
            Player online = Bukkit.getPlayer(uuid);
            String name = account != null ? account.name() : online != null ? online.getName() : uuid.toString();
            plugin.getMessages().raw(sender, "room.member-line", Messages.ph("player", name));
        }
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(plugin.getMessages().comp("general.admin-help-header"));
        for (String line : plugin.getMessages().rawLore("general.admin-help")) {
            sender.sendMessage(Text.mm(line));
        }
    }

    private Account account(String name) {
        Account account = plugin.getDataStore().getByName(name);
        if (account != null) return account;
        Player online = Bukkit.getPlayerExact(name);
        return online == null ? null : plugin.getDataStore().get(online.getUniqueId());
    }

    private boolean require(CommandSender sender, String node) {
        if (sender.hasPermission(node)) return true;
        plugin.getMessages().send(sender, "general.no-permission");
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(SUBCOMMANDS, args[0]);
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give", "take", "set", "reset", "stats" -> filter(playerNames(), args[1]);
                case "hu" -> filter(List.of("info", "set", "add", "take"), args[1]);
                case "collateral", "thechap" -> {
                    List<String> options = new ArrayList<>(playerNames());
                    options.add("clear");
                    yield filter(options, args[1]);
                }
                case "cau" -> filter(List.of("set", "info", "off", "clear"), args[1]);
                default -> List.of();
            };
        }
        if (args.length >= 3 && args.length <= 5
                && args[0].equalsIgnoreCase("cau")
                && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("dat"))) {
            return filter(diceOptions(), args[args.length - 1]);
        }
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("collateral") && args[1].equalsIgnoreCase("clear")) {
                return filter(playerNames(), args[2]);
            }
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "give", "take", "set", "hu" -> filter(List.of("100", "1000", "10000", "100000"), args[2]);
                default -> List.of();
            };
        }
        return List.of();
    }

    private List<String> playerNames() {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) names.add(player.getName());
        for (Account account : plugin.getDataStore().all()) {
            if (!names.contains(account.name())) names.add(account.name());
        }
        return names;
    }

    private List<String> diceOptions() {
        List<String> options = new ArrayList<>();
        for (int i = 1; i <= plugin.getSettings().diceSides; i++) options.add(Integer.toString(i));
        return options;
    }

    private static List<String> filter(List<String> options, String prefix) {
        if (prefix == null || prefix.isBlank()) return options;
        String lower = prefix.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        for (String option : options) {
            if (option.toLowerCase(Locale.ROOT).startsWith(lower)) result.add(option);
        }
        return result;
    }
}
