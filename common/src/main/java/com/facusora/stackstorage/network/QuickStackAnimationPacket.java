package com.facusora.stackstorage.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Server -> Client. Describes what a quick stack just moved and where it went, so
 * every client watching can fly the items from the player into the containers.
 *
 * The client cannot work this out on its own: the server picks the containers and
 * decides how the stacks are split between them.
 */
public class QuickStackAnimationPacket
{
    /** Sanity bound on decode; a normal quick stack is far below this. */
    private static final int MAX_TRANSFERS = 512;

    /** One item flying from the player to one container. */
    public record Transfer(BlockPos target, Item item)
    {
    }

    private final Vec3 origin;
    private final List<Transfer> transfers;

    public QuickStackAnimationPacket(Vec3 origin, List<Transfer> transfers)
    {
        this.origin = origin;
        this.transfers = transfers;
    }

    public Vec3 origin()
    {
        return origin;
    }

    public List<Transfer> transfers()
    {
        return transfers;
    }

    public static void encode(QuickStackAnimationPacket packet, FriendlyByteBuf buf)
    {
        buf.writeDouble(packet.origin.x);
        buf.writeDouble(packet.origin.y);
        buf.writeDouble(packet.origin.z);

        int count = Math.min(packet.transfers.size(), MAX_TRANSFERS);
        buf.writeVarInt(count);
        for (int i = 0; i < count; i++)
        {
            Transfer transfer = packet.transfers.get(i);
            buf.writeBlockPos(transfer.target());
            buf.writeVarInt(Item.getId(transfer.item()));
        }
    }

    public static QuickStackAnimationPacket decode(FriendlyByteBuf buf)
    {
        Vec3 origin = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
        int count = Math.min(buf.readVarInt(), MAX_TRANSFERS);

        List<Transfer> transfers = new ArrayList<>(count);
        for (int i = 0; i < count; i++)
        {
            BlockPos target = buf.readBlockPos();
            Item item = Item.byId(buf.readVarInt());
            if (item != Items.AIR)
                transfers.add(new Transfer(target, item));
        }
        return new QuickStackAnimationPacket(origin, transfers);
    }

}
