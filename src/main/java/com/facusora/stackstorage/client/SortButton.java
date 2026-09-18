package com.facusora.stackstorage.client;

import com.facusora.stackstorage.StackStorageMod;
import com.facusora.stackstorage.quickstack.SortMode;
import net.minecraft.client.gui.ComponentPath;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.navigation.FocusNavigationEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Small "S" button shown on container screens. Texture holds two 12x12 states
 * stacked vertically: normal (v=0) and hovered (v=12).
 *
 * Left click sorts with the layout the player has selected; right click switches to
 * the next layout and sorts with it straight away.
 */
public class SortButton extends Button
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(StackStorageMod.MODID, "textures/gui/sort_button.png");
    public static final int SIZE = 12;

    private SortMode tooltipMode;

    public SortButton(int x, int y, OnPress onPress)
    {
        super(x, y, SIZE, SIZE, Component.translatable("button.stackstorage.sort"), onPress, DEFAULT_NARRATION);
        refreshTooltip();
    }

    @Override
    protected boolean isValidClickButton(int button)
    {
        return button == 0 || button == 1;
    }

    /**
     * Keeps the button out of the Tab cycle. A focused widget shows its tooltip, so
     * otherwise tabbing around the chest screen pops the help text open over the slots.
     */
    @Nullable
    @Override
    public ComponentPath nextFocusPath(FocusNavigationEvent event)
    {
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        // Cycle first, then let the normal press path run, so one right click both
        // switches the layout and applies it
        if (button == 1 && this.active && this.visible && clicked(mouseX, mouseY))
            SortPreferences.cycle();

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        if (tooltipMode != SortPreferences.current())
            refreshTooltip();

        int v = isHovered ? SIZE : 0;
        guiGraphics.blit(TEXTURE, getX(), getY(), 0, v, SIZE, SIZE, SIZE, SIZE * 2);
    }

    private void refreshTooltip()
    {
        tooltipMode = SortPreferences.current();
        String key = tooltipMode == SortMode.VERTICAL
                ? "button.stackstorage.sort.tooltip.vertical"
                : "button.stackstorage.sort.tooltip.alphabetical";
        setTooltip(Tooltip.create(Component.translatable(key)));
    }
}
