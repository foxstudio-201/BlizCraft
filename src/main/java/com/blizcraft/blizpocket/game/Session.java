package com.blizcraft.blizpocket.game;

import com.blizcraft.blizpocket.data.Bet;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class Session {
    public enum Phase {
        BETTING, LOCKED, ROLLING, RESULT, WAITING, IDLE;

        public String key() {
            return name().toLowerCase();
        }
    }

    private final long id;
    private final long startedAt;
    private final Map<UUID, Bet> bets = new LinkedHashMap<>();
    private final Map<UUID, Bet> forfeited = new LinkedHashMap<>();
    private Phase phase = Phase.IDLE;
    private long phaseStartedAt;
    private long phaseEndsAt;
    private int[] dice = new int[]{1, 1, 1};
    private int total;
    private GameResult result;
    private boolean resolved;
    private long lastAnnouncedSecond = Long.MIN_VALUE;
    private long lastBossbarSecond = Long.MIN_VALUE;
    private int winnersCount;
    private long jackpotAdded;
    private long jackpotWon;
    private long houseCut;

    public Session(long id) {
        this.id = id;
        this.startedAt = System.currentTimeMillis();
    }

    public long id() {
        return id;
    }

    public long startedAt() {
        return startedAt;
    }

    public Phase phase() {
        return phase;
    }

    public void setPhase(Phase phase, long now, long durationMillis) {
        this.phase = phase;
        this.phaseStartedAt = now;
        this.phaseEndsAt = now + Math.max(1, durationMillis);
        this.lastAnnouncedSecond = Long.MIN_VALUE;
    }

    public long phaseStartedAt() {
        return phaseStartedAt;
    }

    public long phaseEndsAt() {
        return phaseEndsAt;
    }

    public long secondsLeft(long now) {
        long millis = Math.max(0, phaseEndsAt - now);
        return (millis + 999) / 1000;
    }

    public float progress(long now) {
        long duration = Math.max(1, phaseEndsAt - phaseStartedAt);
        float value = (float) ((phaseEndsAt - now) / (double) duration);
        if (value < 0) return 0;
        if (value > 1) return 1;
        return value;
    }

    public Map<UUID, Bet> bets() {
        return bets;
    }

    public Bet bet(UUID uuid) {
        return bets.get(uuid);
    }

    public void addBet(Bet bet) {
        bets.put(bet.playerId(), bet);
    }

    public Bet removeBet(UUID uuid) {
        return bets.remove(uuid);
    }

    public boolean hasParticipated(UUID uuid) {
        return bets.containsKey(uuid) || forfeited.containsKey(uuid);
    }

    public Bet forfeit(UUID uuid) {
        Bet bet = bets.remove(uuid);
        if (bet != null) forfeited.put(uuid, bet);
        return bet;
    }

    public int forfeitedCount() {
        return forfeited.size();
    }

    public int bettorCount() {
        return bets.size();
    }

    public long sideTotal(BetSide side) {
        long total = 0;
        for (Bet bet : bets.values()) if (bet.side() == side) total += bet.amount();
        return total;
    }

    public long totalPool() {
        long total = 0;
        for (Bet bet : bets.values()) total += bet.amount();
        return total;
    }

    public boolean isEmpty() {
        return bets.isEmpty();
    }

    public int[] dice() {
        return dice.clone();
    }

    public void dice(int[] dice) {
        this.dice = dice.clone();
        int sum = 0;
        for (int value : dice) sum += value;
        this.total = sum;
    }

    public int total() {
        return total;
    }

    public GameResult result() {
        return result;
    }

    public void result(GameResult result) {
        this.result = result;
    }

    public boolean resolved() {
        return resolved;
    }

    public void resolved(boolean resolved) {
        this.resolved = resolved;
    }

    public long lastAnnouncedSecond() {
        return lastAnnouncedSecond;
    }

    public void lastAnnouncedSecond(long second) {
        this.lastAnnouncedSecond = second;
    }

    public long lastBossbarSecond() {
        return lastBossbarSecond;
    }

    public void lastBossbarSecond(long second) {
        this.lastBossbarSecond = second;
    }

    public int winnersCount() {
        return winnersCount;
    }

    public void winnersCount(int winnersCount) {
        this.winnersCount = winnersCount;
    }

    public long jackpotAdded() {
        return jackpotAdded;
    }

    public void jackpotAdded(long jackpotAdded) {
        this.jackpotAdded = jackpotAdded;
    }

    public long jackpotWon() {
        return jackpotWon;
    }

    public void jackpotWon(long jackpotWon) {
        this.jackpotWon = jackpotWon;
    }

    public long houseCut() {
        return houseCut;
    }

    public void houseCut(long houseCut) {
        this.houseCut = houseCut;
    }

    public String diceText(String[] symbols) {
        StringBuilder builder = new StringBuilder();
        for (int value : dice) {
            if (builder.length() > 0) builder.append(' ');
            builder.append(symbols == null || value < 1 || value > symbols.length ? String.valueOf(value) : symbols[value - 1]);
        }
        return builder.toString();
    }
}
