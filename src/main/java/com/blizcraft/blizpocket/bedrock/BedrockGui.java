package com.blizcraft.blizpocket.bedrock;

import com.blizcraft.blizpocket.collateral.AllowedItem;
import com.blizcraft.blizpocket.collateral.CollateralContract;
import com.blizcraft.blizpocket.data.SessionRecord;
import org.bukkit.entity.Player;

public interface BedrockGui {
    boolean isBedrock(Player player);

    void openMainMenu(Player player);

    void openRegister(Player player);

    void openAccount(Player player);

    void openExchange(Player player);

    void openExchangeAmount(Player player, String currencyId, boolean deposit);

    void openCustomBet(Player player);

    void openPledge(Player player, AllowedItem item);

    void openRepay(Player player, CollateralContract contract);

    void openSessionDetail(Player player, SessionRecord record);

    void openJackpot(Player player);

    void openHelp(Player player);

    void openNotice(Player player, String body);
}
