package com.blizcraft.blizpocket.bedrock;

import com.blizcraft.blizpocket.collateral.AllowedItem;
import com.blizcraft.blizpocket.collateral.CollateralContract;
import com.blizcraft.blizpocket.collateral.CollateralManager;
import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Account;
import com.blizcraft.blizpocket.data.SessionRecord;
import com.blizcraft.blizpocket.economy.CurrencyProvider;
import com.blizcraft.blizpocket.economy.ExchangeResult;
import com.blizcraft.blizpocket.game.BetSide;
import com.blizcraft.blizpocket.game.GameManager;
import com.blizcraft.blizpocket.game.Session;
import com.blizcraft.blizpocket.gui.GuiManager;
import com.blizcraft.blizpocket.util.Numbers;
import com.blizcraft.blizpocket.util.Text;
import com.blizcraft.blizpocket.util.TimeFmt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.cumulus.form.Form;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.response.CustomFormResponse;
import org.geysermc.cumulus.response.ModalFormResponse;
import org.geysermc.cumulus.response.SimpleFormResponse;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class FloodgateBedrockGui implements BedrockGui {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final GuiManager gui;

    public FloodgateBedrockGui(GuiManager gui) {
        this.gui = gui;
    }

    @Override
    public boolean isBedrock(Player player) {
        try {
            return FloodgateApi.getInstance() != null
                    && FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Throwable t) {
            return false;
        }
    }

    private void send(Player player, Form form) {
        try {
            FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
        } catch (Throwable t) {
            player.sendMessage(Component.text("Không mở được form Bedrock: " + t.getMessage()));
        }
    }

    private String legacy(String key, Map<String, String> placeholders) {
        return LEGACY.serialize(gui.messages().comp(key, placeholders));
    }

    private String legacyComponent(Component component) {
        return LEGACY.serialize(component);
    }

    private void click(Player player) {
        gui.game().play(player, "click");
    }

    private static Map<String, String> ph(Object... pairs) {
        return Messages.ph(pairs);
    }

    @Override
    public void openMainMenu(Player player) {
        GameManager game = gui.game();
        Account account = game.data().get(player.getUniqueId());
        Map<String, String> ph = ph(
                "balance", Numbers.format(account == null ? 0 : account.balance()),
                "symbol", gui.settings().baseSymbol,
                "room", game.roomSize(),
                "jackpot", Numbers.format(game.data().jackpot()),
                "player", player.getName()
        );
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(legacy("dialog.menu.title", ph))
                .content(legacy("dialog.menu.body", ph));
        if (account == null) {
            builder.button(legacy("dialog.menu.register", ph));
            builder.button(legacy("dialog.menu.help", ph));
            builder.validResultHandler((SimpleFormResponse response) -> {
                click(player);
                if (response.clickedButtonId() == 0) openRegister(player);
                else openHelp(player);
            });
        } else {
            builder.button(legacy(game.isInRoom(player) ? "dialog.menu.play" : "dialog.menu.join", ph));
            builder.button(legacy("dialog.menu.account", ph));
            builder.button(legacy("dialog.menu.exchange", ph));
            builder.button(legacy("dialog.menu.history", ph));
            builder.button(legacy("dialog.menu.road", ph));
            builder.button(legacy("dialog.menu.jackpot", ph));
            builder.button(legacy("dialog.menu.collateral", ph));
            builder.button(legacy("dialog.menu.help", ph));
            builder.validResultHandler((SimpleFormResponse response) -> {
                click(player);
                switch (response.clickedButtonId()) {
                    case 0 -> {
                        if (game.joinRoom(player)) gui.openBet(player);
                    }
                    case 1 -> openAccount(player);
                    case 2 -> openExchange(player);
                    case 3 -> gui.openHistory(player, 0);
                    case 4 -> gui.openRoad(player, 0);
                    case 5 -> openJackpot(player);
                    case 6 -> gui.openCollateral(player);
                    case 7 -> openHelp(player);
                    default -> {
                    }
                }
            });
        }
        send(player, builder.build());
    }

    @Override
    public void openRegister(Player player) {
        Map<String, String> ph = ph(
                "bonus", Numbers.format(gui.settings().startingBalance),
                "symbol", gui.settings().baseSymbol
        );
        ModalForm form = ModalForm.builder()
                .title(legacy("dialog.register.title", ph))
                .content(legacy("dialog.register.body", ph))
                .button1(legacy("dialog.register.confirm", ph))
                .button2(legacy("dialog.register.cancel", ph))
                .validResultHandler((ModalFormResponse response) -> {
                    click(player);
                    if (response.clickedFirst()) {
                        gui.game().createAccount(player);
                        openMainMenu(player);
                    }
                })
                .build();
        send(player, form);
    }

    @Override
    public void openAccount(Player player) {
        Account account = gui.game().data().get(player.getUniqueId());
        if (account == null) {
            openRegister(player);
            return;
        }
        Settings settings = gui.settings();
        Messages messages = gui.messages();
        Map<String, String> ph = ph(
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
        SimpleForm form = SimpleForm.builder()
                .title(legacy("dialog.account.title", ph))
                .content(legacy("dialog.account.body", ph))
                .button(legacy("dialog.account.history", ph))
                .button(legacy("dialog.account.exchange", ph))
                .button(legacy(account.titleEnabled()
                        ? "dialog.account.toggle-title-off"
                        : "dialog.account.toggle-title-on", ph))
                .button(legacy("dialog.account.menu", ph))
                .validResultHandler((SimpleFormResponse response) -> {
                    click(player);
                    switch (response.clickedButtonId()) {
                        case 0 -> gui.openHistory(player, 0);
                        case 1 -> openExchange(player);
                        case 2 -> {
                            gui.game().toggleTitle(player);
                            openAccount(player);
                        }
                        case 3 -> openMainMenu(player);
                        default -> {
                        }
                    }
                })
                .build();
        send(player, form);
    }

    @Override
    public void openExchange(Player player) {
        Account account = gui.game().data().get(player.getUniqueId());
        if (account == null) {
            player.sendMessage(gui.messages().comp("account.required"));
            return;
        }
        gui.currencies().refresh();
        List<CurrencyProvider> providers = gui.currencies().availableProviders();
        if (providers.isEmpty()) {
            openNotice(player, gui.messages().raw("exchange.no-providers"));
            return;
        }
        StringBuilder content = new StringBuilder(legacy("dialog.exchange.body", ph(
                "fee", rate(gui.settings().exchangeFeePercent),
                "balance", Numbers.format(account.balance()),
                "symbol", gui.settings().baseSymbol,
                "providers", ""
        )));
        for (CurrencyProvider provider : providers) {
            content.append("\n§r").append(legacy("exchange.balance-line", ph(
                    "icon", "•",
                    "currency", provider.displayName(),
                    "balance", Numbers.format(provider.balance(player)),
                    "rate", rate(provider.rate())
            )));
        }
        SimpleForm.Builder builder = SimpleForm.builder()
                .title(legacy("dialog.exchange.title", ph("balance", Numbers.format(account.balance()), "symbol", gui.settings().baseSymbol)))
                .content(content.toString())
                .button(legacy("dialog.exchange.deposit", ph()));
        if (gui.settings().exchangeAllowWithdraw) {
            builder.button(legacy("dialog.exchange.withdraw", ph()));
        }
        builder.button(legacy("dialog.exchange.back", ph()));
        builder.validResultHandler((SimpleFormResponse response) -> {
            click(player);
            int id = response.clickedButtonId();
            if (id == 0) openExchangeAmount(player, null, true);
            else if (id == 1 && gui.settings().exchangeAllowWithdraw) openExchangeAmount(player, null, false);
            else openMainMenu(player);
        });
        send(player, builder.build());
    }

    @Override
    public void openExchangeAmount(Player player, String currencyId, boolean deposit) {
        Account account = gui.game().data().get(player.getUniqueId());
        if (account == null) {
            player.sendMessage(gui.messages().comp("account.required"));
            return;
        }
        List<CurrencyProvider> providers = gui.currencies().availableProviders();
        if (providers.isEmpty()) {
            openNotice(player, gui.messages().raw("exchange.no-providers"));
            return;
        }
        List<String> names = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < providers.size(); i++) {
            CurrencyProvider provider = providers.get(i);
            names.add(provider.displayName());
            if (currencyId != null && provider.id().equalsIgnoreCase(currencyId)) selected = i;
        }
        List<String> presetLabels = new ArrayList<>();
        presetLabels.add("Tùy chỉnh");
        for (long preset : gui.settings().exchangePresets) presetLabels.add(Numbers.format(preset));
        presetLabels.add("Tất cả");
        String mode = gui.messages().raw(deposit ? "dialog.exchange.deposit" : "dialog.exchange.withdraw");
        CustomForm form = CustomForm.builder()
                .title(legacyComponent(Text.mm(mode)) + " §7- chọn loại tiền / mốc / số lượng")
                .dropdown("Loại tiền", names, selected)
                .dropdown("Mốc có sẵn", presetLabels, 0)
                .input("Số lượng ngoại tệ (nếu chọn Tùy chỉnh)", "1000", "1000")
                .validResultHandler((CustomFormResponse response) -> {
                    click(player);
                    int providerIndex = response.getDropdown(0);
                    int presetIndex = response.getDropdown(1);
                    if (providerIndex < 0 || providerIndex >= providers.size()) return;
                    CurrencyProvider provider = providers.get(providerIndex);
                    long amount;
                    int presets = gui.settings().exchangePresets.size();
                    if (presetIndex == 0) {
                        try {
                            amount = Numbers.parseAmount(response.getInput(2));
                        } catch (RuntimeException e) {
                            gui.messages().send(player, "general.invalid-number", ph("input", response.getInput(2)));
                            return;
                        }
                    } else if (presetIndex <= presets) {
                        amount = gui.settings().exchangePresets.get(presetIndex - 1);
                    } else {
                        amount = deposit ? provider.balance(player)
                                : gui.currencies().maxForeignFromBase(account, provider);
                    }
                    if (amount <= 0) {
                        gui.messages().send(player, "exchange.invalid-amount");
                        return;
                    }
                    ExchangeResult result = deposit
                            ? gui.currencies().depositToBase(player, account, provider.id(), amount)
                            : gui.currencies().withdrawFromBase(player, account, provider.id(), amount);
                    if (result.success()) {
                        if (deposit) {
                            gui.messages().send(player, "exchange.success-deposit", ph(
                                    "amount", Numbers.format(result.amount()),
                                    "currency", result.currencyId(),
                                    "coins", Numbers.format(result.coins()),
                                    "symbol", gui.settings().baseSymbol
                            ));
                        } else {
                            gui.messages().send(player, "exchange.success-withdraw", ph(
                                    "coins", Numbers.format(result.coins()),
                                    "symbol", gui.settings().baseSymbol,
                                    "amount", Numbers.format(result.amount()),
                                    "currency", result.currencyId()
                            ));
                        }
                        gui.game().play(player, "exchange");
                        gui.data().saveAsync();
                        openExchange(player);
                    } else {
                        gui.messages().send(player, result.errorKey() == null ? "exchange.insufficient" : result.errorKey(),
                                ph("currency", provider.displayName()));
                        gui.game().play(player, "error");
                        openExchangeAmount(player, provider.id(), deposit);
                    }
                })
                .build();
        send(player, form);
    }

    @Override
    public void openCustomBet(Player player) {
        Session session = gui.game().session();
        Map<String, String> ph = ph(
                "min", Numbers.format(gui.settings().minBet),
                "max", Numbers.format(gui.settings().maxBet),
                "balance", Numbers.format(gui.game().data().get(player.getUniqueId()) == null ? 0
                        : gui.game().data().get(player.getUniqueId()).balance()),
                "symbol", gui.settings().baseSymbol,
                "phase", session == null ? gui.messages().raw("phase.idle")
                        : gui.messages().raw("phase." + session.phase().key())
        );
        CustomForm form = CustomForm.builder()
                .title(legacy("dialog.custom-bet.title", ph))
                .dropdown("Chọn cửa", List.of("Tài", "Xỉu"), 0)
                .input("Số tiền BlizCoin", "1000", "1000")
                .validResultHandler((CustomFormResponse response) -> {
                    click(player);
                    long amount;
                    try {
                        amount = Numbers.parseAmount(response.getInput(1));
                    } catch (RuntimeException e) {
                        gui.messages().send(player, "general.invalid-number", ph("input", response.getInput(1)));
                        return;
                    }
                    BetSide side = response.getDropdown(0) == 0 ? BetSide.TAI : BetSide.XIU;
                    if (gui.game().placeBet(player, side, amount)) gui.openBet(player);
                    else openCustomBet(player);
                })
                .build();
        send(player, form);
    }

    @Override
    public void openPledge(Player player, AllowedItem item) {
        CollateralManager manager = gui.collateral();
        if (!manager.enabled()) {
            player.sendMessage(gui.messages().comp("collateral.disabled"));
            return;
        }
        List<AllowedItem> allowed = manager.allowedItems();
        if (allowed.isEmpty()) {
            openNotice(player, gui.messages().raw("gui.collateral.empty-allowed"));
            return;
        }
        List<String> names = new ArrayList<>();
        int selected = 0;
        for (int i = 0; i < allowed.size(); i++) {
            names.add(allowed.get(i).display());
            if (allowed.get(i).material() == item.material()) selected = i;
        }
        long count = manager.countItem(player, item.material());
        Map<String, String> ph = ph(
                "item", item.display(),
                "rate", rate(item.rate()),
                "count", Numbers.format(count),
                "max", Numbers.format(manager.estimateValue(player, item)),
                "hours", rate(manager.durationHours()),
                "interest", rate(manager.interestPercent()),
                "enchant", rate(manager.enchantBonusPercentPerLevel()),
                "symbol", gui.settings().baseSymbol
        );
        CustomForm form = CustomForm.builder()
                .title(legacy("dialog.collateral.pledge-title", ph))
                .dropdown("Vật phẩm", names, selected)
                .input("Số lượng (hoặc gõ all)", "1", "1")
                .validResultHandler((CustomFormResponse response) -> {
                    click(player);
                    int index = response.getDropdown(0);
                    if (index < 0 || index >= allowed.size()) return;
                    AllowedItem chosen = allowed.get(index);
                    long amount;
                    try {
                        amount = "all".equalsIgnoreCase(response.getInput(1))
                                ? manager.countItem(player, chosen.material())
                                : Numbers.parseAmount(response.getInput(1));
                    } catch (RuntimeException e) {
                        gui.messages().send(player, "general.invalid-number", ph("input", response.getInput(1)));
                        return;
                    }
                    CollateralContract contract = manager.pledge(player, chosen, amount);
                    if (contract != null) gui.openCollateral(player);
                    else openPledge(player, chosen);
                })
                .build();
        send(player, form);
    }

    @Override
    public void openRepay(Player player, CollateralContract contract) {
        CollateralContract current = gui.collateral().contract(player.getUniqueId(), contract.id());
        if (current == null) {
            player.sendMessage(gui.messages().comp("collateral.not-found", ph("id", contract.id())));
            gui.openCollateral(player);
            return;
        }
        Account account = gui.game().data().get(player.getUniqueId());
        Map<String, String> ph = ph(
                "id", current.id(),
                "amount", Numbers.format(current.amount()),
                "item", current.itemDisplay(),
                "principal", Numbers.format(current.principal()),
                "repay", Numbers.format(current.repayCost()),
                "expires", gui.collateral().formatExpires(current.expiresAt()),
                "balance", Numbers.format(account == null ? 0 : account.balance()),
                "symbol", gui.settings().baseSymbol
        );
        ModalForm form = ModalForm.builder()
                .title(legacy("dialog.collateral.repay-title", ph))
                .content(legacy("dialog.collateral.repay-body", ph))
                .button1(legacy("dialog.collateral.repay", ph))
                .button2(legacy("gui.collateral.close-name", ph))
                .validResultHandler((ModalFormResponse response) -> {
                    click(player);
                    if (response.clickedFirst()) gui.collateral().repay(player, current.id());
                    gui.openCollateral(player);
                })
                .build();
        send(player, form);
    }

    @Override
    public void openSessionDetail(Player player, SessionRecord record) {
        Settings settings = gui.settings();
        Messages messages = gui.messages();
        StringBuilder top = new StringBuilder();
        for (SessionRecord.WinnerEntry winner : record.topWinners()) {
            if (top.length() > 0) top.append("§7, §r");
            top.append("§f").append(winner.name()).append(" §6+").append(Numbers.format(winner.payout()));
        }
        if (top.length() == 0) top.append("§7—");
        Map<String, String> ph = ph(
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
        SimpleForm form = SimpleForm.builder()
                .title(legacy("dialog.session.title", ph))
                .content(legacy("dialog.session.body", ph))
                .button(legacy("dialog.session.close", ph))
                .validResultHandler((SimpleFormResponse response) -> click(player))
                .build();
        send(player, form);
    }

    @Override
    public void openJackpot(Player player) {
        Map<String, String> ph = ph(
                "jackpot", Numbers.format(gui.game().data().jackpot()),
                "symbol", gui.settings().baseSymbol,
                "chance", rate(gui.settings().jackpotExplodeChancePercent),
                "min", Numbers.format(gui.settings().jackpotMinExplodeAmount)
        );
        SimpleForm form = SimpleForm.builder()
                .title(legacy("dialog.jackpot.title", ph))
                .content(legacy("dialog.jackpot.body", ph))
                .button(legacy("dialog.jackpot.close", ph))
                .validResultHandler((SimpleFormResponse response) -> click(player))
                .build();
        send(player, form);
    }

    @Override
    public void openHelp(Player player) {
        SimpleForm form = SimpleForm.builder()
                .title(legacy("dialog.help.title", ph()))
                .content(legacy("dialog.help.body", ph()))
                .button(legacy("dialog.help.close", ph()))
                .validResultHandler((SimpleFormResponse response) -> click(player))
                .build();
        send(player, form);
    }

    @Override
    public void openNotice(Player player, String body) {
        SimpleForm form = SimpleForm.builder()
                .title(legacy("dialog.help.title", ph()))
                .content(legacyComponent(Text.mm(body)))
                .button(legacy("dialog.menu.close", ph()))
                .validResultHandler((SimpleFormResponse response) -> click(player))
                .build();
        send(player, form);
    }

    private static String rate(double value) {
        if (value == Math.rint(value)) return String.valueOf((long) value);
        return String.format(Locale.US, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
