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

import java.util.function.Consumer;

/**
 * Small "S" button shown on container screens. Texture holds two 12x12 states
 * stacked vertically: normal (v=0) and hovered (v=12).
 *
 * Each mouse button is its own layout: left click sorts alphabetically, right click
 * sorts into vertical columns. Nothing is remembered between clicks.
 */
public class SortButton extends Button
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(StackStorageMod.MODID, "textures/gui/sort_button.png");
    public static final int SIZE = 12;

    private static final int RIGHT_BUTTON = 1;

    private final Consumer<SortMode> onSort;
    /** Set by the click that is being handled; anything else (the keyboard) sorts alphabetically. */
    private SortMode pressedMode = SortMode.ALPHABETICAL;

    public SortButton(int x, int y, Consumer<SortMode> onSort)
    {
        super(x, y, SIZE, SIZE, Component.translatable("button.stackstorage.sort"), b -> {}, DEFAULT_NARRATION);
        this.onSort = onSort;
        setTooltip(Tooltip.create(Component.translatable("button.stackstorage.sort.tooltip")));
    }

    @Override
    protected boolean isValidClickButton(int button)
    {
        return button == 0 || button == RIGHT_BUTTON;
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
        pressedMode = button == RIGHT_BUTTON ? SortMode.VERTICAL : SortMode.ALPHABETICAL;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void onPress()
    {
        onSort.accept(pressedMode);
        pressedMode = SortMode.ALPHABETICAL;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        int v = isHovered ? SIZE : 0;
        guiGraphics.blit(TEXTURE, getX(), getY(), 0, v, SIZE, SIZE, SIZE, SIZE * 2);
    }
}
