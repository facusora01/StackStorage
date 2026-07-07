package com.facusora.stackstorage.client;

import com.facusora.stackstorage.StackStorageMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * On world join, asks the Modrinth API for the newest published Forge/1.20.1
 * version of the mod and, if it is newer than the installed one, shows the
 * player a chat message with a download link. Runs once per game launch,
 * off-thread, and fails silently (no internet, API down, etc.).
 */
@Mod.EventBusSubscriber(modid = StackStorageMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class UpdateChecker
{
    private static final String VERSIONS_API_URL =
            "https://api.modrinth.com/v2/project/stack-storage/version?loaders=%5B%22forge%22%5D&game_versions=%5B%221.20.1%22%5D";
    private static final String DOWNLOAD_URL = "https://modrinth.com/mod/stack-storage/versions";

    private static final AtomicBoolean CHECKED = new AtomicBoolean(false);

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
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
            HttpRequest request = HttpRequest.newBuilder(URI.create(VERSIONS_API_URL))
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
            ArtifactVersion current = ModList.get().getModContainerById(StackStorageMod.MODID)
                    .orElseThrow().getModInfo().getVersion();
            if (new DefaultArtifactVersion(latest).compareTo(current) <= 0)
                return;

            Minecraft.getInstance().execute(() -> notifyPlayer(latest));
        }
        catch (Exception ignored)
        {
            // Update check is best-effort; never bother the player with failures
        }
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
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, DOWNLOAD_URL)));

        player.sendSystemMessage(Component.translatable("chat.stackstorage.update", latestVersion)
                .withStyle(ChatFormatting.YELLOW)
                .append(" ")
                .append(link));
    }
}
