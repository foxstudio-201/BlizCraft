package com.blizcraft.blizpocket.game;

public enum GameResult {
    TAI("result.tai"),
    XIU("result.xiu"),
    HOUSE("result.house");

    private final String messageKey;

    GameResult(String messageKey) {
        this.messageKey = messageKey;
    }

    public String messageKey() {
        return messageKey;
    }

    public static GameResult fromTotal(int total, boolean houseWin) {
        if (houseWin) return HOUSE;
        return total >= 11 ? TAI : XIU;
    }

    public static GameResult parse(String raw) {
        if (raw == null) return HOUSE;
        return switch (raw.toUpperCase()) {
            case "TAI" -> TAI;
            case "XIU" -> XIU;
            default -> HOUSE;
        };
    }
}
