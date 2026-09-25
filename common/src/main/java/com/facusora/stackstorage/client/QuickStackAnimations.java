package com.facusora.stackstorage.client;

import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Flies the quick stacked items from the player into the containers that took them.
 *
 * Purely visual: the transfer itself already happened server side, and this only
 * reacts to what the server reported. Everyone nearby sees it, which matches the
 * sound the quick stack already plays for the whole area.
 *
 * The loader calls {@link #tick}, {@link #clear} and {@link #render} from its own events.
 */
public class QuickStackAnimations
{
    /** How long one item takes to reach its container. */
    private static final float FLIGHT_TICKS = 24.0F;
    /**
     * Gap between two items heading for the same container. They share a trajectory, so
     * without a real gap they fly stacked on top of each other and read as one item.
     * Items bound for different containers are free to leave together: their paths
     * diverge immediately.
     */
    private static final int SAME_TARGET_STAGGER_TICKS = 5;
    /** Ceiling on that wait, so emptying a full inventory into one chest still ends. */
    private static final int MAX_STAGGER_TICKS = 60;
    private static final float ARC_HEIGHT = 0.35F;
    /**
     * How much of the path is covered at a flat speed: 1 is fully linear, 0 is a hard
     * squared curve. In between the item eases in without lurching at the end.
     */
    private static final float LINEAR_SHARE = 0.6F;
    private static final float SCALE = 0.75F;
    private static final float SPIN_DEGREES_PER_TICK = 10.0F;

    private static final int NO_OUTLINE = 0;

    private static final List<FlyingItem> ACTIVE = new ArrayList<>();
    /** Rebuilt for each item as it is drawn; rendering is single threaded. */
    private static final ItemStackRenderState ITEM_STATE = new ItemStackRenderState();
    private static long clientTicks;

    public static void play(QuickStackAnimationPacket packet)
    {
        Vec3 origin = packet.origin();
        long now = clientTicks;

        // Queue per container rather than over the whole batch: what has to be spread
        // out is the items sharing a trajectory, not the batch as a whole. Seeded from
        // what is still on its way, so a second quick stack falls in behind the first
        // instead of flying through it.
        Map<BlockPos, Long> nextDeparture = new HashMap<>();
        for (FlyingItem item : ACTIVE)
            nextDeparture.merge(item.target, item.startTick + SAME_TARGET_STAGGER_TICKS, Math::max);

        for (QuickStackAnimationPacket.Transfer transfer : packet.transfers())
        {
            BlockPos target = transfer.target();
            long departure = Math.min(
                    Math.max(now, nextDeparture.getOrDefault(target, now)),
                    now + MAX_STAGGER_TICKS);
            nextDeparture.put(target, departure + SAME_TARGET_STAGGER_TICKS);

            ACTIVE.add(new FlyingItem(new ItemStack(transfer.item()), origin, target, departure));
        }
    }

    /** Once per client tick, at the end of it. */
    public static void tick()
    {
        clientTicks++;
        for (Iterator<FlyingItem> it = ACTIVE.iterator(); it.hasNext(); )
            if (it.next().isDone(clientTicks))
                it.remove();
    }

    /** Nothing in flight should survive a disconnect or a world change. */
    public static void clear()
    {
        ACTIVE.clear();
    }

    /** After entities are drawn, with the pose stack still in world space. */
    public static void render(PoseStack pose, Vec3 camera, float partialTick)
    {
        if (ACTIVE.isEmpty())
            return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;

        // Since 1.21.9 models are submitted as render states and drawn in one go by
        // the feature dispatcher, the same pair the level renderer uses for entities
        SubmitNodeStorage nodes = minecraft.gameRenderer.getSubmitNodeStorage();
        float now = clientTicks + partialTick;

        for (FlyingItem item : ACTIVE)
        {
            float progress = item.progress(now);
            if (progress < 0.0F || progress > 1.0F)
                continue; // still waiting its turn, or already landed

            Vec3 position = item.positionAt(progress);
            int light = LevelRenderer.getLightColor(minecraft.level, BlockPos.containing(position));

            pose.pushPose();
            pose.translate(position.x - camera.x, position.y - camera.y, position.z - camera.z);
            pose.scale(SCALE, SCALE, SCALE);
            pose.mulPose(Axis.YP.rotationDegrees(now * SPIN_DEGREES_PER_TICK));
            minecraft.getItemModelResolver().updateForTopItem(ITEM_STATE, item.stack,
                    ItemDisplayContext.GROUND, minecraft.level, null, 0);
            ITEM_STATE.submit(pose, nodes, light, OverlayTexture.NO_OVERLAY, NO_OUTLINE);
            pose.popPose();
        }

        minecraft.gameRenderer.getFeatureRenderDispatcher().renderAllFeatures();
    }

    private static class FlyingItem
    {
        final ItemStack stack;
        final Vec3 origin;
        /** Kept as the block so items heading for one container can share a queue. */
        final BlockPos target;
        final Vec3 destination;
        final long startTick;

        FlyingItem(ItemStack stack, Vec3 origin, BlockPos target, long startTick)
        {
            this.stack = stack;
            this.origin = origin;
            this.target = target;
            this.destination = new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5);
            this.startTick = startTick;
        }

        /** 0 at the player, 1 at the container; negative while the item still waits. */
        float progress(float now)
        {
            return (now - startTick) / FLIGHT_TICKS;
        }

        boolean isDone(long now)
        {
            return now - startTick > FLIGHT_TICKS;
        }

        Vec3 positionAt(float progress)
        {
            // Part flat speed, part squared, so the item picks up pace towards the
            // container without the squared curve's late lurch
            float eased = progress * (LINEAR_SHARE + (1.0F - LINEAR_SHARE) * progress);

            double x = Mth.lerp(eased, origin.x, destination.x);
            double z = Mth.lerp(eased, origin.z, destination.z);
            // A sine bump so the item arcs over instead of sliding along a straight line.
            // Driven by the eased value too, so the peak lands halfway along the path.
            double y = Mth.lerp(eased, origin.y, destination.y)
                    + Math.sin(eased * Math.PI) * ARC_HEIGHT;
            return new Vec3(x, y, z);
        }
    }
}
