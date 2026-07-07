package com.facusora.stackstorage.network;

import com.facusora.stackstorage.quickstack.QuickStackHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Client -> Server. Asks the server to quick stack the sender's inventory into
 * nearby containers. Carries the item types the player locked (sent as items,
 * not slot indices, so a laggy inventory reshuffle can't desync the exclusion).
 * All logic runs server-side.
 */
public class QuickStackPacket
{
    /** Hard cap so a hostile client can't send an unbounded item list. */
    private static final int MAX_LOCKED_ITEMS = 256;

    private final Set<Item> lockedItems;

    public QuickStackPacket(Set<Item> lockedItems)
    {
        this.lockedItems = lockedItems;
    }

    public static void encode(QuickStackPacket packet, FriendlyByteBuf buf)
    {
        buf.writeVarInt(Math.min(packet.lockedItems.size(), MAX_LOCKED_ITEMS));
        packet.lockedItems.stream().limit(MAX_LOCKED_ITEMS)
                .forEach(item -> buf.writeVarInt(Item.getId(item)));
    }

    public static QuickStackPacket decode(FriendlyByteBuf buf)
    {
        int count = Math.min(buf.readVarInt(), MAX_LOCKED_ITEMS);
        Set<Item> lockedItems = new HashSet<>();
        for (int i = 0; i < count; i++)
        {
            Item item = Item.byId(buf.readVarInt());
            if (item != Items.AIR)
                lockedItems.add(item);
        }
        return new QuickStackPacket(lockedItems);
    }

    public static void handle(QuickStackPacket packet, Supplier<NetworkEvent.Context> ctxSupplier)
    {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null)
            {
                QuickStackHandler.performQuickStack(player, packet.lockedItems);
            }
        });
        ctx.setPacketHandled(true);
    }
}
