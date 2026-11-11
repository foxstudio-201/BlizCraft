package com.blizcraft.blizpocket.data;

import com.blizcraft.blizpocket.game.BetSide;

public record Bet(java.util.UUID playerId, String playerName, BetSide side, long amount) {
}
