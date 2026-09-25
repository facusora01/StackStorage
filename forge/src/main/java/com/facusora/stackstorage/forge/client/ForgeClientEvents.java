package com.facusora.stackstorage.forge.client;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.client.ClientHooks;
import com.facusora.stackstorage.client.QuickStackAnimations;
import com.facusora.stackstorage.client.UpdateChecker;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

/**
 * Hands Forge's client events to the shared client code.
 *
 * EventBus 7 (Forge 1.21.6+) gives every event its own bus, so the listeners are
 * added here one by one instead of through annotated subscriber classes. Only
 * called on the client, so the server never loads this class.
 */
public final class ForgeClientEvents
{
    /** Key categories are registered ids since 1.21.9; the label is key.category.stackstorage.main. */
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(StackStorage.MODID, "main"));

    public static final KeyMapping LOCK_SLOT_KEY = new KeyMapping(
            "key.stackstorage.lock_slot",
            KeyConflictContext.GUI,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_LEFT_ALT,
            CATEGORY,
            0);

    private ForgeClientEvents()
    {
    }

    public static void register(BusGroup modBusGroup)
    {
        RegisterKeyMappingsEvent.getBus(modBusGroup).addListener(event -> event.register(LOCK_SLOT_KEY));

        ScreenEvent.Init.Post.BUS.addListener(event ->
                ClientHooks.onScreenInit(event.getScreen(), event::addListener));

        // Direct keycode comparison: isActiveAndMatches() rejects bare modifier keys
        // (ALT/CTRL/SHIFT) because Forge treats them as an active KeyModifier != NONE.
        // Returning true cancels the event, so the screen never sees the key.
        Predicate<ScreenEvent.KeyPressed.Pre> lockSlot = event ->
                ClientHooks.onKeyPressed(event.getScreen(), event.getKeyCode(), LOCK_SLOT_KEY.getKey().getValue());
        ScreenEvent.KeyPressed.Pre.BUS.addListener(lockSlot);

        ContainerScreenEvent.Render.Foreground.BUS.addListener(event ->
                ClientHooks.renderLockStars(event.getContainerScreen(), event.getGuiGraphics()));

        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> QuickStackAnimations.tick());

        ClientPlayerNetworkEvent.LoggingIn.BUS.addListener(event -> UpdateChecker.onLoggingIn());

        // Nothing in flight should survive a disconnect or a world change
        ClientPlayerNetworkEvent.LoggingOut.BUS.addListener(event -> QuickStackAnimations.clear());
    }
}
