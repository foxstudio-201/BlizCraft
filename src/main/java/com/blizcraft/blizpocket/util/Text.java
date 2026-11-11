package com.blizcraft.blizpocket.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component mm(String input) {
        return MM.deserialize(normalize(input));
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return input
                .replace("<purple>", "<light_purple>")
                .replace("</purple>", "</light_purple>")
                .replace("<pink>", "<light_purple>")
                .replace("</pink>", "</light_purple>")
                .replace("<orange>", "<gold>")
                .replace("</orange>", "</gold>");
    }

    public static Component mm(String input, Map<String, String> placeholders) {
        return mm(replace(input, placeholders));
    }

    public static String replace(String input, Map<String, String> placeholders) {
        if (input == null) return "";
        if (placeholders == null || placeholders.isEmpty()) return input;
        String out = input;
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            out = out.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        return out;
    }

    public static List<Component> mmList(List<String> lines, Map<String, String> placeholders) {
        List<Component> result = new ArrayList<>(lines.size());
        for (String line : lines) {
            result.add(mm(replace(line, placeholders)));
        }
        return result;
    }

    public static String stripTags(String input) {
        return MM.stripTags(input == null ? "" : input);
    }
}
