package com.blizcraft.blizpocket.update;

import com.blizcraft.blizpocket.config.Messages;
import com.blizcraft.blizpocket.config.Settings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UpdateChecker implements Listener {
    private final JavaPlugin plugin;
    private final Settings settings;
    private final Messages messages;
    private final AtomicBoolean checked = new AtomicBoolean(false);

    public UpdateChecker(JavaPlugin plugin, Settings settings, Messages messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
    }

    public void register() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onServerLoad(ServerLoadEvent event) {
        if (!settings.updateCheckerEnabled) return;
        if (event.getType() != ServerLoadEvent.LoadType.STARTUP) return;
        if (!checked.compareAndSet(false, true)) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, this::check);
    }

    private void check() {
        try {
            String repo = settings.updateGitHubRepo;
            if (repo == null || repo.isBlank()) return;
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(Math.max(3, settings.updateTimeoutSeconds)))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.github.com/repos/" + repo + "/releases/latest"))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "BlizPocket-UpdateChecker")
                    .timeout(Duration.ofSeconds(Math.max(3, settings.updateTimeoutSeconds)))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                plugin.getLogger().info("Update checker: GitHub trả về HTTP " + response.statusCode() + " (bỏ qua).");
                return;
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String latest = json.has("tag_name") ? json.get("tag_name").getAsString() : "";
            String url = json.has("html_url") ? json.get("html_url").getAsString() : "";
            String current = plugin.getPluginMeta().getVersion();
            if (!isNewer(latest, current)) return;

            String normalizedLatest = latest.startsWith("v") ? latest.substring(1) : latest;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled()) return;
                plugin.getLogger().warning("Có bản BlizPocket mới: " + normalizedLatest
                        + " (đang dùng " + current + "). Tải tại: " + url);
                if (!settings.updateNotifyAdmins) return;
                Map<String, String> ph = Messages.ph(
                        "current", current,
                        "latest", normalizedLatest,
                        "url", url
                );
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.hasPermission("bliztaixiu.admin")) messages.send(player, "update.available", ph);
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            plugin.getLogger().info("Update checker: không kiểm tra được bản mới (" + t.getMessage() + ").");
        }
    }

    static boolean isNewer(String latest, String current) {
        int[] a = parseVersion(latest);
        int[] b = parseVersion(current);
        int length = Math.max(a.length, b.length);
        for (int i = 0; i < length; i++) {
            int left = i < a.length ? a[i] : 0;
            int right = i < b.length ? b[i] : 0;
            if (left > right) return true;
            if (left < right) return false;
        }
        return false;
    }

    private static int[] parseVersion(String version) {
        if (version == null) return new int[0];
        String clean = version.trim();
        if (clean.startsWith("v") || clean.startsWith("V")) clean = clean.substring(1);
        clean = clean.split("[-+]")[0];
        String[] parts = clean.split("\\.");
        int[] values = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String digits = parts[i].replaceAll("[^0-9].*$", "");
            try {
                values[i] = digits.isEmpty() ? 0 : Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                values[i] = 0;
            }
        }
        return values;
    }
}
