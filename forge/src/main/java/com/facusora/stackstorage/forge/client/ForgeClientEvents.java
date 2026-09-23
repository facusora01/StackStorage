package com.facusora.stackstorage.forge.client;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.client.ClientHooks;
import com.facusora.stackstorage.client.QuickStackAnimations;
import com.facusora.stackstorage.client.UpdateChecker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** Hands Forge's client events to the shared client code. */
public class ForgeClientEvents
{
    public static final KeyMapping LOCK_SLOT_KEY = new KeyMapping(
            "key.stackstorage.lock_slot",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            "key.categories.stackstorage");

    @Mod.EventBusSubscriber(modid = StackStorage.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModBus
    {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
        {
            event.register(LOCK_SLOT_KEY);
        }
    }

    @Mod.EventBusSubscriber(modid = StackStorage.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class GameBus
    {
        @SubscribeEvent
        public static void onScreenInit(ScreenEvent.Init.Post event)
        {
            ClientHooks.onScreenInit(event.getScreen(), event::addListener);
        }

        @SubscribeEvent
        public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event)
        {
            // Direct keycode comparison: isActiveAndMatches() rejects bare modifier keys
            // (ALT/CTRL/SHIFT) because Forge treats them as an active KeyModifier != NONE.
            if (ClientHooks.onKeyPressed(event.getScreen(), event.getKeyCode(), LOCK_SLOT_KEY.getKey().getValue()))
                event.setCanceled(true);
        }

        @SubscribeEvent
        public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event)
        {
            ClientHooks.renderLockStars(event.getContainerScreen(), event.getGuiGraphics());
        }

        @SubscribeEvent
        public static void onClientTick(TickEvent.ClientTickEvent event)
        {
            if (event.phase == TickEvent.Phase.END)
                QuickStackAnimations.tick();
        }

        @SubscribeEvent
        public static void onRenderLevel(RenderLevelStageEvent event)
        {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES)
                QuickStackAnimations.render(event.getPoseStack(), event.getCamera().getPosition(), event.getPartialTick());
        }

        @SubscribeEvent
        public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event)
        {
            UpdateChecker.onLoggingIn();
        }

        /** Nothing in flight should survive a disconnect or a world change. */
        @SubscribeEvent
        public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event)
        {
            QuickStackAnimations.clear();
        }
    }
}
