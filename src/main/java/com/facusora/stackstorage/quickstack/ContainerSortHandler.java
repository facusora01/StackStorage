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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ContainerSortHandler
{
    /** Every container we sort is a 9-wide grid; both chest and shulker menus are. */
    private static final int WIDTH = 9;

    /**
     * Vertical mode needs room for a full armor set (helmet to boots) stacked in one
     * column, so anything shorter than four rows -- single chest, shulker box -- is
     * sorted alphabetically instead.
     */
    private static final int MIN_VERTICAL_ROWS = 4;

    private static final Comparator<MergedStack> BY_ITEM_ID = Comparator
            .comparing((MergedStack m) -> ForgeRegistries.ITEMS.getKey(m.prototype.getItem()).toString())
            .thenComparing(m -> String.valueOf(m.prototype.getTag()))
            .thenComparing(m -> -m.count);

    /**
     * Sorts the container of the menu the player currently has open. Only chest-like
     * menus (generic 9xN and shulker boxes) are eligible; the player inventory is
     * untouched.
     *
     * @param mode the layout the player picked on the client
     */
    public static void sortOpenContainer(ServerPlayer player, SortMode mode)
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

        sortContainer(container, mode);
    }

    /**
     * Rearranges a container in place. Split out from the menu plumbing so the layout
     * can be exercised on a bare container.
     */
    public static void sortContainer(Container container, SortMode mode)
    {
        int size = container.getContainerSize();
        ItemStack[] layout = new ItemStack[size];
        boolean[] reserved = new boolean[size];

        List<MergedStack> leftovers = collectAndMerge(container);
        if (mode == SortMode.VERTICAL && fitsVerticalGrid(size))
            leftovers = layoutColumns(container, leftovers, layout, reserved);

        leftovers.sort(BY_ITEM_ID);
        if (!fillFreeSlots(container, layout, reserved, leftovers))
            return; // could not place everything; leave the container untouched rather than void items

        for (int slot = 0; slot < size; slot++)
            container.setItem(slot, layout[slot] == null ? ItemStack.EMPTY : layout[slot]);
        container.setChanged();
    }

    private static boolean fitsVerticalGrid(int size)
    {
        return size % WIDTH == 0 && size / WIDTH >= MIN_VERTICAL_ROWS;
    }

    /**
     * Gives every recognised material its own column, one row per tier, and reserves
     * that band of the grid so the gaps left by missing tiers stay visible.
     *
     * @return the stacks that found no column and still need placing
     */
    private static List<MergedStack> layoutColumns(Container container, List<MergedStack> merged,
                                                   ItemStack[] layout, boolean[] reserved)
    {
        List<MergedStack> leftovers = new ArrayList<>();
        Map<String, Column> byMaterial = new LinkedHashMap<>();

        for (MergedStack stack : merged)
        {
            ItemFamilies.Family family = ItemFamilies.classify(stack.prototype);
            if (family == null)
            {
                leftovers.add(stack);
                continue;
            }

            Column column = byMaterial.computeIfAbsent(
                    family.kind().ordinal() + ":" + family.material(),
                    key -> new Column(family));
            // Two different items can claim the same row (a gem and a dust of one
            // material, say); the first one keeps the row, the rest fall through.
            if (column.byTier[family.tier()] == null)
                column.byTier[family.tier()] = stack;
            else
                leftovers.add(stack);
        }

        // A mineral on its own is not worth a column of mostly gaps (a lone brick, say);
        // it reads better down in the alphabetical block. Armor keeps its column even as
        // a single piece, because the gaps are exactly what show which pieces are missing.
        List<Column> columns = new ArrayList<>();
        for (Column column : byMaterial.values())
        {
            if (column.family.kind() == ItemFamilies.Kind.ARMOR || column.occupiedTiers() > 1)
                columns.add(column);
            else
                for (MergedStack stack : column.byTier)
                    if (stack != null)
                        leftovers.add(stack);
        }

        if (columns.isEmpty())
            return leftovers;

        columns.sort(Comparator.comparing(c -> c.family, ItemFamilies.COLUMN_ORDER));

        int rows = container.getContainerSize() / WIDTH;
        int placedColumns = Math.min(columns.size(), WIDTH);
        int band = 0;

        for (int c = 0; c < columns.size(); c++)
        {
            Column column = columns.get(c);
            if (c >= placedColumns)
            {
                // More materials than the grid is wide: the rest join the leftovers
                for (MergedStack stack : column.byTier)
                    if (stack != null)
                        leftovers.add(stack);
                continue;
            }

            band = Math.max(band, Math.min(column.family.kind().tiers, rows));
            for (int tier = 0; tier < column.byTier.length; tier++)
            {
                MergedStack stack = column.byTier[tier];
                if (stack == null)
                    continue;
                if (tier >= rows)
                {
                    leftovers.add(stack); // tier sits deeper than this container is tall
                    continue;
                }

                int maxSize = Math.min(stack.prototype.getMaxStackSize(), container.getMaxStackSize());
                int chunk = Math.min(maxSize, stack.count);
                layout[tier * WIDTH + c] = stack.prototype.copyWithCount(chunk);
                if (stack.count > chunk)
                    leftovers.add(new MergedStack(stack.prototype, stack.count - chunk));
            }
        }

        for (int row = 0; row < band; row++)
            for (int c = 0; c < placedColumns; c++)
                reserved[row * WIDTH + c] = true;

        return leftovers;
    }

    /**
     * Fills the stacks that are not part of a column into the free slots, in reading
     * order. Reserved gaps are skipped on the first pass and only used as a last
     * resort, because keeping the columns aligned is never worth destroying items over.
     *
     * @return false if there was not enough room, meaning the sort must be abandoned
     */
    private static boolean fillFreeSlots(Container container, ItemStack[] layout, boolean[] reserved,
                                         List<MergedStack> leftovers)
    {
        for (boolean useReserved : new boolean[]{false, true})
        {
            int slot = 0;
            for (MergedStack entry : leftovers)
            {
                while (entry.count > 0)
                {
                    while (slot < layout.length && (layout[slot] != null || (reserved[slot] && !useReserved)))
                        slot++;
                    if (slot >= layout.length)
                        break;

                    int maxSize = Math.min(entry.prototype.getMaxStackSize(), container.getMaxStackSize());
                    int chunk = Math.min(maxSize, entry.count);
                    layout[slot] = entry.prototype.copyWithCount(chunk);
                    entry.count -= chunk;
                }
            }

            leftovers.removeIf(entry -> entry.count <= 0);
            if (leftovers.isEmpty())
                return true;
        }
        return leftovers.isEmpty();
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

    /** One column: the stacks sharing a material, indexed by tier. */
    private static class Column
    {
        final ItemFamilies.Family family;
        final MergedStack[] byTier;

        Column(ItemFamilies.Family family)
        {
            this.family = family;
            this.byTier = new MergedStack[family.kind().tiers];
        }

        int occupiedTiers()
        {
            int count = 0;
            for (MergedStack stack : byTier)
                if (stack != null)
                    count++;
            return count;
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
