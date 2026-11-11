package com.blizcraft.blizpocket.data;

import com.blizcraft.blizpocket.game.BetSide;
import com.blizcraft.blizpocket.game.GameResult;

import java.util.List;

public final class SessionRecord {
    public record WinnerEntry(String name, BetSide side, long stake, long payout) {
    }

    private final long id;
    private final long at;
    private final int[] dice;
    private final int total;
    private final GameResult result;
    private final long taiTotal;
    private final long xiuTotal;
    private final int bettors;
    private final int winners;
    private final long jackpotAdded;
    private final long jackpotWon;
    private final long houseCut;
    private final List<WinnerEntry> topWinners;

    public SessionRecord(long id, long at, int[] dice, int total, GameResult result,
                         long taiTotal, long xiuTotal, int bettors, int winners,
                         long jackpotAdded, long jackpotWon, long houseCut,
                         List<WinnerEntry> topWinners) {
        this.id = id;
        this.at = at;
        this.dice = dice == null ? new int[0] : dice.clone();
        this.total = total;
        this.result = result;
        this.taiTotal = taiTotal;
        this.xiuTotal = xiuTotal;
        this.bettors = bettors;
        this.winners = winners;
        this.jackpotAdded = jackpotAdded;
        this.jackpotWon = jackpotWon;
        this.houseCut = houseCut;
        this.topWinners = topWinners == null ? List.of() : List.copyOf(topWinners);
    }

    public long id() {
        return id;
    }

    public long at() {
        return at;
    }

    public int[] dice() {
        return dice.clone();
    }

    public int total() {
        return total;
    }

    public GameResult result() {
        return result;
    }

    public long taiTotal() {
        return taiTotal;
    }

    public long xiuTotal() {
        return xiuTotal;
    }

    public int bettors() {
        return bettors;
    }

    public int winners() {
        return winners;
    }

    public long jackpotAdded() {
        return jackpotAdded;
    }

    public long jackpotWon() {
        return jackpotWon;
    }

    public long houseCut() {
        return houseCut;
    }

    public List<WinnerEntry> topWinners() {
        return topWinners;
    }
}
