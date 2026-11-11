package com.blizcraft.blizpocket;

import com.blizcraft.blizpocket.collateral.CollateralManager;
import com.blizcraft.blizpocket.command.TxCommand;
import com.blizcraft.blizpocket.command.TxaCommand;
import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.DataStore;
import com.blizcraft.blizpocket.economy.CurrencyManager;
import com.blizcraft.blizpocket.game.GameManager;
import com.blizcraft.blizpocket.gui.GuiManager;
import com.blizcraft.blizpocket.listener.GuiListener;
import com.blizcraft.blizpocket.listener.PlayerListener;
import com.blizcraft.blizpocket.update.UpdateChecker;
import com.blizcraft.blizpocket.util.Numbers;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class BlizPocketPlugin extends JavaPlugin {
    private Settings settings;
    private Messages messages;
    private DataStore dataStore;
    private CollateralManager collateral;
    private CurrencyManager currencies;
    private GameManager game;
    private GuiManager guis;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = new Settings();
        settings.reload(getConfig());
        Numbers.setPattern(settings.numberFormat);
        messages = new Messages(this);
        dataStore = new DataStore(this);
        dataStore.load(settings);
        collateral = new CollateralManager(this, settings, messages, dataStore);
        currencies = new CurrencyManager(this, settings, dataStore);
        game = new GameManager(this, settings, messages, dataStore);
        guis = new GuiManager(this, game, settings, messages, dataStore, currencies, collateral);
        game.setGuiManager(guis);

        PluginCommand tx = getCommand("taixiu");
        if (tx != null) {
            TxCommand executor = new TxCommand(this);
            tx.setExecutor(executor);
            tx.setTabCompleter(executor);
        }
        PluginCommand txa = getCommand("taixiuadmin");
        if (txa != null) {
            TxaCommand executor = new TxaCommand(this);
            txa.setExecutor(executor);
            txa.setTabCompleter(executor);
        }
        getServer().getPluginManager().registerEvents(new GuiListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        new UpdateChecker(this, settings, messages).register();
        game.start();
        getServer().getScheduler().runTaskLater(this, () -> {
            currencies.refresh();
            long available = currencies.availableProviders().size();
            getLogger().info("BlizPocket sẵn sàng. Loại tiền khả dụng: " + available + "/" + currencies.providers().size() + ".");
        }, 40L);
        getLogger().info("BlizPocket đã bật. BlizCoin: " + settings.baseName + ".");
    }

    @Override
    public void onDisable() {
        if (game != null) game.shutdown();
        if (collateral != null) collateral.saveNow();
        if (dataStore != null) dataStore.saveNow();
        getLogger().info("BlizPocket đã tắt.");
    }

    public void reloadAll() {
        reloadConfig();
        settings.reload(getConfig());
        Numbers.setPattern(settings.numberFormat);
        messages.reload();
        dataStore.setMaxSessions(settings.maxSessions);
        currencies.reload();
        currencies.refresh();
        collateral.reloadConfig();
        game.reloadSettings();
    }

    public Settings getSettings() {
        return settings;
    }

    public Messages getMessages() {
        return messages;
    }

    public DataStore getDataStore() {
        return dataStore;
    }

    public CurrencyManager getCurrencies() {
        return currencies;
    }

    public CollateralManager getCollateral() {
        return collateral;
    }

    public GameManager getGame() {
        return game;
    }

    public GuiManager getGuis() {
        return guis;
    }
}
