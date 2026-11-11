package com.blizcraft.blizpocket.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public final class Numbers {
    private static volatile DecimalFormat formatter = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));

    private Numbers() {
    }

    public static void setPattern(String pattern) {
        try {
            formatter = new DecimalFormat(pattern == null || pattern.isBlank() ? "#,##0" : pattern,
                    DecimalFormatSymbols.getInstance(Locale.US));
        } catch (IllegalArgumentException ignored) {
            formatter = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        }
    }

    public static String format(long amount) {
        return formatter.format(amount);
    }

    public static String format(double amount) {
        return formatter.format(amount);
    }

    public static long parseAmount(String raw) {
        if (raw == null) throw new NumberFormatException("null");
        String cleaned = raw.trim().replace(",", "").replace("_", "").replace(" ", "");
        if (cleaned.isEmpty()) throw new NumberFormatException("empty");
        long value = Long.parseLong(cleaned);
        if (value < 0) throw new NumberFormatException("negative");
        return value;
    }

    public static long clamp(long value, long min, long max) {
        if (value < min) return min;
        if (max > 0 && value > max) return max;
        return value;
    }

    public static String compact(long amount) {
        long abs = Math.abs(amount);
        if (abs < 1000) return format(amount);
        double value;
        String suffix;
        if (abs < 1_000_000) {
            value = amount / 1000.0;
            suffix = "K";
        } else if (abs < 1_000_000_000) {
            value = amount / 1_000_000.0;
            suffix = "M";
        } else {
            value = amount / 1_000_000_000.0;
            suffix = "B";
        }
        String text = String.format(Locale.US, "%.1f", value);
        if (text.endsWith(".0")) text = text.substring(0, text.length() - 2);
        return text + suffix;
    }

    public static long ceilDiv(double value) {
        if (value <= 0) return 0;
        return (long) Math.ceil(value - 1.0e-9);
    }
}
