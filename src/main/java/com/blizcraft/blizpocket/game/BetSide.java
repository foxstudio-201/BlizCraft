package com.blizcraft.blizpocket.game;

import java.util.Locale;

public enum BetSide {
    TAI("tai", "result.tai"),
    XIU("xiu", "result.xiu");

    private final String key;
    private final String messageKey;

    BetSide(String key, String messageKey) {
        this.key = key;
        this.messageKey = messageKey;
    }

    public String key() {
        return key;
    }

    public String messageKey() {
        return messageKey;
    }

    public BetSide opposite() {
        return this == TAI ? XIU : TAI;
    }

    public static BetSide parse(String input) {
        if (input == null) return null;
        String normalized = input.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "tai", "tài", "t", "big", "lon", "lớn" -> TAI;
            case "xiu", "xỉu", "x", "small", "nho", "nhỏ" -> XIU;
            default -> null;
        };
    }
}
