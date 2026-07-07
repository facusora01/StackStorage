package com.facusora.stackstorage.client;

import com.facusora.stackstorage.StackStorageMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Small "S" button shown on container screens. Texture holds two 12x12 states
 * stacked vertically: normal (v=0) and hovered (v=12).
 */
public class SortButton extends Button
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(StackStorageMod.MODID, "textures/gui/sort_button.png");
    public static final int SIZE = 12;

    public SortButton(int x, int y, OnPress onPress)
    {
        super(x, y, SIZE, SIZE, Component.translatable("button.stackstorage.sort"), onPress, DEFAULT_NARRATION);
        setTooltip(Tooltip.create(Component.translatable("button.stackstorage.sort.tooltip")));
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int v = isHovered ? SIZE : 0;
        guiGraphics.blit(TEXTURE, getX(), getY(), 0, v, SIZE, SIZE, SIZE, SIZE * 2);
    }
}
