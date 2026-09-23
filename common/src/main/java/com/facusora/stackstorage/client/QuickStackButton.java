package com.facusora.stackstorage.client;

import com.facusora.stackstorage.StackStorage;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Icon button styled like the recipe book toggle. The texture holds two 20x18
 * states stacked vertically: normal (v=0) and hovered/focused (v=18), with the
 * quick stack icon baked in.
 *
 * The position is re-derived from the screen every frame because InventoryScreen
 * shifts horizontally when the recipe book opens without re-running init.
 */
public class QuickStackButton extends Button
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(StackStorage.MODID, "textures/gui/quick_stack_button.png");
    // Texture is authored at 2x the logical size for crispness at higher GUI scales
    private static final int TEX_WIDTH = 40;
    private static final int TEX_HEIGHT = 72;
    private static final int STATE_HEIGHT = 36;
    public static final int WIDTH = 20;
    public static final int HEIGHT = 18;

    // Offset from the GUI corner: below the 2x2 crafting grid, right of the recipe book toggle.
    private static final int X_OFFSET = 136;
    private static final int Y_OFFSET = 61;

    private final InventoryScreen screen;

    public QuickStackButton(InventoryScreen screen, OnPress onPress)
    {
        super(screen.getGuiLeft() + X_OFFSET, screen.getGuiTop() + Y_OFFSET, WIDTH, HEIGHT,
                Component.translatable("button.stackstorage.quick_stack"), onPress, DEFAULT_NARRATION);
        this.screen = screen;
        setTooltip(Tooltip.create(Component.translatable("button.stackstorage.quick_stack.tooltip")));
    }

    /**
     * Keeps the button out of the Tab cycle. A focused widget shows its tooltip, so
     * otherwise tabbing around the inventory pops the help text open over the slots.
     */
    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event)
    {
        return null;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        setX(screen.getGuiLeft() + X_OFFSET);
        setY(screen.getGuiTop() + Y_OFFSET);
        // isHovered only: clicking focuses the widget, which would leave
        // isHoveredOrFocused() (and the hover art) stuck on after the click
        float v = isHovered ? STATE_HEIGHT : 0;
        guiGraphics.blit(TEXTURE, getX(), getY(), WIDTH, HEIGHT, 0, v, TEX_WIDTH, STATE_HEIGHT, TEX_WIDTH, TEX_HEIGHT);
    }
}
