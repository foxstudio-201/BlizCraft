package com.blizcraft.blizpocket.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeFmt {
    private TimeFmt() {
    }

    public static String format(long epochMillis, String pattern, String zone) {
        try {
            ZoneId zoneId = zone == null || zone.isBlank() ? ZoneId.systemDefault() : ZoneId.of(zone);
            return DateTimeFormatter.ofPattern(pattern).withZone(zoneId).format(Instant.ofEpochMilli(epochMillis));
        } catch (RuntimeException e) {
            return String.valueOf(epochMillis / 1000);
        }
    }

    public static String duration(long millis) {
        long seconds = Math.max(0, millis / 1000);
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        if (days > 0) return days + "d " + hours + "h " + minutes + "m";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + secs + "s";
        return secs + "s";
    }
}
