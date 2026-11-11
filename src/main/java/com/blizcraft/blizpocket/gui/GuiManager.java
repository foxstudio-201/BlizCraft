package com.blizcraft.blizpocket.gui;

import com.blizcraft.blizpocket.bedrock.BedrockGui;
import com.blizcraft.blizpocket.collateral.CollateralContract;
import com.blizcraft.blizpocket.collateral.CollateralManager;
import com.blizcraft.blizpocket.collateral.AllowedItem;
import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.DataStore;
import com.blizcraft.blizpocket.data.SessionRecord;
import com.blizcraft.blizpocket.economy.CurrencyManager;
import com.blizcraft.blizpocket.game.GameManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

public final class GuiManager {
    private final JavaPlugin plugin;
    private final GameManager game;
    private final Settings settings;
    private final Messages messages;
    private final DataStore data;
    private final CurrencyManager currencies;
    private final CollateralManager collateral;
    private final Dialogs dialogs;
    private final BedrockGui bedrockGui;

    public GuiManager(JavaPlugin plugin, GameManager game, Settings settings, Messages messages,
                      DataStore data, CurrencyManager currencies, CollateralManager collateral) {
        this.plugin = plugin;
        this.game = game;
        this.settings = settings;
        this.messages = messages;
        this.data = data;
        this.currencies = currencies;
        this.collateral = collateral;
        this.dialogs = new Dialogs(this);
        BedrockGui bedrock = null;
        if (Bukkit.getPluginManager().isPluginEnabled("floodgate")) {
            try {
                Class<?> type = Class.forName("com.blizcraft.blizpocket.bedrock.FloodgateBedrockGui");
                bedrock = (BedrockGui) type.getConstructor(GuiManager.class).newInstance(this);
            } catch (Throwable t) {
                plugin.getLogger().warning("Không khởi tạo được GUI Bedrock: " + t.getMessage());
            }
        }
        this.bedrockGui = bedrock;
        if (bedrock != null) plugin.getLogger().info("Đã bật GUI Bedrock native (Floodgate).");
    }

    public boolean isBedrock(Player player) {
        return bedrockGui != null && bedrockGui.isBedrock(player);
    }

    public void openBet(Player player) {
        prepare(player);
        Bukkit.getScheduler().runTask(plugin, () -> new BetGui(this, player).open());
    }

    public void openHistory(Player player, int page) {
        prepare(player);
        Bukkit.getScheduler().runTask(plugin, () -> new HistoryGui(this, player, page).open());
    }

    public void openRoad(Player player, int page) {
        prepare(player);
        Bukkit.getScheduler().runTask(plugin, () -> new RoadGui(this, player, page).open());
    }

    public void openMainMenu(Player player) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openMainMenu(player); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openMainMenu(player));
    }

    public void openRegister(Player player) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openRegister(player); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openRegister(player));
    }

    public void openAccount(Player player) {
        if (!player.hasPermission("bliztaixiu.account")) { messages.send(player, "general.no-permission"); return; }
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openAccount(player); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openAccount(player));
    }

    public void openExchangeAmount(Player player, String currencyId, boolean deposit) {
        if (!player.hasPermission("bliztaixiu.exchange")) { messages.send(player, "general.no-permission"); return; }
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openExchangeAmount(player, currencyId, deposit); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openExchangeAmount(player, currencyId, deposit));
    }

    public void openExchange(Player player) {
        if (!player.hasPermission("bliztaixiu.exchange")) { messages.send(player, "general.no-permission"); return; }
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openExchange(player); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openExchange(player));
    }

    public void openJackpot(Player player) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openJackpot(player); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openJackpot(player));
    }

    public void openHelp(Player player) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openHelp(player); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openHelp(player));
    }

    public void openCustomBet(Player player) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openCustomBet(player); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openCustomBet(player));
    }

    public void openSessionDetail(Player player, SessionRecord record) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openSessionDetail(player, record); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openSessionDetail(player, record));
    }

    public void openCollateral(Player player) {
        if (!player.hasPermission("bliztaixiu.collateral")) { messages.send(player, "general.no-permission"); return; }
        prepare(player);
        Bukkit.getScheduler().runTask(plugin, () -> new CollateralGui(this, player).open());
    }

    public void openPledgeDialog(Player player, AllowedItem item) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openPledge(player, item); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openPledge(player, item));
    }

    public void openRepayDialog(Player player, CollateralContract contract) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openRepay(player, contract); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openRepay(player, contract));
    }

    public void openNotice(Player player, String body) {
        prepare(player);
        if (isBedrock(player)) { bedrockGui.openNotice(player, body); return; }
        Bukkit.getScheduler().runTask(plugin, () -> dialogs.openNotice(player, body));
    }

    private void prepare(Player player) {
        player.closeInventory();
        player.closeDialog();
    }

    public void tickOpen() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Inventory top = player.getOpenInventory().getTopInventory();
            if (top.getHolder() instanceof BetGui betGui) {
                betGui.update();
            } else if (top.getHolder() instanceof CollateralGui collateralGui) {
                collateralGui.update();
            }
        }
    }

    public JavaPlugin plugin() {
        return plugin;
    }

    public GameManager game() {
        return game;
    }

    public Settings settings() {
        return settings;
    }

    public Messages messages() {
        return messages;
    }

    public DataStore data() {
        return data;
    }

    public CurrencyManager currencies() {
        return currencies;
    }

    public CollateralManager collateral() {
        return collateral;
    }

    public Dialogs dialogs() {
        return dialogs;
    }
}
