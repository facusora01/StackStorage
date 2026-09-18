package com.facusora.stackstorage.quickstack;

import com.facusora.stackstorage.Config;
import com.facusora.stackstorage.network.ModNetworking;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuickStackHandler
{
    private static final int COOLDOWN_TICKS = 10;
    private static final String COOLDOWN_KEY = "stackstorage:quick_stack_ready_at";

    /** Extra blocks past the search radius, so onlookers standing back still see the items. */
    private static final int VIEW_MARGIN = 32;


    /**
     * Moves items from the player's main inventory + hotbar into nearby containers,
     * but only topping off stacks of item types the container already holds.
     *
     * @param lockedItems item types the player marked as locked; stacks of these are never touched
     */
    public static void performQuickStack(ServerPlayer player, Set<Item> lockedItems)
    {
        if (!player.isAlive() || player.isSpectator())
            return;
        if (isOnCooldown(player))
            return;

        ServerLevel level = player.serverLevel();
        int radius = Config.quickStackRadius;

        List<ContainerEntry> containers = findNearbyContainers(level, player, radius);
        if (containers.isEmpty())
            return;

        // Closest containers absorb items first
        containers.sort((a, b) -> Double.compare(a.distSqr, b.distSqr));

        List<QuickStackAnimationPacket.Transfer> transfers = new ArrayList<>();
        List<ItemStack> playerItems = player.getInventory().items; // 36 slots: main + hotbar
        for (ItemStack stack : playerItems)
        {
            if (stack.isEmpty() || lockedItems.contains(stack.getItem()))
                continue;

            Item item = stack.getItem(); // captured before the stack can be emptied
            for (ContainerEntry entry : containers)
            {
                if (transferToContainer(entry.container, stack))
                {
                    entry.dirty = true;
                    transfers.add(new QuickStackAnimationPacket.Transfer(entry.pos, item));
                }
                if (stack.isEmpty())
                    break;
            }
        }

        for (ContainerEntry entry : containers)
        {
            if (entry.dirty)
                entry.container.setChanged();
        }

        if (!transfers.isEmpty())
        {
            player.getInventory().setChanged();
            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.2F);
            sendAnimation(player, level, radius, transfers);
        }
    }

    /**
     * Tells every client watching the area to fly the items across. Sent to the whole
     * area rather than just the sender, since the quick stack sound already carries
     * that far and a silent-but-visible mismatch would read as a bug.
     */
    private static void sendAnimation(ServerPlayer player, ServerLevel level, int radius,
                                      List<QuickStackAnimationPacket.Transfer> transfers)
    {
        Vec3 origin = new Vec3(player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ());
        ModNetworking.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        player.getX(), player.getY(), player.getZ(),
                        radius + VIEW_MARGIN, level.dimension())),
                new QuickStackAnimationPacket(origin, transfers));
    }

    /**
     * Quick stacking scans the block entities of every chunk in range, so a client
     * holding the button down would have the server sweeping them every tick and the
     * whole area watching items fly non stop.
     */
    private static boolean isOnCooldown(ServerPlayer player)
    {
        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        long readyAt = data.getLong(COOLDOWN_KEY);

        // A value far in the future means it came from another world, not a real cooldown
        if (readyAt > now && readyAt <= now + COOLDOWN_TICKS)
            return true;

        data.putLong(COOLDOWN_KEY, now + COOLDOWN_TICKS);
        return false;
    }

    /**
     * Collects valid containers by iterating block entities of the chunks in range
     * instead of scanning every block position: a 64-block radius touches ~81 chunk
     * lookups rather than millions of block reads, so a hostile client spamming the
     * packet cannot stall the server.
     */
    private static List<ContainerEntry> findNearbyContainers(ServerLevel level, ServerPlayer player, int radius)
    {
        List<ContainerEntry> containers = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        double radiusSqr = radius * radius;
        BlockPos origin = player.blockPosition();

        int minChunkX = (origin.getX() - radius) >> 4;
        int maxChunkX = (origin.getX() + radius) >> 4;
        int minChunkZ = (origin.getZ() - radius) >> 4;
        int maxChunkZ = (origin.getZ() + radius) >> 4;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
        {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
            {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null)
                    continue; // never force-load chunks for a quick stack

                for (BlockEntity blockEntity : chunk.getBlockEntities().values())
                {
                    BlockPos pos = blockEntity.getBlockPos();
                    if (blockEntity.isRemoved() || visited.contains(pos))
                        continue;

                    double distSqr = pos.distToCenterSqr(player.getX(), player.getY(), player.getZ());
                    if (distSqr > radiusSqr)
                        continue;

                    BlockState state = blockEntity.getBlockState();
                    Container container = null;

                    if (state.getBlock() instanceof ChestBlock chestBlock)
                    {
                        // Resolves double chests into one combined 54-slot container; null if blocked (cat/solid block on top)
                        container = ChestBlock.getContainer(chestBlock, state, level, pos, false);
                        visited.add(pos);
                        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE)
                            visited.add(pos.relative(ChestBlock.getConnectedDirection(state)));
                    }
                    else if (state.getBlock() instanceof BarrelBlock || state.getBlock() instanceof ShulkerBoxBlock)
                    {
                        if (blockEntity instanceof Container c)
                            container = c;
                    }

                    if (container != null)
                        containers.add(new ContainerEntry(container, pos, distSqr));
                }
            }
        }
        return containers;
    }

    /**
     * Moves the given stack into {@code container}, shrinking {@code stack} in place.
     * First tops off existing stacks of the same item (same item + same NBT); if the
     * container already held that item, the remainder also goes into empty slots.
     * A container that never held the item is left untouched.
     *
     * @return true if at least one item was moved
     */
    private static boolean transferToContainer(Container container, ItemStack stack)
    {
        boolean moved = false;
        boolean containsType = false;
        int size = container.getContainerSize();

        for (int slot = 0; slot < size && !stack.isEmpty(); slot++)
        {
            ItemStack target = container.getItem(slot);
            if (target.isEmpty() || !ItemStack.isSameItemSameTags(target, stack))
                continue;
            containsType = true;
            if (!container.canPlaceItem(slot, stack))
                continue;

            int maxSize = Math.min(target.getMaxStackSize(), container.getMaxStackSize());
            int room = maxSize - target.getCount();
            if (room <= 0)
                continue;

            int toMove = Math.min(room, stack.getCount());
            target.grow(toMove);
            stack.shrink(toMove);
            moved = true;
        }

        if (containsType)
        {
            for (int slot = 0; slot < size && !stack.isEmpty(); slot++)
            {
                if (!container.getItem(slot).isEmpty() || !container.canPlaceItem(slot, stack))
                    continue;

                int toMove = Math.min(Math.min(stack.getMaxStackSize(), container.getMaxStackSize()), stack.getCount());
                container.setItem(slot, stack.split(toMove));
                moved = true;
            }
        }
        return moved;
    }

    private static class ContainerEntry
    {
        final Container container;
        /** Where the items should fly to; for a double chest, the half we found first. */
        final BlockPos pos;
        final double distSqr;
        boolean dirty = false;

        ContainerEntry(Container container, BlockPos pos, double distSqr)
        {
            this.container = container;
            this.pos = pos;
            this.distSqr = distSqr;
        }
    }
}
