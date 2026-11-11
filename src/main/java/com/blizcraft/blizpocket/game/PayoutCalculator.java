package com.blizcraft.blizpocket.game;

import com.blizcraft.blizpocket.config.Settings;
import com.blizcraft.blizpocket.data.Bet;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class PayoutCalculator {
    public record Outcome(GameResult result, boolean houseWin, int total,
                          long losingPool, long jackpotAdded, long houseCut, long prizePool, long houseTopUp,
                          Map<UUID, Long> payouts, Map<UUID, Long> winningStakes, int winnersCount,
                          Map<UUID, Long> taxes, long totalTax) {
    }

    private PayoutCalculator() {
    }

    public static Outcome calculate(Map<UUID, Bet> bets, int[] dice, Settings settings, Random random) {
        int total = Dice.total(dice);
        boolean house = (settings.houseWinOnTriple && Dice.triple(dice))
                || (settings.houseRandomChancePercent > 0
                && random.nextDouble() * 100.0 < settings.houseRandomChancePercent);
        GameResult result = GameResult.fromTotal(total, house);

        if (house) {
            long losingPool = 0;
            for (Bet bet : bets.values()) losingPool += bet.amount();
            return new Outcome(GameResult.HOUSE, true, total, losingPool, losingPool, 0, 0, 0,
                    Map.of(), Map.of(), 0, Map.of(), 0);
        }

        BetSide winnerSide = result == GameResult.TAI ? BetSide.TAI : BetSide.XIU;
        Map<UUID, Long> winningStakes = new LinkedHashMap<>();
        long losingPool = 0;
        for (Bet bet : bets.values()) {
            if (bet.side() == winnerSide) {
                winningStakes.put(bet.playerId(), bet.amount());
            } else {
                losingPool += bet.amount();
            }
        }

        if (winningStakes.isEmpty()) {
            return new Outcome(result, false, total, losingPool, losingPool, 0, 0, 0,
                    Map.of(), Map.of(), 0, Map.of(), 0);
        }

        long jackpotAdd = (long) Math.floor(losingPool * settings.jackpotContributionPercent / 100.0);
        long houseCut = settings.rakeEnabled
                ? (long) Math.floor(losingPool * settings.rakePercent / 100.0)
                : 0;
        if (jackpotAdd < 0) jackpotAdd = 0;
        if (houseCut < 0) houseCut = 0;
        if (jackpotAdd + houseCut > losingPool) {
            jackpotAdd = Math.min(jackpotAdd, losingPool);
            houseCut = Math.min(houseCut, losingPool - jackpotAdd);
        }
        long prizePool = losingPool - jackpotAdd - houseCut;

        Map<UUID, Long> shares = prizePool > 0
                ? distribute(prizePool, winningStakes)
                : Map.of();
        Map<UUID, Long> payouts = new LinkedHashMap<>();
        for (Map.Entry<UUID, Long> entry : winningStakes.entrySet()) {
            payouts.put(entry.getKey(), entry.getValue() + shares.getOrDefault(entry.getKey(), 0L));
        }

        long houseTopUp = 0;
        if (settings.houseCoversShortfall && settings.minimumWinProfitPercent > 0) {
            for (Map.Entry<UUID, Long> entry : winningStakes.entrySet()) {
                long stake = entry.getValue();
                long minimumPayout = stake + (long) Math.ceil(stake * settings.minimumWinProfitPercent / 100.0);
                long current = payouts.getOrDefault(entry.getKey(), stake);
                if (current < minimumPayout) {
                    houseTopUp += minimumPayout - current;
                    payouts.put(entry.getKey(), minimumPayout);
                }
            }
        }

        Map<UUID, Long> taxes = new LinkedHashMap<>();
        long totalTax = 0;
        if (settings.taxEnabled && settings.winTaxPercent > 0) {
            for (Map.Entry<UUID, Long> entry : winningStakes.entrySet()) {
                long stake = entry.getValue();
                long payout = payouts.getOrDefault(entry.getKey(), stake);
                long profit = Math.max(0, payout - stake);
                if (profit <= 0) continue;
                long tax = (long) Math.floor(profit * settings.winTaxPercent / 100.0);
                if (tax <= 0) continue;
                payouts.put(entry.getKey(), payout - tax);
                taxes.put(entry.getKey(), tax);
                totalTax += tax;
            }
        }

        return new Outcome(result, false, total, losingPool, jackpotAdd, houseCut, prizePool, houseTopUp,
                payouts, winningStakes, winningStakes.size(), taxes, totalTax);
    }

    public static Map<UUID, Long> distribute(long total, Map<UUID, Long> weights) {
        Map<UUID, Long> result = new LinkedHashMap<>();
        if (total <= 0 || weights.isEmpty()) return result;

        long sum = 0;
        for (long weight : weights.values()) if (weight > 0) sum += weight;
        if (sum <= 0) return result;

        Map<UUID, Long> base = new LinkedHashMap<>();
        long assigned = 0;
        BigInteger bigTotal = BigInteger.valueOf(total);
        BigInteger bigSum = BigInteger.valueOf(sum);
        for (Map.Entry<UUID, Long> entry : weights.entrySet()) {
            long weight = Math.max(0, entry.getValue());
            long share = bigTotal.multiply(BigInteger.valueOf(weight)).divide(bigSum).longValue();
            base.put(entry.getKey(), share);
            assigned += share;
        }

        long leftover = total - assigned;
        if (leftover > 0) {
            List<Map.Entry<UUID, Long>> order = new ArrayList<>(weights.entrySet());
            order.sort(Comparator.comparingLong((Map.Entry<UUID, Long> e) -> e.getValue()).reversed());
            int index = 0;
            while (leftover > 0 && !order.isEmpty()) {
                UUID uuid = order.get(index % order.size()).getKey();
                base.merge(uuid, 1L, Long::sum);
                leftover--;
                index++;
            }
        }

        for (Map.Entry<UUID, Long> entry : weights.entrySet()) {
            result.put(entry.getKey(), base.getOrDefault(entry.getKey(), 0L));
        }
        return result;
    }
}
