package com.facusora.stackstorage.quickstack;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ContainerSortHandler
{
    /**
     * Sorts the container of the menu the player currently has open: merges partial
     * stacks of the same item+NBT and orders them by item id. Only chest-like menus
     * (generic 9xN and shulker boxes) are eligible; the player inventory is untouched.
     */
    public static void sortOpenContainer(ServerPlayer player)
    {
        if (!player.isAlive() || player.isSpectator())
            return;

        AbstractContainerMenu menu = player.containerMenu;
        if (!(menu instanceof ChestMenu) && !(menu instanceof ShulkerBoxMenu))
            return;
        if (!menu.stillValid(player))
            return;

        Container container = menu instanceof ChestMenu chestMenu
                ? chestMenu.getContainer()
                : menu.getSlot(0).container; // ShulkerBoxMenu keeps its container private; slot 0 is always the box

        List<MergedStack> merged = collectAndMerge(container);
        merged.sort(Comparator
                .comparing((MergedStack m) -> ForgeRegistries.ITEMS.getKey(m.prototype.getItem()).toString())
                .thenComparing(m -> String.valueOf(m.prototype.getTag()))
                .thenComparing(m -> -m.count));

        refill(container, merged);
        container.setChanged();
    }

    private static List<MergedStack> collectAndMerge(Container container)
    {
        List<MergedStack> merged = new ArrayList<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++)
        {
            ItemStack stack = container.getItem(slot);
            if (stack.isEmpty())
                continue;

            MergedStack match = null;
            for (MergedStack candidate : merged)
            {
                if (ItemStack.isSameItemSameTags(candidate.prototype, stack))
                {
                    match = candidate;
                    break;
                }
            }
            if (match == null)
                merged.add(new MergedStack(stack.copyWithCount(1), stack.getCount()));
            else
                match.count += stack.getCount();
        }
        return merged;
    }

    private static void refill(Container container, List<MergedStack> merged)
    {
        container.clearContent();
        int slot = 0;
        for (MergedStack entry : merged)
        {
            int remaining = entry.count;
            int maxSize = Math.min(entry.prototype.getMaxStackSize(), container.getMaxStackSize());
            while (remaining > 0 && slot < container.getContainerSize())
            {
                int chunk = Math.min(maxSize, remaining);
                container.setItem(slot++, entry.prototype.copyWithCount(chunk));
                remaining -= chunk;
            }
        }
    }

    private static class MergedStack
    {
        final ItemStack prototype;
        int count;

        MergedStack(ItemStack prototype, int count)
        {
            this.prototype = prototype;
            this.count = count;
        }
    }
}
