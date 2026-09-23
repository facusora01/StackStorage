package com.facusora.stackstorage.forge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ForgeNetworking
{
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StackStorage.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register()
    {
        CHANNEL.registerMessage(0, QuickStackPacket.class,
                QuickStackPacket::encode,
                QuickStackPacket::decode,
                onServer(QuickStackPacket::handle));
        CHANNEL.registerMessage(1, SortContainerPacket.class,
                SortContainerPacket::encode,
                SortContainerPacket::decode,
                onServer(SortContainerPacket::handle));
        CHANNEL.registerMessage(2, QuickStackAnimationPacket.class,
                QuickStackAnimationPacket::encode,
                QuickStackAnimationPacket::decode,
                ForgeNetworking::handleAnimation,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    /** Runs a client -> server packet on the server thread, for the player who sent it. */
    private static <T> BiConsumer<T, Supplier<NetworkEvent.Context>> onServer(BiConsumer<T, ServerPlayer> handler)
    {
        return (packet, ctxSupplier) -> {
            NetworkEvent.Context ctx = ctxSupplier.get();
            ctx.enqueueWork(() -> {
                ServerPlayer player = ctx.getSender();
                if (player != null)
                    handler.accept(packet, player);
            });
            ctx.setPacketHandled(true);
        };
    }

    private static void handleAnimation(QuickStackAnimationPacket packet, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        // Wrapped so the server never loads the client-only animation class
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> com.facusora.stackstorage.client.QuickStackAnimations.play(packet)));
        ctx.setPacketHandled(true);
    }
}
