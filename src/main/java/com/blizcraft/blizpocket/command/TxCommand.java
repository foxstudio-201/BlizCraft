package com.blizcraft.blizpocket.command;

import com.blizcraft.blizpocket.BlizPocketPlugin;
import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.economy.CurrencyProvider;
import com.blizcraft.blizpocket.economy.ExchangeResult;
import com.blizcraft.blizpocket.game.BetSide;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TxCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUBCOMMANDS = List.of(
            "menu", "taikhoan", "dangky", "vaophong", "thoatphong", "cuoc",
            "doicuoc", "nap", "rut", "thechap", "lichsu", "cau", "hu", "title", "top", "help"
    );

    private final BlizPocketPlugin plugin;

    public TxCommand(BlizPocketPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.getMessages().send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("bliztaixiu.use")) {
            plugin.getMessages().send(player, "general.no-permission");
            return true;
        }
        if (args.length == 0) {
            plugin.getGuis().openMainMenu(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "menu", "gui" -> plugin.getGuis().openMainMenu(player);
            case "taikhoan", "account", "acc" -> {
                if (!require(player, "bliztaixiu.account")) return true;
                plugin.getGuis().openAccount(player);
            }
            case "dangky", "register", "tao" -> {
                if (!require(player, "bliztaixiu.account")) return true;
                Account account = plugin.getDataStore().get(player.getUniqueId());
                if (account != null) {
                    plugin.getMessages().send(player, "account.exists");
                } else if (args.length >= 2 && args[1].equalsIgnoreCase("confirm")) {
                    plugin.getGame().createAccount(player);
                } else {
                    plugin.getGuis().openRegister(player);
                }
            }
            case "vaophong", "join", "vao" -> {
                if (!require(player, "bliztaixiu.play")) return true;
                if (plugin.getGame().joinRoom(player)) plugin.getGuis().openBet(player);
            }
            case "thoatphong", "leave", "thoat" -> plugin.getGame().leaveRoom(player);
            case "cuoc", "bet", "dat" -> {
                if (!require(player, "bliztaixiu.play")) return true;
                if (args.length < 3) {
                    player.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-bet")));
                    return true;
                }
                BetSide side = BetSide.parse(args[1]);
                if (side == null) {
                    plugin.getMessages().send(player, "general.invalid-side");
                    return true;
                }
                long amount;
                try {
                    if ("all".equalsIgnoreCase(args[2])) {
                        Account account = plugin.getDataStore().get(player.getUniqueId());
                        if (account == null) {
                            plugin.getMessages().send(player, "account.required");
                            return true;
                        }
                        amount = Math.min(account.balance(), plugin.getSettings().maxBet);
                        if (amount < plugin.getSettings().minBet) {
                            plugin.getMessages().send(player, "bet.all-invalid");
                            return true;
                        }
                    } else {
                        amount = Numbers.parseAmount(args[2]);
                    }
                } catch (NumberFormatException e) {
                    plugin.getMessages().send(player, "general.invalid-number", Messages.ph("input", args[2]));
                    return true;
                }
                plugin.getGame().placeBet(player, side, amount);
            }
            case "doicuoc", "exchange", "doi" -> {
                if (!require(player, "bliztaixiu.exchange")) return true;
                if (!plugin.getSettings().exchangeEnabled) {
                    plugin.getMessages().send(player, "exchange.disabled");
                    return true;
                }
                if (args.length >= 3) {
                    var provider = plugin.getCurrencies().get(args[1]);
                    if (provider == null) {
                        plugin.getMessages().send(player, "general.invalid-currency", Messages.ph("currency", args[1]));
                        return true;
                    }
                    String mode = args[2].toLowerCase(Locale.ROOT);
                    boolean deposit = mode.equals("nap") || mode.equals("deposit");
                    boolean withdraw = mode.equals("rut") || mode.equals("withdraw");
                    if (!deposit && !withdraw) {
                        plugin.getMessages().send(player, "general.unknown-command");
                        return true;
                    }
                    plugin.getGuis().openExchangeAmount(player, provider.id(), deposit);
                    return true;
                }
                plugin.getGuis().openExchange(player);
            }
            case "nap", "deposit" -> {
                if (!require(player, "bliztaixiu.exchange")) return true;
                if (args.length < 3) {
                    player.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-exchange")));
                    return true;
                }
                exchange(player, args[1], args[2], true);
            }
            case "rut", "withdraw" -> {
                if (!require(player, "bliztaixiu.exchange")) return true;
                if (args.length < 3) {
                    player.sendMessage(Text.mm(plugin.getMessages().raw("general.usage-exchange")));
                    return true;
                }
                exchange(player, args[1], args[2], false);
            }
            case "lichsu", "history", "ls" -> {
                int page = page(args, 1);
                plugin.getGuis().openHistory(player, page);
            }
            case "cau", "road" -> {
                int page = page(args, 1);
                plugin.getGuis().openRoad(player, page);
            }
            case "thechap", "collateral", "camco", "pawn" -> collateral(player, args);
            case "hu", "jackpot" -> plugin.getGuis().openJackpot(player);
            case "title", "titlebar" -> title(player, args);
            case "top", "bxh" -> sendTop(player);
            case "help", "trogiup" -> sendHelp(player);
            default -> plugin.getMessages().send(player, "general.unknown-command");
        }
        return true;
    }

    private void title(Player player, String[] args) {
        if (!require(player, "bliztaixiu.title")) return;
        if (args.length < 2) {
            plugin.getGame().toggleTitle(player);
            return;
        }
        String value = args[1].toLowerCase(Locale.ROOT);
        if (value.equals("on") || value.equals("bat") || value.equals("true")) {
            plugin.getGame().setTitleEnabled(player, true);
        } else if (value.equals("off") || value.equals("tat") || value.equals("false")) {
            plugin.getGame().setTitleEnabled(player, false);
        } else {
            plugin.getMessages().send(player, "general.unknown-command");
        }
    }

    private void collateral(Player player, String[] args) {
        if (!require(player, "bliztaixiu.collateral")) return;
        Account account = plugin.getDataStore().get(player.getUniqueId());
        if (account == null) {
            plugin.getMessages().send(player, "account.required");
            return;
        }
        if (args.length < 2) {
            plugin.getGuis().openCollateral(player);
            return;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        if (action.equals("dat") || action.equals("pledge")) {
            if (args.length < 4) {
                player.sendMessage(Text.mm(plugin.getMessages().raw("collateral.usage")));
                return;
            }
            var item = plugin.getCollateral().allowed(args[2]);
            if (item == null) {
                plugin.getMessages().send(player, "collateral.not-allowed", Messages.ph("item", args[2]));
                return;
            }
            long amount;
            try {
                amount = "all".equalsIgnoreCase(args[3])
                        ? plugin.getCollateral().countItem(player, item.material())
                        : Numbers.parseAmount(args[3]);
            } catch (NumberFormatException e) {
                plugin.getMessages().send(player, "general.invalid-number", Messages.ph("input", args[3]));
                return;
            }
            var contract = plugin.getCollateral().pledge(player, item, amount);
            if (contract != null) plugin.getGuis().openCollateral(player);
            return;
        }
        if (action.equals("tra") || action.equals("repay")) {
            if (args.length < 3) {
                player.sendMessage(Text.mm(plugin.getMessages().raw("collateral.usage")));
                return;
            }
            long id;
            try {
                id = Long.parseLong(args[2]);
            } catch (NumberFormatException e) {
                plugin.getMessages().send(player, "general.invalid-number", Messages.ph("input", args[2]));
                return;
            }
            if (plugin.getCollateral().repay(player, id)) plugin.getGuis().openCollateral(player);
            return;
        }
        if (action.equals("list")) {
            var contracts = plugin.getCollateral().contracts(player);
            plugin.getMessages().sendNoPrefix(player, "collateral.list-header", Messages.ph("player", player.getName()));
            if (contracts.isEmpty()) {
                plugin.getMessages().raw(player, "collateral.no-contracts", Messages.ph());
                return;
            }
            for (var contract : contracts) {
                plugin.getMessages().raw(player, "collateral.list-line", Messages.ph(
                        "id", contract.id(),
                        "amount", Numbers.format(contract.amount()),
                        "item", contract.itemDisplay(),
                        "principal", Numbers.format(contract.principal()),
                        "repay", Numbers.format(contract.repayCost()),
                        "expires", plugin.getCollateral().formatExpires(contract.expiresAt())
                ));
            }
            return;
        }
        plugin.getGuis().openCollateral(player);
    }

    private void exchange(Player player, String currencyId, String rawAmount, boolean deposit) {
        Settings settings = plugin.getSettings();
        if (!settings.exchangeEnabled) {
            plugin.getMessages().send(player, "exchange.disabled");
            return;
        }
        Account account = plugin.getDataStore().get(player.getUniqueId());
        if (account == null) {
            plugin.getMessages().send(player, "account.required");
            return;
        }
        CurrencyProvider provider = plugin.getCurrencies().get(currencyId);
        if (provider == null) {
            plugin.getMessages().send(player, "general.invalid-currency", Messages.ph("currency", currencyId));
            return;
        }
        long amount;
        try {
            if ("all".equalsIgnoreCase(rawAmount)) {
                amount = deposit ? provider.balance(player) : plugin.getCurrencies().maxForeignFromBase(account, provider);
            } else {
                amount = Numbers.parseAmount(rawAmount);
            }
        } catch (NumberFormatException e) {
            plugin.getMessages().send(player, "general.invalid-number", Messages.ph("input", rawAmount));
            return;
        }
        ExchangeResult result = deposit
                ? plugin.getCurrencies().depositToBase(player, account, currencyId, amount)
                : plugin.getCurrencies().withdrawFromBase(player, account, currencyId, amount);
        if (result.success()) {
            if (deposit) {
                plugin.getMessages().send(player, "exchange.success-deposit", Messages.ph(
                        "amount", Numbers.format(result.amount()),
                        "currency", result.currencyId(),
                        "coins", Numbers.format(result.coins()),
                        "symbol", settings.baseSymbol
                ));
            } else {
                plugin.getMessages().send(player, "exchange.success-withdraw", Messages.ph(
                        "coins", Numbers.format(result.coins()),
                        "symbol", settings.baseSymbol,
                        "amount", Numbers.format(result.amount()),
                        "currency", result.currencyId()
                ));
            }
            plugin.getGame().play(player, "exchange");
            plugin.getDataStore().saveAsync();
        } else {
            String key = result.errorKey() == null ? "exchange.insufficient" : result.errorKey();
            plugin.getMessages().send(player, key, Messages.ph("currency", provider.displayName()));
            plugin.getGame().play(player, "error");
        }
    }

    private void sendTop(Player player) {
        Messages messages = plugin.getMessages();
        List<Account> top = plugin.getDataStore().top(10);
        player.sendMessage(messages.comp("general.top-header"));
        if (top.isEmpty()) {
            player.sendMessage(messages.comp("general.top-empty"));
            return;
        }
        int rank = 1;
        for (Account account : top) {
            player.sendMessage(messages.comp("general.top-line", Messages.ph(
                    "rank", rank++,
                    "player", account.name(),
                    "balance", Numbers.format(account.balance()),
                    "symbol", plugin.getSettings().baseSymbol
            )));
        }
    }

    private void sendHelp(Player player) {
        Messages messages = plugin.getMessages();
        player.sendMessage(messages.comp("general.help-header"));
        for (String line : messages.rawLore("general.help-line")) {
            player.sendMessage(Text.mm(line));
        }
    }

    private boolean require(Player player, String node) {
        if (player.hasPermission(node)) return true;
        plugin.getMessages().send(player, "general.no-permission");
        return false;
    }

    private static int page(String[] args, int index) {
        if (args.length <= index) return 0;
        try {
            return Math.max(0, Integer.parseInt(args[index]) - 1);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return filter(SUBCOMMANDS, args[0]);
        }
        if (args.length == 2) {
            return switch (args[0].toLowerCase(Locale.ROOT)) {
                case "cuoc", "bet", "dat" -> filter(List.of("tai", "xiu"), args[1]);
                case "doicuoc", "exchange", "doi", "nap", "deposit", "rut", "withdraw" -> filter(plugin.getCurrencies().availableIds(), args[1]);
                case "thechap", "collateral", "camco", "pawn" -> filter(List.of("dat", "tra", "list"), args[1]);
                case "title", "titlebar" -> filter(List.of("on", "off"), args[1]);
                case "lichsu", "history", "ls", "cau", "road" -> filter(List.of("1", "2", "3"), args[1]);
                default -> List.of();
            };
        }
        if (args.length == 3 && List.of("thechap", "collateral", "camco", "pawn").contains(args[0].toLowerCase(Locale.ROOT))) {
            String action = args[1].toLowerCase(Locale.ROOT);
            if (action.equals("dat")) {
                List<String> items = new ArrayList<>();
                for (var item : plugin.getCollateral().allowedItems()) items.add(item.id());
                return filter(items, args[2]);
            }
            if (action.equals("tra")) {
                List<String> ids = new ArrayList<>();
                if (sender instanceof Player p) {
                    for (var contract : plugin.getCollateral().contracts(p)) ids.add(String.valueOf(contract.id()));
                }
                return filter(ids, args[2]);
            }
            return List.of();
        }
        if (args.length == 4 && List.of("thechap", "collateral", "camco", "pawn").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(List.of("all", "1", "8", "16", "32", "64"), args[3]);
        }
        if (args.length == 3 && List.of("doicuoc", "exchange", "doi").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(List.of("nap", "rut"), args[2]);
        }
        if (args.length == 3 && List.of("nap", "deposit", "rut", "withdraw").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(List.of("all", "1000", "10000", "100000"), args[2]);
        }
        if (args.length == 3 && List.of("cuoc", "bet", "dat").contains(args[0].toLowerCase(Locale.ROOT))) {
            return filter(List.of("all", "100", "1000", "10000", "100000"), args[2]);
        }
        return List.of();
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
