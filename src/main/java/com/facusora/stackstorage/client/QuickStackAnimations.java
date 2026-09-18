package com.facusora.stackstorage.client;

import com.facusora.stackstorage.StackStorageMod;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Flies the quick stacked items from the player into the containers that took them.
 *
 * Purely visual: the transfer itself already happened server side, and this only
 * reacts to what the server reported. Everyone nearby sees it, which matches the
 * sound the quick stack already plays for the whole area.
 */
@Mod.EventBusSubscriber(modid = StackStorageMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class QuickStackAnimations
{
    /** How long one item takes to reach its container. */
    private static final float FLIGHT_TICKS = 24.0F;
    /** Items leave one after another rather than in lockstep, up to this much of a wait. */
    private static final int MAX_STAGGER_TICKS = 40;
    private static final float ARC_HEIGHT = 0.9F;
    private static final float SCALE = 0.75F;
    private static final float SPIN_DEGREES_PER_TICK = 10.0F;

    private static final List<FlyingItem> ACTIVE = new ArrayList<>();
    private static long clientTicks;

    public static void play(QuickStackAnimationPacket packet)
    {
        Vec3 origin = packet.origin();
        List<QuickStackAnimationPacket.Transfer> transfers = packet.transfers();

        for (int i = 0; i < transfers.size(); i++)
        {
            QuickStackAnimationPacket.Transfer transfer = transfers.get(i);
            BlockPos target = transfer.target();
            ACTIVE.add(new FlyingItem(
                    new ItemStack(transfer.item()),
                    origin,
                    new Vec3(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5),
                    clientTicks + Math.min(i, MAX_STAGGER_TICKS)));
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END)
            return;

        clientTicks++;
        for (Iterator<FlyingItem> it = ACTIVE.iterator(); it.hasNext(); )
            if (it.next().isDone(clientTicks))
                it.remove();
    }

    /** Nothing in flight should survive a disconnect or a world change. */
    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
    {
        ACTIVE.clear();
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event)
    {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES || ACTIVE.isEmpty())
            return;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null)
            return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        float now = clientTicks + event.getPartialTick();

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
            minecraft.getItemRenderer().renderStatic(item.stack, ItemDisplayContext.GROUND,
                    light, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                    pose, buffers, minecraft.level, 0);
            pose.popPose();
        }

        buffers.endBatch();
    }

    private static class FlyingItem
    {
        final ItemStack stack;
        final Vec3 origin;
        final Vec3 target;
        final long startTick;

        FlyingItem(ItemStack stack, Vec3 origin, Vec3 target, long startTick)
        {
            this.stack = stack;
            this.origin = origin;
            this.target = target;
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
            // Squared so the item drifts out of the player and then pulls into the
            // container, rather than covering the distance at a flat speed
            float eased = progress * progress;

            double x = Mth.lerp(eased, origin.x, target.x);
            double z = Mth.lerp(eased, origin.z, target.z);
            // A sine bump so the item arcs over instead of sliding along a straight line.
            // Driven by the eased value too, so the peak lands halfway along the path.
            double y = Mth.lerp(eased, origin.y, target.y)
                    + Math.sin(eased * Math.PI) * ARC_HEIGHT;
            return new Vec3(x, y, z);
        }
    }
}
