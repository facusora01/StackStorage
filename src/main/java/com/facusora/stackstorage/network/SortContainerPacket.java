package com.facusora.stackstorage.network;

import com.facusora.stackstorage.quickstack.ContainerSortHandler;
import com.facusora.stackstorage.quickstack.SortMode;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Client -> Server. Asks the server to sort the container the sender currently has
 * open, using the layout the player selected on the client. The server validates the
 * open menu itself, so the packet cannot target arbitrary blocks, and an unknown mode
 * id falls back to the alphabetical layout.
 */
public class SortContainerPacket
{
    private final SortMode mode;

    public SortContainerPacket(SortMode mode)
    {
        this.mode = mode;
    }

    public static void encode(SortContainerPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(packet.mode.ordinal());
    }

    public static SortContainerPacket decode(FriendlyByteBuf buf)
    {
        return new SortContainerPacket(SortMode.byId(buf.readVarInt()));
    }

    public static void handle(SortContainerPacket packet, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null)
            {
                ContainerSortHandler.sortOpenContainer(player, packet.mode);
            }
        });
        ctx.setPacketHandled(true);
    }
}
