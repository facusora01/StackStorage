package com.facusora.stackstorage.client;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import com.facusora.stackstorage.platform.Services;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.ShulkerBoxScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.function.Consumer;

/**
 * What the mod does on the client's screen events. The loaders subscribe to their own
 * versions of these events and hand the pieces over.
 */
public final class ClientHooks
{
    private static final ResourceLocation LOCK_STAR =
            ResourceLocation.fromNamespaceAndPath(StackStorage.MODID, "textures/gui/lock_star.png");

    private ClientHooks()
    {
    }

    /** After a screen is built; {@code addWidget} puts a widget on it. */
    public static void onScreenInit(Screen screen, Consumer<AbstractWidget> addWidget)
    {
        if (screen instanceof InventoryScreen inventory)
        {
            addWidget.accept(new QuickStackButton(inventory,
                    b -> Services.PLATFORM.sendToServer(new QuickStackPacket(SlotLocks.snapshot()))));
        }
        else if (screen instanceof ContainerScreen || screen instanceof ShulkerBoxScreen)
        {
            AbstractContainerScreen<?> container = (AbstractContainerScreen<?>) screen;
            // Top-right corner of the container area, on the title row
            addWidget.accept(new SortButton(
                    container.getGuiLeft() + container.getXSize() - SortButton.SIZE - 7,
                    container.getGuiTop() + 4,
                    mode -> Services.PLATFORM.sendToServer(new SortContainerPacket(mode))));
        }
    }

    /**
     * Before a screen handles a key press. Returns true when the key locked or unlocked
     * the hovered slot, and the screen should not see it.
     */
    public static boolean onKeyPressed(Screen screen, int keyCode, int lockKeyCode)
    {
        if (!(screen instanceof AbstractContainerScreen<?> container) || keyCode != lockKeyCode)
            return false;

        Slot slot = container.getSlotUnderMouse();
        if (slot != null && slot.container instanceof Inventory && slot.getContainerSlot() < 36
                && slot.hasItem())
        {
            SlotLocks.toggle(slot.getItem());
            return true;
        }
        return false;
    }

    /** Over a container screen's slots, in the menu's own coordinates. */
    public static void renderLockStars(AbstractContainerScreen<?> screen, GuiGraphics guiGraphics)
    {
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
