package com.facusora.stackstorage.neoforge.client;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.client.ClientHooks;
import com.facusora.stackstorage.client.QuickStackAnimations;
import com.facusora.stackstorage.client.UpdateChecker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.joml.Matrix3x2fStack;

/** Hands NeoForge's client events to the shared client code. */
public class NeoForgeClientEvents
{
    /** Key categories are registered ids since 1.21.9; the label is key.category.stackstorage.main. */
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(StackStorage.MODID, "main"));

    public static final KeyMapping LOCK_SLOT_KEY = new KeyMapping(
            "key.stackstorage.lock_slot",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_LALT,
            CATEGORY);

    @EventBusSubscriber(modid = StackStorage.MODID, value = Dist.CLIENT)
    public static class ModBus
    {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event)
        {
            event.registerCategory(CATEGORY);
            event.register(LOCK_SLOT_KEY);
        }
    }

    @EventBusSubscriber(modid = StackStorage.MODID, value = Dist.CLIENT)
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
            // (ALT/CTRL/SHIFT) because they count as an active KeyModifier != NONE.
            if (ClientHooks.onKeyPressed(event.getScreen(), event.getKeyCode(), LOCK_SLOT_KEY.getKey().getValue()))
                event.setCanceled(true);
        }

        /**
         * NeoForge 26.2 folded the container screen events into the generic screen
         * one, which fires in screen space: the stars are placed in the menu's own
         * coordinates, so they are shifted to the container's corner first.
         */
        @SubscribeEvent
        public static void onRenderForeground(ScreenEvent.Render.Foreground event)
        {
            if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen))
                return;

            Matrix3x2fStack pose = event.getGuiGraphics().pose();
            pose.pushMatrix();
            pose.translate(screen.getGuiLeft(), screen.getGuiTop());
            ClientHooks.renderLockStars(screen, event.getGuiGraphics());
            pose.popMatrix();
        }

        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event)
        {
            QuickStackAnimations.tick();
        }

        @SubscribeEvent
        public static void onSubmitGeometry(SubmitCustomGeometryEvent event)
        {
            QuickStackAnimations.submit(event.getPoseStack(), event.getLevelRenderState().cameraRenderState.pos,
                    Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false),
                    event.getSubmitNodeCollector());
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
