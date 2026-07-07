package com.facusora.stackstorage.client;

import com.facusora.stackstorage.StackStorageMod;
import com.facusora.stackstorage.network.ModNetworking;
import com.facusora.stackstorage.network.QuickStackPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = StackStorageMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientEvents
{
    private static final ResourceLocation LOCK_STAR =
            new ResourceLocation(StackStorageMod.MODID, "textures/gui/lock_star.png");

    @SubscribeEvent
    public static void onScreenInit(ScreenEvent.Init.Post event)
    {
        if (!(event.getScreen() instanceof InventoryScreen screen))
            return;

        event.addListener(new QuickStackButton(screen,
                b -> ModNetworking.CHANNEL.sendToServer(new QuickStackPacket(SlotLocks.snapshot()))));
    }

    @SubscribeEvent
    public static void onKeyPressed(ScreenEvent.KeyPressed.Pre event)
    {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen))
            return;
        // Direct keycode comparison: isActiveAndMatches() rejects bare modifier keys
        // (ALT/CTRL/SHIFT) because Forge treats them as an active KeyModifier != NONE.
        if (ClientModEvents.LOCK_SLOT_KEY.getKey().getValue() != event.getKeyCode())
            return;

        Slot slot = screen.getSlotUnderMouse();
        if (slot != null && slot.container instanceof Inventory && slot.getContainerSlot() < 36
                && slot.hasItem())
        {
            SlotLocks.toggle(slot.getItem());
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRenderForeground(ContainerScreenEvent.Render.Foreground event)
    {
        AbstractContainerScreen<?> screen = event.getContainerScreen();
        GuiGraphics guiGraphics = event.getGuiGraphics();

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0, 0, 300);
        for (Slot slot : screen.getMenu().slots)
        {
            if (slot.container instanceof Inventory && slot.getContainerSlot() < 36
                    && SlotLocks.isLocked(slot.getItem()))
            {
                guiGraphics.blit(LOCK_STAR, slot.x - 2, slot.y - 2, 0, 0, 8, 8, 8, 8);
            }
        }
        guiGraphics.pose().popPose();
    }
}
