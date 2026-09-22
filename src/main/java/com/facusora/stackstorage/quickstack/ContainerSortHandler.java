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

    private static final Comparator<ItemStack> BY_ITEM_ID = Comparator
            .comparing((ItemStack s) -> ForgeRegistries.ITEMS.getKey(s.getItem()).toString())
            .thenComparing(s -> String.valueOf(s.getTag()));

    private static final Comparator<MergedStack> MERGED_BY_ITEM_ID = Comparator
            .comparing((MergedStack m) -> m.prototype, BY_ITEM_ID)
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
        List<MergedStack> merged = collectAndMerge(container);

        boolean placed = mode == SortMode.VERTICAL && fitsVerticalGrid(size)
                ? arrangeInColumns(container, merged, layout, reserved)
                : arrangeAlphabetically(container, merged, layout);
        if (!placed)
            return; // could not place everything; leave the container untouched rather than void items

        for (int slot = 0; slot < size; slot++)
            container.setItem(slot, layout[slot] == null ? ItemStack.EMPTY : layout[slot]);
        container.setChanged();
    }

    private static boolean fitsVerticalGrid(int size)
    {
        return size % WIDTH == 0 && size / WIDTH >= MIN_VERTICAL_ROWS;
    }

    /** Merged stacks by item id, packed from the first slot in reading order. */
    private static boolean arrangeAlphabetically(Container container, List<MergedStack> merged, ItemStack[] layout)
    {
        merged.sort(MERGED_BY_ITEM_ID);
        List<ItemStack> stacks = new ArrayList<>();
        for (MergedStack entry : merged)
            stacks.addAll(split(container, entry));

        if (stacks.size() > layout.length)
            return false;
        for (int i = 0; i < stacks.size(); i++)
            layout[i] = stacks.get(i);
        return true;
    }

    /**
     * Gives every recognised material a column, in value order. A mineral fills its
     * column from the top with every stack it has, heaviest form first and no gaps; what
     * does not fit spills into the nearest column with room, from the bottom up, so a
     * material with plenty borrows the space a sparse neighbour leaves. Armor keeps a row
     * per piece, and the gap where a piece is missing stays empty. Anything that is not a
     * recognised material goes at the end of the container.
     */
    private static boolean arrangeInColumns(Container container, List<MergedStack> merged,
                                            ItemStack[] layout, boolean[] reserved)
    {
        int rows = layout.length / WIDTH;
        List<MergedStack> unrecognised = new ArrayList<>();
        Map<String, Group> groups = new LinkedHashMap<>();

        for (MergedStack stack : merged)
        {
            ItemFamilies.Family family = ItemFamilies.classify(stack.prototype);
            if (family == null)
                unrecognised.add(stack);
            else
                groups.computeIfAbsent(family.kind().ordinal() + ":" + family.material(), key -> new Group(family))
                        .add(family.tier(), split(container, stack));
        }

        List<Column> columns = new ArrayList<>();
        for (Group group : groups.values())
            columns.addAll(group.toColumns());
        // Stable, so several sets of one armor material keep their order side by side
        columns.sort(Comparator.comparing(c -> c.family, ItemFamilies.COLUMN_ORDER));

        List<ItemStack> atTheEnd = new ArrayList<>();
        List<List<ItemStack>> overflow = new ArrayList<>();

        for (int c = 0; c < columns.size(); c++)
        {
            Column column = columns.get(c);
            if (c >= WIDTH)
            {
                // More materials than the grid is wide: they go at the end, still grouped
                atTheEnd.addAll(column.stacks());
                continue;
            }

            List<ItemStack> spilled = new ArrayList<>();
            overflow.add(spilled);
            if (column.byRow != null)
            {
                for (int row = 0; row < column.byRow.length; row++)
                {
                    int slot = row * WIDTH + c;
                    if (column.byRow[row] != null)
                        layout[slot] = column.byRow[row];
                    else
                        reserved[slot] = true; // the missing piece keeps its row
                }
            }
            else
            {
                List<ItemStack> stacks = column.inOrder;
                for (int i = 0; i < stacks.size(); i++)
                {
                    if (i < rows)
                        layout[i * WIDTH + c] = stacks.get(i);
                    else
                        spilled.add(stacks.get(i));
                }
            }
        }

        // Only once every column has its own stacks, so a spill never takes a slot the
        // neighbour's own material needed
        for (int c = 0; c < overflow.size(); c++)
        {
            for (ItemStack stack : overflow.get(c))
            {
                int slot = nearestFreeBelow(layout, reserved, rows, c);
                if (slot < 0)
                    atTheEnd.add(stack);
                else
                    layout[slot] = stack;
            }
        }

        unrecognised.sort(MERGED_BY_ITEM_ID);
        for (MergedStack stack : unrecognised)
            atTheEnd.addAll(split(container, stack));

        return fillFromTheEnd(layout, reserved, atTheEnd);
    }

    /**
     * The closest other column with a free slot, checking one step right then one step
     * left, then two, and so on; within it, the lowest free slot, so the spill builds up
     * from the bottom away from whatever the column already holds on top.
     *
     * @return the slot, or -1 if the grid is full
     */
    private static int nearestFreeBelow(ItemStack[] layout, boolean[] reserved, int rows, int column)
    {
        for (int distance = 1; distance < WIDTH; distance++)
        {
            for (int candidate : new int[]{column + distance, column - distance})
            {
                if (candidate < 0 || candidate >= WIDTH)
                    continue;
                for (int row = rows - 1; row >= 0; row--)
                {
                    int slot = row * WIDTH + candidate;
                    if (layout[slot] == null && !reserved[slot])
                        return slot;
                }
            }
        }
        return -1;
    }

    /**
     * Places the stacks in the last free slots of the container, in reading order, so
     * they sit below and after the columns rather than in among them. Armor gaps are
     * only given up when there is nowhere else, since keeping them is never worth
     * destroying items over.
     *
     * @return false if even the gaps are not enough, meaning the sort must be abandoned
     */
    private static boolean fillFromTheEnd(ItemStack[] layout, boolean[] reserved, List<ItemStack> stacks)
    {
        List<Integer> free = new ArrayList<>();
        List<Integer> gaps = new ArrayList<>();
        for (int slot = 0; slot < layout.length; slot++)
        {
            if (layout[slot] != null)
                continue;
            (reserved[slot] ? gaps : free).add(slot);
        }

        List<Integer> targets;
        if (stacks.size() <= free.size())
            targets = free.subList(free.size() - stacks.size(), free.size());
        else
        {
            targets = new ArrayList<>(free);
            targets.addAll(gaps);
            if (stacks.size() > targets.size())
                return false;
        }

        for (int i = 0; i < stacks.size(); i++)
            layout[targets.get(i)] = stacks.get(i);
        return true;
    }

    /** The merged count cut back into stacks the container accepts, full ones first. */
    private static List<ItemStack> split(Container container, MergedStack entry)
    {
        List<ItemStack> stacks = new ArrayList<>();
        int maxSize = Math.min(entry.prototype.getMaxStackSize(), container.getMaxStackSize());
        for (int remaining = entry.count; remaining > 0; remaining -= maxSize)
            stacks.add(entry.prototype.copyWithCount(Math.min(maxSize, remaining)));
        return stacks;
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

    /** Every stack of one material, filed by tier. */
    private static class Group
    {
        final ItemFamilies.Family family;
        final List<List<ItemStack>> byTier = new ArrayList<>();

        Group(ItemFamilies.Family family)
        {
            this.family = family;
            for (int tier = 0; tier < family.kind().tiers; tier++)
                byTier.add(new ArrayList<>());
        }

        void add(int tier, List<ItemStack> stacks)
        {
            byTier.get(tier).addAll(stacks);
        }

        /**
         * A mineral is one column of everything it has. Armor is one column per set:
         * the first of each piece forms the first set, the second of each the next, and
         * so on, so two diamond sets stand side by side.
         */
        List<Column> toColumns()
        {
            List<Column> columns = new ArrayList<>();
            if (family.kind() == ItemFamilies.Kind.ARMOR)
            {
                int sets = 0;
                for (List<ItemStack> pieces : byTier)
                {
                    pieces.sort(BY_ITEM_ID);
                    sets = Math.max(sets, pieces.size());
                }
                for (int set = 0; set < sets; set++)
                {
                    ItemStack[] byRow = new ItemStack[byTier.size()];
                    for (int tier = 0; tier < byTier.size(); tier++)
                        if (set < byTier.get(tier).size())
                            byRow[tier] = byTier.get(tier).get(set);
                    columns.add(new Column(family, byRow, null));
                }
            }
            else
            {
                List<ItemStack> inOrder = new ArrayList<>();
                for (List<ItemStack> tier : byTier)
                {
                    // Same item stays together, full stacks before the partial one
                    tier.sort(BY_ITEM_ID.thenComparing(s -> -s.getCount()));
                    inOrder.addAll(tier);
                }
                columns.add(new Column(family, null, inOrder));
            }
            return columns;
        }
    }

    /**
     * One column of the layout: either a fixed row per piece ({@code byRow}, armor) or
     * a run of stacks laid top down ({@code inOrder}, minerals).
     */
    private record Column(ItemFamilies.Family family, ItemStack[] byRow, List<ItemStack> inOrder)
    {
        List<ItemStack> stacks()
        {
            if (inOrder != null)
                return inOrder;
            List<ItemStack> pieces = new ArrayList<>();
            for (ItemStack piece : byRow)
                if (piece != null)
                    pieces.add(piece);
            return pieces;
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
