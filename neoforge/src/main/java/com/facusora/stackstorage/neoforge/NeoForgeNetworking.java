package com.facusora.stackstorage.neoforge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.BiConsumer;

/**
 * NeoForge sends payloads rather than bare messages, so each shared packet travels
 * inside a thin record that carries its channel id.
 */
public class NeoForgeNetworking
{
    private static final String PROTOCOL_VERSION = "1";

    public record QuickStack(QuickStackPacket packet) implements CustomPacketPayload
    {
        static final Type<QuickStack> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StackStorage.MODID, "quick_stack"));
        static final StreamCodec<FriendlyByteBuf, QuickStack> CODEC = StreamCodec.of(
                (buf, payload) -> QuickStackPacket.encode(payload.packet(), buf),
                buf -> new QuickStack(QuickStackPacket.decode(buf)));

        @Override
        public Type<QuickStack> type()
        {
            return TYPE;
        }
    }

    public record Sort(SortContainerPacket packet) implements CustomPacketPayload
    {
        static final Type<Sort> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StackStorage.MODID, "sort_container"));
        static final StreamCodec<FriendlyByteBuf, Sort> CODEC = StreamCodec.of(
                (buf, payload) -> SortContainerPacket.encode(payload.packet(), buf),
                buf -> new Sort(SortContainerPacket.decode(buf)));

        @Override
        public Type<Sort> type()
        {
            return TYPE;
        }
    }

    public record Animation(QuickStackAnimationPacket packet) implements CustomPacketPayload
    {
        static final Type<Animation> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(StackStorage.MODID, "quick_stack_animation"));
        static final StreamCodec<FriendlyByteBuf, Animation> CODEC = StreamCodec.of(
                (buf, payload) -> QuickStackAnimationPacket.encode(payload.packet(), buf),
                buf -> new Animation(QuickStackAnimationPacket.decode(buf)));

        @Override
        public Type<Animation> type()
        {
            return TYPE;
        }
    }

    static void register(RegisterPayloadHandlersEvent event)
    {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToServer(QuickStack.TYPE, QuickStack.CODEC,
                onServer((payload, player) -> payload.packet().handle(player)));
        registrar.playToServer(Sort.TYPE, Sort.CODEC,
                onServer((payload, player) -> payload.packet().handle(player)));
        registrar.playToClient(Animation.TYPE, Animation.CODEC,
                // The call sits inside a lambda so the server never loads the client-only class
                (payload, ctx) -> ctx.enqueueWork(
                        () -> com.facusora.stackstorage.client.QuickStackAnimations.play(payload.packet())));
    }

    /** Runs a client -> server payload on the server thread, for the player who sent it. */
    private static <T extends CustomPacketPayload> IPayloadHandler<T> onServer(BiConsumer<T, ServerPlayer> handler)
    {
        return (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player)
                handler.accept(payload, player);
        });
    }
}
