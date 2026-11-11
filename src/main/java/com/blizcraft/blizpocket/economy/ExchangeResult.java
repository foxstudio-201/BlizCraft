package com.blizcraft.blizpocket.economy;

public record ExchangeResult(boolean success, String errorKey, long coins, long amount, String currencyId) {
    public static ExchangeResult fail(String key) {
        return new ExchangeResult(false, key, 0, 0, null);
    }

    public static ExchangeResult ok(long coins, long amount, String currencyId) {
        return new ExchangeResult(true, null, coins, amount, currencyId);
    }
}
