package com.facusora.stackstorage.network;

import com.facusora.stackstorage.quickstack.ContainerSortHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server. Empty payload: asks the server to sort the container the
 * sender currently has open. The server validates the open menu itself, so the
 * packet cannot target arbitrary blocks.
 */
public class SortContainerPacket
{
    public SortContainerPacket()
    {
    }

    public static void encode(SortContainerPacket packet, FriendlyByteBuf buf)
    {
    }

    public static SortContainerPacket decode(FriendlyByteBuf buf)
    {
        return new SortContainerPacket();
    }

    public static void handle(SortContainerPacket packet, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null)
            {
                ContainerSortHandler.sortOpenContainer(player);
            }
        });
        ctx.setPacketHandled(true);
    }
}
