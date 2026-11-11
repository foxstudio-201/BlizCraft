package com.blizcraft.blizpocket.gui;

import com.blizcraft.blizpocket.collateral.AllowedItem;
import com.blizcraft.blizpocket.collateral.CollateralContract;
import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.data.SessionRecord;
import com.blizcraft.blizpocket.economy.CurrencyProvider;
import com.blizcraft.blizpocket.economy.ExchangeResult;
import com.blizcraft.blizpocket.game.BetSide;
import com.blizcraft.blizpocket.game.GameManager;
import com.blizcraft.blizpocket.game.Session;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.Text;
import com.blizcraft.blizpocket.util.TimeFmt;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.DialogInstancesProvider;
import io.papermc.paper.registry.data.dialog.DialogRegistryEntry;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class Dialogs {
    private final GuiManager gui;

    public Dialogs(GuiManager gui) {
        this.gui = gui;
    }

    public void openMainMenu(Player player) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        Session session = game.session();
        Map<String, String> ph = Messages.ph(
                "balance", Numbers.format(account == null ? 0 : account.balance()),
                "symbol", settings.baseSymbol,
                "room", game.roomSize(),
                "jackpot", Numbers.format(game.data().jackpot()),
                "player", player.getName()
        );
        String body;
        List<ActionButton> actions = new ArrayList<>();
        if (account == null) {
            body = messages.raw("dialog.register.body", Messages.ph(
                    "bonus", Numbers.format(settings.startingBalance),
                    "symbol", settings.baseSymbol
            ));
            actions.add(button(messages.comp("dialog.menu.register"), custom((view, audience) -> {
                if (!(audience instanceof Player p)) return;
                game.createAccount(p);
                gui.openMainMenu(p);
            })));
        } else {
            body = messages.raw("dialog.menu.body", ph);
            actions.add(button(messages.comp(game.isInRoom(player) ? "dialog.menu.play" : "dialog.menu.join"),
                    custom((view, audience) -> {
                        if (!(audience instanceof Player p)) return;
                        if (game.joinRoom(p)) gui.openBet(p);
                    })));
            actions.add(button(messages.comp("dialog.menu.account"),
                    custom((view, audience) -> {
                        if (audience instanceof Player p) gui.openAccount(p);
                    })));
            actions.add(button(messages.comp("dialog.menu.exchange"),
                    custom((view, audience) -> {
                        if (audience instanceof Player p) gui.openExchange(p);
                    })));
            actions.add(button(messages.comp("dialog.menu.history"),
                    custom((view, audience) -> {
                        if (audience instanceof Player p) gui.openHistory(p, 0);
                    })));
            actions.add(button(messages.comp("dialog.menu.road"),
                    custom((view, audience) -> {
                        if (audience instanceof Player p) gui.openRoad(p, 0);
                    })));
            actions.add(button(messages.comp("dialog.menu.jackpot"),
                    custom((view, audience) -> {
                        if (audience instanceof Player p) gui.openJackpot(p);
                    })));
            actions.add(button(messages.comp("dialog.menu.collateral"),
                    custom((view, audience) -> {
                        if (audience instanceof Player p) gui.openCollateral(p);
                    })));
        }
        actions.add(button(messages.comp("dialog.menu.help"),
                custom((view, audience) -> {
                    if (audience instanceof Player p) gui.openHelp(p);
                })));
        player.showDialog(build(messages.comp("dialog.menu.title"), body, List.of(), actions,
                messages.comp("dialog.menu.close")));
    }

    public void openRegister(Player player) {
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        String body = messages.raw("dialog.register.body", Messages.ph(
                "bonus", Numbers.format(settings.startingBalance),
                "symbol", settings.baseSymbol
        ));
        List<ActionButton> actions = List.of(
                button(messages.comp("dialog.register.confirm"), custom((view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    gui.game().createAccount(p);
                    gui.openMainMenu(p);
                }))
        );
        player.showDialog(build(messages.comp("dialog.register.title"), body, List.of(), actions,
                messages.comp("dialog.register.cancel")));
    }

    public void openAccount(Player player) {
        Account account = gui.game().data().get(player.getUniqueId());
        if (account == null) {
            openRegister(player);
            return;
        }
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        Map<String, String> ph = Messages.ph(
                "player", player.getName(),
                "balance", Numbers.format(account.balance()),
                "symbol", settings.baseSymbol,
                "total_bet", Numbers.format(account.totalBet()),
                "total_won", Numbers.format(account.totalWon()),
                "wins", account.wins(),
                "losses", account.losses(),
                "draws", account.draws(),
                "streak", account.streak(),
                "best_streak", account.bestStreak(),
                "biggest_win", Numbers.format(account.biggestWin()),
                "joined", TimeFmt.format(account.joinedAt(), settings.dateFormat, settings.timezone),
                "last_side", account.lastSide() == null ? "-" : messages.raw(account.lastSide().messageKey()),
                "last_amount", Numbers.format(account.lastAmount())
        );
        List<ActionButton> actions = List.of(
                button(messages.comp("dialog.account.history"), custom((view, audience) -> {
                    if (audience instanceof Player p) gui.openHistory(p, 0);
                })),
                button(messages.comp("dialog.account.exchange"), custom((view, audience) -> {
                    if (audience instanceof Player p) gui.openExchange(p);
                })),
                button(messages.comp(account.titleEnabled()
                                ? "dialog.account.toggle-title-off"
                                : "dialog.account.toggle-title-on"),
                        custom((view, audience) -> {
                            if (audience instanceof Player p) {
                                gui.game().toggleTitle(p);
                                gui.openAccount(p);
                            }
                        })),
                button(messages.comp("dialog.account.menu"), custom((view, audience) -> {
                    if (audience instanceof Player p) gui.openMainMenu(p);
                }))
        );
        player.showDialog(build(messages.comp("dialog.account.title", ph),
                messages.raw("dialog.account.body", ph), List.of(), actions,
                messages.comp("dialog.account.close")));
    }

    public void openExchange(Player player) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        if (account == null) {
            messages().send(player, "account.required");
            return;
        }
        gui.currencies().refresh();
        List<CurrencyProvider> providers = gui.currencies().availableProviders();
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        if (providers.isEmpty()) {
            player.showDialog(build(messages.comp("dialog.exchange.title"),
                    messages.raw("exchange.no-providers"), List.of(), List.of(),
                    messages.comp("dialog.exchange.close")));
            return;
        }

        StringBuilder providerLines = new StringBuilder();
        List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
        DialogInstancesProvider instance = DialogInstancesProvider.instance();
        for (int i = 0; i < providers.size(); i++) {
            CurrencyProvider provider = providers.get(i);
            if (providerLines.length() > 0) providerLines.append('\n');
            providerLines.append(messages.raw("exchange.balance-line", Messages.ph(
                    "icon", "•",
                    "currency", provider.displayName(),
                    "balance", Numbers.format(provider.balance(player)),
                    "rate", rate(provider.rate())
            )));
            entries.add(instance.singleOptionEntry(provider.id(), Text.mm(provider.displayName()), i == 0));
        }
        Map<String, String> bodyPh = Messages.ph(
                "fee", rate(settings.exchangeFeePercent),
                "balance", Numbers.format(account.balance()),
                "symbol", settings.baseSymbol,
                "providers", providerLines.toString()
        );
        List<DialogInput> inputs = List.of(
                DialogInput.singleOption("currency", messages.comp("dialog.exchange.currency-label"), entries)
                        .width(200).build()
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(button(messages.comp("dialog.exchange.deposit"), custom((view, audience) -> {
            if (!(audience instanceof Player p)) return;
            String currencyId = view.getText("currency");
            if (currencyId != null) {
                playerRefSchedule(p, () -> openExchangeAmount(p, currencyId, true));
            }
        })));
        if (settings.exchangeAllowWithdraw) {
            actions.add(button(messages.comp("dialog.exchange.withdraw"), custom((view, audience) -> {
                if (!(audience instanceof Player p)) return;
                String currencyId = view.getText("currency");
                if (currencyId != null) {
                    playerRefSchedule(p, () -> openExchangeAmount(p, currencyId, false));
                }
            })));
        }
        player.showDialog(build(messages.comp("dialog.exchange.title"),
                messages.raw("dialog.exchange.body", bodyPh), inputs, actions,
                messages.comp("dialog.exchange.close")));
    }

    public void openExchangeAmount(Player player, String currencyId, boolean deposit) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        if (account == null) {
            gui.messages().send(player, "account.required");
            return;
        }
        CurrencyProvider provider = gui.currencies().get(currencyId);
        if (provider == null || !provider.available()) {
            gui.messages().send(player, "exchange.unavailable", Messages.ph("currency", String.valueOf(currencyId)));
            return;
        }
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        long providerBalance = provider.balance(player);
        long max = deposit ? providerBalance : gui.currencies().maxForeignFromBase(account, provider);
        String mode = messages.raw(deposit ? "dialog.exchange.deposit" : "dialog.exchange.withdraw");
        Map<String, String> ph = Messages.ph(
                "mode", mode,
                "mode_lower", deposit ? "nạp" : "rút",
                "currency", provider.displayName(),
                "rate", rate(provider.rate()),
                "balance", Numbers.format(providerBalance),
                "coins", Numbers.format(account.balance()),
                "max", Numbers.format(Math.max(0, max)),
                "fee", rate(settings.exchangeFeePercent),
                "symbol", settings.baseSymbol
        );
        long initial = Math.max(1, Math.min(1000, Math.max(1, max)));
        List<DialogInput> inputs = List.of(
                DialogInput.text("amount", messages.comp("dialog.exchange.amount-label"))
                        .initial(String.valueOf(initial)).maxLength(24).build()
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(button(messages.comp("dialog.exchange.custom"), custom((view, audience) -> {
            if (!(audience instanceof Player p)) return;
            long amount;
            try {
                amount = Numbers.parseAmount(view.getText("amount"));
            } catch (RuntimeException e) {
                gui.messages().send(p, "general.invalid-number", Messages.ph("input", String.valueOf(view.getText("amount"))));
                game.play(p, "error");
                playerRefSchedule(p, () -> openExchangeAmount(p, currencyId, deposit));
                return;
            }
            executeExchange(p, currencyId, deposit, amount);
        })));
        for (long preset : settings.exchangePresets) {
            actions.add(button(messages.comp("dialog.exchange.preset", Messages.ph("amount", Numbers.format(preset))),
                    custom((view, audience) -> {
                        if (audience instanceof Player p) executeExchange(p, currencyId, deposit, preset);
                    })));
        }
        actions.add(button(messages.comp("dialog.exchange.all"), custom((view, audience) -> {
            if (audience instanceof Player p) executeExchange(p, currencyId, deposit, max);
        })));
        actions.add(button(messages.comp("dialog.exchange.back"), custom((view, audience) -> {
            if (audience instanceof Player p) gui.openExchange(p);
        })));
        player.showDialog(build(messages.comp("dialog.exchange.amount-title", ph),
                messages.raw("dialog.exchange.amount-body", ph), inputs, actions,
                messages.comp("dialog.exchange.close")));
    }

    private void executeExchange(Player player, String currencyId, boolean deposit, long amount) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        if (account == null) return;
        CurrencyProvider provider = gui.currencies().get(currencyId);
        if (provider == null) {
            gui.messages().send(player, "general.invalid-currency", Messages.ph("currency", String.valueOf(currencyId)));
            game.play(player, "error");
            return;
        }
        if (amount <= 0) {
            gui.messages().send(player, "exchange.invalid-amount");
            game.play(player, "error");
            playerRefSchedule(player, () -> openExchangeAmount(player, currencyId, deposit));
            return;
        }
        ExchangeResult result = deposit
                ? gui.currencies().depositToBase(player, account, currencyId, amount)
                : gui.currencies().withdrawFromBase(player, account, currencyId, amount);
        if (result.success()) {
            if (deposit) {
                gui.messages().send(player, "exchange.success-deposit", Messages.ph(
                        "amount", Numbers.format(result.amount()),
                        "currency", result.currencyId(),
                        "coins", Numbers.format(result.coins()),
                        "symbol", gui.settings().baseSymbol
                ));
            } else {
                gui.messages().send(player, "exchange.success-withdraw", Messages.ph(
                        "coins", Numbers.format(result.coins()),
                        "symbol", gui.settings().baseSymbol,
                        "amount", Numbers.format(result.amount()),
                        "currency", result.currencyId()
                ));
            }
            game.play(player, "exchange");
            gui.data().saveAsync();
            gui.openExchange(player);
        } else {
            sendExchangeError(player, result, provider);
            playerRefSchedule(player, () -> openExchangeAmount(player, currencyId, deposit));
        }
    }

    private void playerRefSchedule(Player player, Runnable action) {
        org.bukkit.Bukkit.getScheduler().runTask(gui.plugin(), () -> {
            player.closeDialog();
            action.run();
        });
    }

    private void sendExchangeError(Player player, ExchangeResult result, CurrencyProvider provider) {
        String key = result.errorKey() == null ? "exchange.insufficient" : result.errorKey();
        gui.messages().send(player, key, Messages.ph("currency", provider.displayName()));
        gui.game().play(player, "error");
    }

    public void openPledge(Player player, AllowedItem item) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        if (account == null) {
            gui.messages().send(player, "account.required");
            return;
        }
        if (!gui.collateral().enabled()) {
            gui.messages().send(player, "collateral.disabled");
            return;
        }
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        long count = gui.collateral().countItem(player, item.material());
        long estimated = gui.collateral().estimateValue(player, item);
        Map<String, String> ph = Messages.ph(
                "item", item.display(),
                "rate", rate(item.rate()),
                "count", Numbers.format(count),
                "hours", rate(gui.collateral().durationHours()),
                "interest", rate(gui.collateral().interestPercent()),
                "enchant", rate(gui.collateral().enchantBonusPercentPerLevel()),
                "max", Numbers.format(estimated),
                "symbol", settings.baseSymbol
        );
        List<DialogInput> inputs = List.of(
                DialogInput.text("amount", messages.comp("dialog.collateral.amount-label"))
                        .initial("1").maxLength(12).build()
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(button(messages.comp("dialog.collateral.pledge"), custom((view, audience) -> {
            if (!(audience instanceof Player p)) return;
            long amount;
            try {
                amount = Numbers.parseAmount(view.getText("amount"));
            } catch (RuntimeException e) {
                gui.messages().send(p, "general.invalid-number", Messages.ph("input", String.valueOf(view.getText("amount"))));
                game.play(p, "error");
                gui.openPledgeDialog(p, item);
                return;
            }
            CollateralContract contract = gui.collateral().pledge(p, item, amount);
            if (contract != null) {
                gui.openCollateral(p);
            } else {
                gui.openPledgeDialog(p, item);
            }
        })));
        actions.add(button(messages.comp("dialog.collateral.all"), custom((view, audience) -> {
            if (!(audience instanceof Player p)) return;
            CollateralContract contract = gui.collateral().pledge(p, item, count);
            if (contract != null) {
                gui.openCollateral(p);
            } else {
                gui.openPledgeDialog(p, item);
            }
        })));
        actions.add(button(messages.comp("gui.collateral.back-name"), custom((view, audience) -> {
            if (audience instanceof Player p) gui.openCollateral(p);
        })));
        player.showDialog(build(messages.comp("dialog.collateral.pledge-title", ph),
                messages.raw("dialog.collateral.pledge-body", ph), inputs, actions,
                messages.comp("dialog.collateral.close")));
    }

    public void openRepay(Player player, CollateralContract contract) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        if (account == null) {
            gui.messages().send(player, "account.required");
            return;
        }
        CollateralContract current = gui.collateral().contract(player.getUniqueId(), contract.id());
        if (current == null) {
            gui.messages().send(player, "collateral.not-found", Messages.ph("id", contract.id()));
            gui.openCollateral(player);
            return;
        }
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        Map<String, String> ph = Messages.ph(
                "id", current.id(),
                "amount", Numbers.format(current.amount()),
                "item", current.itemDisplay(),
                "principal", Numbers.format(current.principal()),
                "repay", Numbers.format(current.repayCost()),
                "expires", gui.collateral().formatExpires(current.expiresAt()),
                "balance", Numbers.format(account.balance()),
                "symbol", settings.baseSymbol
        );
        List<ActionButton> actions = new ArrayList<>();
        actions.add(button(messages.comp("dialog.collateral.repay"), custom((view, audience) -> {
            if (!(audience instanceof Player p)) return;
            if (gui.collateral().repay(p, current.id())) {
                gui.openCollateral(p);
            } else {
                CollateralContract still = gui.collateral().contract(p.getUniqueId(), current.id());
                if (still != null) gui.openRepayDialog(p, still);
                else gui.openCollateral(p);
            }
        })));
        actions.add(button(messages.comp("gui.collateral.back-name"), custom((view, audience) -> {
            if (audience instanceof Player p) gui.openCollateral(p);
        })));
        player.showDialog(build(messages.comp("dialog.collateral.repay-title", ph),
                messages.raw("dialog.collateral.repay-body", ph), List.of(), actions,
                messages.comp("dialog.collateral.close")));
    }

    public void openCustomBet(Player player) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        if (account == null) {
            gui.messages().send(player, "account.required");
            return;
        }
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        Session session = game.session();
        Map<String, String> ph = Messages.ph(
                "min", Numbers.format(settings.minBet),
                "max", Numbers.format(settings.maxBet),
                "balance", Numbers.format(account.balance()),
                "symbol", settings.baseSymbol,
                "phase", session == null ? messages.raw("phase.idle") : messages.raw("phase." + session.phase().key())
        );
        long initial = account.lastAmount() >= settings.minBet ? Math.min(settings.maxBet, account.lastAmount()) : settings.defaultBet;
        List<DialogInput> inputs = List.of(
                DialogInput.text("amount", messages.comp("dialog.custom-bet.amount-label"))
                        .initial(String.valueOf(initial)).maxLength(24).build()
        );
        List<ActionButton> actions = List.of(
                button(messages.comp("dialog.custom-bet.tai"), custom((view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    handleCustomBet(p, view, BetSide.TAI);
                })),
                button(messages.comp("dialog.custom-bet.xiu"), custom((view, audience) -> {
                    if (!(audience instanceof Player p)) return;
                    handleCustomBet(p, view, BetSide.XIU);
                }))
        );
        player.showDialog(build(messages.comp("dialog.custom-bet.title"),
                messages.raw("dialog.custom-bet.body", ph), inputs, actions,
                messages.comp("dialog.custom-bet.close")));
    }

    private void handleCustomBet(Player player, DialogResponseView view, BetSide side) {
        long amount;
        try {
            amount = Numbers.parseAmount(view.getText("amount"));
        } catch (RuntimeException e) {
            gui.messages().send(player, "general.invalid-number", Messages.ph("input", String.valueOf(view.getText("amount"))));
            gui.game().play(player, "error");
            gui.openCustomBet(player);
            return;
        }
        boolean success = gui.game().placeBet(player, side, amount);
        if (success) {
            gui.openBet(player);
        } else {
            gui.openCustomBet(player);
        }
    }

    public void openJackpot(Player player) {
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        Map<String, String> ph = Messages.ph(
                "jackpot", Numbers.format(gui.game().data().jackpot()),
                "symbol", settings.baseSymbol,
                "chance", rate(settings.jackpotExplodeChancePercent),
                "min", Numbers.format(settings.jackpotMinExplodeAmount)
        );
        player.showDialog(build(messages.comp("dialog.jackpot.title"),
                messages.raw("dialog.jackpot.body", ph), List.of(), List.of(),
                messages.comp("dialog.jackpot.close")));
    }

    public void openHelp(Player player) {
        Messages messages = gui.messages();
        player.showDialog(build(messages.comp("dialog.help.title"),
                messages.raw("dialog.help.body"), List.of(), List.of(),
                messages.comp("dialog.help.close")));
    }

    public void openSessionDetail(Player player, SessionRecord record) {
        Messages messages = gui.messages();
        Settings settings = gui.settings();
        StringBuilder top = new StringBuilder();
        for (SessionRecord.WinnerEntry winner : record.topWinners()) {
            if (top.length() > 0) top.append("<gray>, </gray>");
            top.append("<white>").append(winner.name()).append("</white> <gold>+")
                    .append(Numbers.format(winner.payout())).append("</gold>");
        }
        if (top.length() == 0) top.append("<gray>—</gray>");
        Map<String, String> ph = Messages.ph(
                "session", record.id(),
                "when", TimeFmt.format(record.at(), settings.dateFormat, settings.timezone),
                "dice", gui.game().formatDice(record.dice()),
                "total", record.total(),
                "result", messages.raw(record.result().messageKey()),
                "tai", Numbers.format(record.taiTotal()),
                "xiu", Numbers.format(record.xiuTotal()),
                "bettors", record.bettors(),
                "winners", record.winners(),
                "jackpot_add", Numbers.format(record.jackpotAdded()),
                "jackpot_won", Numbers.format(record.jackpotWon()),
                "top_winners", top.toString()
        );
        player.showDialog(build(messages.comp("dialog.session.title", ph),
                messages.raw("dialog.session.body", ph), List.of(), List.of(),
                messages.comp("dialog.session.close")));
    }

    public void openNotice(Player player, String body) {
        Messages messages = gui.messages();
        player.showDialog(build(messages.comp("dialog.help.title"), body, List.of(), List.of(),
                messages.comp("dialog.menu.close")));
    }

    private Dialog build(Component title, String bodyRaw, List<DialogInput> inputs,
                         List<ActionButton> actions, Component closeLabel) {
        List<DialogBody> body = new ArrayList<>();
        for (String line : bodyRaw.split("\\R", -1)) {
            body.add(DialogBody.plainMessage(Text.mm(line), 300));
        }
        if (body.isEmpty()) body.add(DialogBody.plainMessage(Component.empty(), 300));

        DialogBase base = DialogBase.builder(title)
                .body(body)
                .inputs(inputs)
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.CLOSE)
                .canCloseWithEscape(true)
                .build();

        ActionButton exit = ActionButton.builder(closeLabel)
                .width(150)
                .action(custom((view, audience) -> audience.closeDialog()))
                .build();
        DialogType type = actions.isEmpty()
                ? DialogType.notice(exit)
                : DialogType.multiAction(actions, exit, 2);

        return Dialog.create(factory -> {
            DialogRegistryEntry.Builder builder = factory.empty();
            builder.base(base);
            builder.type(type);
        });
    }

    private ActionButton button(Component label, DialogAction action) {
        return ActionButton.builder(label)
                .width(150)
                .action(action)
                .build();
    }

    private DialogAction custom(DialogActionCallback callback) {
        DialogActionCallback wrapped = (view, audience) -> {
            if (audience instanceof Player p) gui.game().play(p, "click");
            callback.accept(view, audience);
        };
        return DialogAction.customClick(wrapped, ClickCallback.Options.builder()
                .lifetime(Duration.ofMinutes(30))
                .uses(ClickCallback.UNLIMITED_USES)
                .build());
    }

    private static String rate(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private Messages messages() {
        return gui.messages();
    }
}
