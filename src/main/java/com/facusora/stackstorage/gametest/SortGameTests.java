package com.facusora.stackstorage.gametest;

import com.facusora.stackstorage.StackStorageMod;
import com.facusora.stackstorage.quickstack.ContainerSortHandler;
import com.facusora.stackstorage.quickstack.SortMode;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Covers the container layouts. These run against a bare container rather than a
 * placed chest, but they need a live server so Forge's material tags are bound.
 */
@GameTestHolder(StackStorageMod.MODID)
@PrefixGameTestTemplate(false)
public class SortGameTests
{
    private static final int DOUBLE_CHEST = 54;
    private static final int SINGLE_CHEST = 27;
    private static final int WIDTH = 9;

    /** A double chest gets one column per material, one row per tier, gaps and all. */
    @GameTest(template = "empty")
    public static void verticalBuildsColumns(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        container.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        container.setItem(5, new ItemStack(Items.RAW_IRON, 12));
        container.setItem(9, new ItemStack(Items.IRON_NUGGET, 5));
        container.setItem(13, new ItemStack(Items.IRON_BLOCK, 3));
        container.setItem(20, new ItemStack(Items.IRON_INGOT, 30));
        container.setItem(22, new ItemStack(Items.RAW_IRON_BLOCK, 2));
        container.setItem(30, new ItemStack(Items.DIAMOND_HELMET));
        container.setItem(31, new ItemStack(Items.DIAMOND_CHESTPLATE));
        container.setItem(32, new ItemStack(Items.DIAMOND_BOOTS)); // leggings left out on purpose
        container.setItem(40, new ItemStack(Items.DIAMOND_BLOCK, 4));
        container.setItem(41, new ItemStack(Items.DIAMOND, 9));

        ContainerSortHandler.sortContainer(container, SortMode.VERTICAL);

        // Column 0: armor comes first, helmet down to boots
        assertAt(helper, container, 0, 0, Items.DIAMOND_HELMET);
        assertAt(helper, container, 1, 0, Items.DIAMOND_CHESTPLATE);
        assertEmptyAt(helper, container, 2, 0); // the missing leggings keep their row
        assertAt(helper, container, 3, 0, Items.DIAMOND_BOOTS);

        // Column 1: diamond is the most valuable mineral present
        assertAt(helper, container, 0, 1, Items.DIAMOND_BLOCK);
        assertAt(helper, container, 1, 1, Items.DIAMOND);
        assertEmptyAt(helper, container, 2, 1);

        // Column 2: the full iron ladder, heaviest form on top
        assertAt(helper, container, 0, 2, Items.IRON_BLOCK);
        assertAt(helper, container, 1, 2, Items.IRON_INGOT);
        assertAt(helper, container, 2, 2, Items.IRON_NUGGET);
        assertAt(helper, container, 3, 2, Items.RAW_IRON_BLOCK);
        assertAt(helper, container, 4, 2, Items.RAW_IRON);

        // Cobblestone belongs to no family, so it starts right after the columns
        assertAt(helper, container, 0, 3, Items.COBBLESTONE);

        helper.succeed();
    }

    /** Three rows cannot hold a full armor set, so those containers stay alphabetical. */
    @GameTest(template = "empty")
    public static void verticalFallsBackOnShortContainer(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(SINGLE_CHEST);
        container.setItem(5, new ItemStack(Items.IRON_BLOCK, 1));
        container.setItem(20, new ItemStack(Items.DIAMOND, 1));
        container.setItem(1, new ItemStack(Items.COBBLESTONE, 1));

        ContainerSortHandler.sortContainer(container, SortMode.VERTICAL);

        // Packed from the first slot by item id, with no column gaps
        assertAt(helper, container, 0, 0, Items.COBBLESTONE);
        assertAt(helper, container, 0, 1, Items.DIAMOND);
        assertAt(helper, container, 0, 2, Items.IRON_BLOCK);
        helper.assertTrue(container.getItem(3).isEmpty(), "expected no gaps in the alphabetical fallback");

        helper.succeed();
    }

    /**
     * When the leftovers outgrow the free slots, the reserved gaps get used rather than
     * items getting dropped. A nearly full chest with one armor column forces that path.
     */
    @GameTest(template = "empty")
    public static void verticalNeverLosesItems(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        for (int slot = 0; slot < 52; slot++)
            container.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        container.setItem(52, new ItemStack(Items.IRON_BLOCK, 1));
        container.setItem(53, new ItemStack(Items.DIAMOND_HELMET));

        Map<Item, Integer> before = tally(container);
        ContainerSortHandler.sortContainer(container, SortMode.VERTICAL);
        Map<Item, Integer> after = tally(container);

        helper.assertTrue(before.equals(after),
                "item counts changed: " + before + " -> " + after);
        // The sort really ran rather than bailing out
        assertAt(helper, container, 0, 0, Items.DIAMOND_HELMET);

        helper.succeed();
    }

    /** Alphabetical mode still merges partial stacks and packs them from the top. */
    @GameTest(template = "empty")
    public static void alphabeticalMergesStacks(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        container.setItem(3, new ItemStack(Items.IRON_INGOT, 20));
        container.setItem(17, new ItemStack(Items.IRON_INGOT, 30));
        container.setItem(44, new ItemStack(Items.DIAMOND, 2));

        ContainerSortHandler.sortContainer(container, SortMode.ALPHABETICAL);

        assertAt(helper, container, 0, 0, Items.DIAMOND);
        assertAt(helper, container, 0, 1, Items.IRON_INGOT);
        helper.assertTrue(container.getItem(1).getCount() == 50,
                "expected the two ingot stacks merged into 50, got " + container.getItem(1).getCount());

        helper.succeed();
    }

    private static void assertAt(GameTestHelper helper, SimpleContainer container, int row, int column, Item expected)
    {
        int slot = row * WIDTH + column;
        Item actual = container.getItem(slot).getItem();
        helper.assertTrue(actual == expected,
                "row " + row + " column " + column + ": expected " + expected + " but found " + actual);
    }

    private static void assertEmptyAt(GameTestHelper helper, SimpleContainer container, int row, int column)
    {
        int slot = row * WIDTH + column;
        helper.assertTrue(container.getItem(slot).isEmpty(),
                "row " + row + " column " + column + " should have stayed empty, found "
                        + container.getItem(slot).getItem());
    }

    private static Map<Item, Integer> tally(SimpleContainer container)
    {
        Map<Item, Integer> counts = new HashMap<>();
        for (int slot = 0; slot < container.getContainerSize(); slot++)
        {
            ItemStack stack = container.getItem(slot);
            if (!stack.isEmpty())
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }
        return counts;
    }
}
