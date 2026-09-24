package com.facusora.stackstorage.forge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.SimpleChannel;

import java.util.function.BiConsumer;

public class ForgeNetworking
{
    private static final int PROTOCOL_VERSION = 1;

    public static final SimpleChannel CHANNEL = ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(StackStorage.MODID, "main"))
            .networkProtocolVersion(PROTOCOL_VERSION)
            .simpleChannel();

    public static void register()
    {
        CHANNEL.messageBuilder(QuickStackPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(QuickStackPacket::encode)
                .decoder(QuickStackPacket::decode)
                .consumerMainThread(onServer(QuickStackPacket::handle))
                .add();
        CHANNEL.messageBuilder(SortContainerPacket.class, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SortContainerPacket::encode)
                .decoder(SortContainerPacket::decode)
                .consumerMainThread(onServer(SortContainerPacket::handle))
                .add();
        CHANNEL.messageBuilder(QuickStackAnimationPacket.class, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(QuickStackAnimationPacket::encode)
                .decoder(QuickStackAnimationPacket::decode)
                .consumerMainThread(ForgeNetworking::handleAnimation)
                .add();
    }

    /** Runs a client -> server packet on the server thread, for the player who sent it. */
    private static <T> BiConsumer<T, CustomPayloadEvent.Context> onServer(BiConsumer<T, ServerPlayer> handler)
    {
        return (packet, ctx) -> {
            ServerPlayer player = ctx.getSender();
            if (player != null)
                handler.accept(packet, player);
        };
    }

    @SuppressWarnings("removal")
    private static void handleAnimation(QuickStackAnimationPacket packet, CustomPayloadEvent.Context ctx)
    {
        // Wrapped so the server never loads the client-only animation class
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.facusora.stackstorage.client.QuickStackAnimations.play(packet));
    }
}
