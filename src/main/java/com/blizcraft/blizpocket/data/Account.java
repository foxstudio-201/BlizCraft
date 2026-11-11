package com.blizcraft.blizpocket.data;

import com.blizcraft.blizpocket.game.BetSide;
import com.blizcraft.blizpocket.game.GameResult;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.UUID;

public final class Account {
    private final UUID uuid;
    private String name;
    private long balance;
    private long totalBet;
    private long totalWon;
    private long biggestWin;
    private int wins;
    private int losses;
    private int draws;
    private int streak;
    private int bestStreak;
    private long joinedAt;
    private BetSide lastSide;
    private long lastAmount;
    private final Deque<PlayerSessionRecord> history = new ArrayDeque<>();

    public Account(UUID uuid, String name, long balance, long joinedAt) {
        this.uuid = uuid;
        this.name = name;
        this.balance = balance;
        this.joinedAt = joinedAt;
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void name(String name) {
        if (name != null && !name.isBlank()) this.name = name;
    }

    public long balance() {
        return balance;
    }

    public void balance(long value) {
        this.balance = value;
    }

    public void addBalance(long amount) {
        this.balance += amount;
    }

    public boolean has(long amount) {
        return balance >= amount;
    }

    public long totalBet() {
        return totalBet;
    }

    public void addTotalBet(long amount) {
        this.totalBet += amount;
    }

    public long totalWon() {
        return totalWon;
    }

    public long biggestWin() {
        return biggestWin;
    }

    public int wins() {
        return wins;
    }

    public int losses() {
        return losses;
    }

    public int draws() {
        return draws;
    }

    public int streak() {
        return streak;
    }

    public int bestStreak() {
        return bestStreak;
    }

    public long joinedAt() {
        return joinedAt;
    }

    public void joinedAt(long joinedAt) {
        this.joinedAt = joinedAt;
    }

    public void loadStats(long totalBet, long totalWon, long biggestWin, int wins, int losses, int draws,
                          int streak, int bestStreak, BetSide lastSide, long lastAmount) {
        this.totalBet = totalBet;
        this.totalWon = totalWon;
        this.biggestWin = biggestWin;
        this.wins = wins;
        this.losses = losses;
        this.draws = draws;
        this.streak = streak;
        this.bestStreak = bestStreak;
        this.lastSide = lastSide;
        this.lastAmount = lastAmount;
    }

    public BetSide lastSide() {
        return lastSide;
    }

    public long lastAmount() {
        return lastAmount;
    }

    public void lastBet(BetSide side, long amount) {
        this.lastSide = side;
        this.lastAmount = amount;
    }

    public Deque<PlayerSessionRecord> history() {
        return history;
    }

    public void recordOutcome(long sessionId, long at, BetSide side, long stake, GameResult result,
                              long payout, long jackpotWin, int maxHistory) {
        long received = payout + jackpotWin;
        long profit = received - stake;
        totalWon += received;
        biggestWin = Math.max(biggestWin, received);
        if (received > stake) {
            wins++;
            streak++;
            bestStreak = Math.max(bestStreak, streak);
        } else if (received == 0) {
            losses++;
            streak = 0;
        } else {
            draws++;
            streak = 0;
        }
        history.addFirst(new PlayerSessionRecord(sessionId, at, side, stake, result.name(), payout, jackpotWin, profit));
        while (history.size() > maxHistory) history.removeLast();
    }

    public void recordForfeit(long sessionId, long at, BetSide side, long stake, int maxHistory) {
        losses++;
        streak = 0;
        history.addFirst(new PlayerSessionRecord(sessionId, at, side, stake, "FORFEIT", 0, 0, -stake));
        while (history.size() > maxHistory) history.removeLast();
    }

    public void reset(long startingBalance) {        this.balance = startingBalance;
        this.totalBet = 0;
        this.totalWon = 0;
        this.biggestWin = 0;
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
        this.streak = 0;
        this.bestStreak = 0;
        this.joinedAt = System.currentTimeMillis();
        this.lastSide = null;
        this.lastAmount = 0;
        this.history.clear();
    }
}
