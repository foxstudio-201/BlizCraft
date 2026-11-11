package com.blizcraft.blizpocket.config;

import com.blizcraft.blizpocket.util.Text;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Messages {
    private final JavaPlugin plugin;
    private final File file;
    private FileConfiguration config;
    private FileConfiguration defaults;
    private Component prefix;

    public Messages(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        try (java.io.InputStream in = plugin.getResource("messages.yml")) {
            if (in != null) {
                defaults = YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
            }
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Không đọc được messages.yml mặc định: " + e.getMessage());
        }
        reload();
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(file);
        prefix = Text.mm(raw("prefix"));
    }

    public Component prefix() {
        return prefix;
    }

    public String raw(String path) {
        String value = config.getString(path);
        if (value == null && defaults != null) value = defaults.getString(path);
        if (value == null) {
            List<String> list = config.getStringList(path);
            if (list.isEmpty() && defaults != null) list = defaults.getStringList(path);
            if (!list.isEmpty()) value = String.join("\n", list);
        }
        return value == null ? "<red>[missing:" + path + "]" : value;
    }

    public String raw(String path, Map<String, String> placeholders) {
        return Text.replace(raw(path), placeholders);
    }

    public Component comp(String path) {
        return Text.mm(raw(path));
    }

    public Component comp(String path, Map<String, String> placeholders) {
        return Text.mm(raw(path), placeholders);
    }

    public List<Component> lore(String path, Map<String, String> placeholders) {
        List<String> lines = config.getStringList(path);
        if (lines.isEmpty() && defaults != null) lines = defaults.getStringList(path);
        if (lines.isEmpty()) {
            String single = config.getString(path);
            if (single == null && defaults != null) single = defaults.getString(path);
            if (single != null) lines = List.of(single);
        }
        return Text.mmList(lines, placeholders);
    }

    public List<String> rawLore(String path) {
        List<String> lines = config.getStringList(path);
        if (lines.isEmpty() && defaults != null) lines = defaults.getStringList(path);
        if (lines.isEmpty()) {
            String single = config.getString(path);
            if (single == null && defaults != null) single = defaults.getString(path);
            if (single != null) return List.of(single);
        }
        return lines;
    }

    public void send(CommandSender sender, String path, Object... pairs) {
        sender.sendMessage(prefix.append(comp(path, ph(pairs))));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(prefix.append(comp(path, placeholders)));
    }

    public void sendNoPrefix(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(comp(path, placeholders));
    }

    public void raw(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(comp(path, placeholders));
    }

    public static Map<String, String> ph(Object... pairs) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            map.put(String.valueOf(pairs[i]), String.valueOf(pairs[i + 1]));
        }
        return map;
    }

    public static Map<String, String> copy(Map<String, String> base) {
        return base == null ? new LinkedHashMap<>() : new LinkedHashMap<>(base);
    }

    public static List<String> lines(String... lines) {
        List<String> list = new ArrayList<>();
        for (String line : lines) if (line != null && !line.isEmpty()) list.add(line);
        return list;
    }
}
