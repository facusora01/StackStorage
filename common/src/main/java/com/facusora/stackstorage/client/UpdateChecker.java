package com.facusora.stackstorage.client;

import com.facusora.stackstorage.platform.Services;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On world join, asks the Modrinth API for the newest published version of the mod
 * for this loader and game version and, if it is newer than the installed one, shows
 * the player a chat message with a download link. Runs once per game launch,
 * off-thread, and fails silently (no internet, API down, etc.).
 */
public final class UpdateChecker
{
    private static final String VERSIONS_API_URL =
            "https://api.modrinth.com/v2/project/stack-storage/version?loaders=%s&game_versions=%s";
    private static final String DOWNLOAD_URL = "https://modrinth.com/mod/stack-storage/versions";

    private static final AtomicBoolean CHECKED = new AtomicBoolean(false);

    private UpdateChecker()
    {
    }

    /** When the local player joins a world. */
    public static void onLoggingIn()
    {
        if (!CHECKED.compareAndSet(false, true))
            return;
        CompletableFuture.runAsync(UpdateChecker::checkForUpdate);
    }

    private static void checkForUpdate()
    {
        try
        {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(versionsUrl()))
                    .header("User-Agent", "facusora01/StackStorage (update checker)")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return;

            JsonArray versions = JsonParser.parseString(response.body()).getAsJsonArray();
            if (versions.isEmpty())
                return;

            // Modrinth returns versions newest-first
            String latest = versions.get(0).getAsJsonObject().get("version_number").getAsString();
            if (!VersionCompare.isNewer(latest, Services.PLATFORM.modVersion()))
                return;

            Minecraft.getInstance().execute(() -> notifyPlayer(latest));
        }
        catch (Exception ignored)
        {
            // Update check is best-effort; never bother the player with failures
        }
    }

    private static String versionsUrl()
    {
        return VERSIONS_API_URL.formatted(
                jsonList(Services.PLATFORM.loaderName()),
                jsonList(SharedConstants.getCurrentVersion().getName()));
    }

    private static String jsonList(String value)
    {
        return URLEncoder.encode("[\"" + value + "\"]", StandardCharsets.UTF_8);
    }

    private static void notifyPlayer(String latestVersion)
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null)
            return;

        Component link = Component.translatable("chat.stackstorage.update.link")
                .withStyle(style -> style
                        .withColor(ChatFormatting.AQUA)
                        .withUnderlined(true)
                        .withClickEvent(new ClickEvent.OpenUrl(URI.create(DOWNLOAD_URL))));

        player.displayClientMessage(Component.translatable("chat.stackstorage.update", latestVersion)
                .withStyle(ChatFormatting.YELLOW)
                .append(" ")
                .append(link), false);
    }
}
