package com.blizcraft.blizpocket.data;

import com.blizcraft.blizpocket.game.BetSide;

public record PlayerSessionRecord(
        long sessionId,
        long at,
        BetSide side,
        long amount,
        String result,
        long payout,
        long jackpotWin,
        long profit
) {
}
