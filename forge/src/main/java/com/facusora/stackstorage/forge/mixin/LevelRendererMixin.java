package com.facusora.stackstorage.forge.mixin;

import com.facusora.stackstorage.client.QuickStackAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Forge 1.21.3 dropped RenderLevelStageEvent and only brought a replacement back in
 * 1.21.4, so the flying items are drawn straight after the entities instead.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin
{
    @Inject(method = "renderEntities", at = @At("TAIL"))
    private void stackstorage$renderQuickStackItems(PoseStack pose, MultiBufferSource.BufferSource buffers,
            Camera camera, DeltaTracker deltaTracker, List<Entity> entities, CallbackInfo ci)
    {
        QuickStackAnimations.render(pose, camera.getPosition(), deltaTracker.getGameTimeDeltaPartialTick(false));
    }
}
