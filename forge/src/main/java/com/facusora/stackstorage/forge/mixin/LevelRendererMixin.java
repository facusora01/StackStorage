package com.facusora.stackstorage.forge.mixin;

import com.facusora.stackstorage.client.QuickStackAnimations;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Forge dropped RenderLevelStageEvent in 1.21.3 and never brought an equivalent
 * back, so the flying items are drawn straight after the entities instead. Since
 * 1.21.9 entities are submitted rather than rendered, hence submitEntities.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin
{
    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void stackstorage$renderQuickStackItems(PoseStack pose, LevelRenderState state,
            SubmitNodeCollector nodes, CallbackInfo ci)
    {
        QuickStackAnimations.render(pose, state.cameraRenderState.pos,
                Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}
