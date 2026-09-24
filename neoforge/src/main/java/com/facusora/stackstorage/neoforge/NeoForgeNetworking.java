package com.facusora.stackstorage.neoforge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

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
        static final ResourceLocation ID = new ResourceLocation(StackStorage.MODID, "quick_stack");

        @Override
        public void write(FriendlyByteBuf buf)
        {
            QuickStackPacket.encode(packet, buf);
        }

        @Override
        public ResourceLocation id()
        {
            return ID;
        }
    }

    public record Sort(SortContainerPacket packet) implements CustomPacketPayload
    {
        static final ResourceLocation ID = new ResourceLocation(StackStorage.MODID, "sort_container");

        @Override
        public void write(FriendlyByteBuf buf)
        {
            SortContainerPacket.encode(packet, buf);
        }

        @Override
        public ResourceLocation id()
        {
            return ID;
        }
    }

    public record Animation(QuickStackAnimationPacket packet) implements CustomPacketPayload
    {
        static final ResourceLocation ID = new ResourceLocation(StackStorage.MODID, "quick_stack_animation");

        @Override
        public void write(FriendlyByteBuf buf)
        {
            QuickStackAnimationPacket.encode(packet, buf);
        }

        @Override
        public ResourceLocation id()
        {
            return ID;
        }
    }

    static void register(RegisterPayloadHandlerEvent event)
    {
        IPayloadRegistrar registrar = event.registrar(StackStorage.MODID).versioned(PROTOCOL_VERSION);
        registrar.play(QuickStack.ID, buf -> new QuickStack(QuickStackPacket.decode(buf)),
                handler -> handler.server(onServer((payload, player) -> payload.packet().handle(player))));
        registrar.play(Sort.ID, buf -> new Sort(SortContainerPacket.decode(buf)),
                handler -> handler.server(onServer((payload, player) -> payload.packet().handle(player))));
        registrar.play(Animation.ID, buf -> new Animation(QuickStackAnimationPacket.decode(buf)),
                // The call sits inside a lambda so the server never loads the client-only class
                handler -> handler.client((payload, ctx) -> ctx.workHandler().execute(
                        () -> com.facusora.stackstorage.client.QuickStackAnimations.play(payload.packet()))));
    }

    /** Runs a client -> server payload on the server thread, for the player who sent it. */
    private static <T extends CustomPacketPayload> net.neoforged.neoforge.network.handling.IPlayPayloadHandler<T> onServer(
            BiConsumer<T, ServerPlayer> handler)
    {
        return (payload, ctx) -> ctx.workHandler().execute(() -> {
            if (ctx.player().orElse(null) instanceof ServerPlayer player)
                handler.accept(payload, player);
        });
    }
}
