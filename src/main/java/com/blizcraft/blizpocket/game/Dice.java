package com.blizcraft.blizpocket.game;

import java.util.concurrent.ThreadLocalRandom;

public final class Dice {
    private Dice() {
    }

    public static int[] roll(int sides, int count) {
        int[] result = new int[count];
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) result[i] = random.nextInt(1, sides + 1);
        return result;
    }

    public static int total(int[] dice) {
        int total = 0;
        for (int value : dice) total += value;
        return total;
    }

    public static boolean triple(int[] dice) {
        if (dice == null || dice.length == 0) return false;
        for (int i = 1; i < dice.length; i++) if (dice[i] != dice[0]) return false;
        return true;
    }
}
