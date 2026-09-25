package com.facusora.stackstorage.neoforge.gametest;

import com.facusora.stackstorage.quickstack.ContainerSortHandler;
import com.facusora.stackstorage.quickstack.SortMode;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashMap;
import java.util.Map;

/**
 * Covers the container layouts. These run against a bare container rather than a
 * placed chest, but they need a live server so the material tags are bound.
 */
public class SortGameTests
{
    private static final int DOUBLE_CHEST = 54;
    private static final int SINGLE_CHEST = 27;
    private static final int WIDTH = 9;

    /** A double chest gets one column per material, one row per tier, gaps and all. */
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
        assertAt(helper, container, 3, 2, Items.RAW_IRON);
        assertAt(helper, container, 4, 2, Items.RAW_IRON_BLOCK);

        // Cobblestone belongs to no family, so it goes to the very end of the chest
        assertAt(helper, container, 5, 8, Items.COBBLESTONE);

        helper.succeed();
    }

    /**
     * A material with a single tier present still gets its own column, in its place in
     * the value order, rather than being dumped in with the unrecognised items.
     */
    public static void verticalOrdersSingleTierMinerals(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        container.setItem(0, new ItemStack(Items.LAPIS_LAZULI, 64));
        container.setItem(1, new ItemStack(Items.IRON_INGOT, 64));
        container.setItem(2, new ItemStack(Items.EMERALD, 64));
        container.setItem(3, new ItemStack(Items.DIAMOND, 64));

        ContainerSortHandler.sortContainer(container, SortMode.VERTICAL);

        // No row is held back for the missing blocks: each sits at the top of its column
        assertAt(helper, container, 0, 0, Items.DIAMOND);
        assertAt(helper, container, 0, 1, Items.EMERALD);
        assertAt(helper, container, 0, 2, Items.IRON_INGOT);
        assertAt(helper, container, 0, 3, Items.LAPIS_LAZULI);
        assertEmptyAt(helper, container, 1, 0);
        assertEmptyAt(helper, container, 1, 3);

        helper.succeed();
    }

    /**
     * Pins the mineral column order, and the items that carry no Forge family tag:
     * charcoal rides under coal, and netherite runs block, ingot, debris, scrap.
     */
    public static void verticalUsesTheMineralValueOrder(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        Item[] scrambled = {
                Items.AMETHYST_SHARD, Items.QUARTZ, Items.COAL, Items.LAPIS_LAZULI, Items.REDSTONE,
                Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT, Items.EMERALD,
                Items.DIAMOND, Items.NETHERITE_INGOT};
        for (int i = 0; i < scrambled.length; i++)
            container.setItem(i, new ItemStack(scrambled[i], 64));
        container.setItem(20, new ItemStack(Items.CHARCOAL, 64));
        container.setItem(30, new ItemStack(Items.NETHERITE_SCRAP, 64));
        container.setItem(31, new ItemStack(Items.NETHERITE_BLOCK, 64));
        container.setItem(32, new ItemStack(Items.ANCIENT_DEBRIS, 64));

        ContainerSortHandler.sortContainer(container, SortMode.VERTICAL);

        Item[] expected = {
                Items.NETHERITE_BLOCK, Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT,
                Items.IRON_INGOT, Items.COPPER_INGOT, Items.REDSTONE, Items.LAPIS_LAZULI,
                Items.COAL};
        for (int column = 0; column < expected.length; column++)
            assertAt(helper, container, 0, column, expected[column]);

        // Netherite reads block, ingot, ancient debris, scrap from the top
        assertAt(helper, container, 1, 0, Items.NETHERITE_INGOT);
        assertAt(helper, container, 2, 0, Items.ANCIENT_DEBRIS);
        assertAt(helper, container, 3, 0, Items.NETHERITE_SCRAP);
        // Charcoal sits directly under coal in the same column
        assertAt(helper, container, 1, 8, Items.CHARCOAL);
        // Only nine columns fit, so amethyst and quartz fall through to the leftovers
        check(helper, tally(container).containsKey(Items.AMETHYST_SHARD), "amethyst went missing");
        check(helper, tally(container).containsKey(Items.QUARTZ), "quartz went missing");

        helper.succeed();
    }

    /**
     * The overfilled chest from in-game testing, checked against the layout it should
     * have produced. Every stack of a material stays in its column with no rows held
     * back, and a full column spills into the nearest neighbour with room, bottom up.
     */
    public static void verticalSpillsFullColumnsIntoNeighbours(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        Object[][] contents = {
                {Items.NETHERITE_BLOCK, 1}, {Items.NETHERITE_INGOT, 64}, {Items.NETHERITE_INGOT, 64},
                {Items.NETHERITE_INGOT, 64}, {Items.ANCIENT_DEBRIS, 1}, {Items.NETHERITE_SCRAP, 64},
                {Items.NETHERITE_SCRAP, 64}, {Items.DIAMOND_BLOCK, 32}, {Items.DIAMOND, 64},
                {Items.EMERALD, 64}, {Items.GOLD_INGOT, 64}, {Items.GOLD_INGOT, 64},
                {Items.GOLD_INGOT, 64}, {Items.GOLD_NUGGET, 64}, {Items.GOLD_NUGGET, 64},
                {Items.RAW_GOLD, 8}, {Items.RAW_GOLD_BLOCK, 1}, {Items.IRON_BLOCK, 64},
                {Items.IRON_INGOT, 64}, {Items.IRON_NUGGET, 64}, {Items.IRON_NUGGET, 64},
                {Items.IRON_NUGGET, 64}, {Items.IRON_NUGGET, 64}, {Items.RAW_IRON, 64},
                {Items.RAW_IRON_BLOCK, 1}, {Items.COPPER_INGOT, 64}, {Items.COPPER_INGOT, 64},
                {Items.COPPER_INGOT, 64}, {Items.RAW_COPPER_BLOCK, 1}, {Items.LAPIS_LAZULI, 64},
                {Items.AMETHYST_SHARD, 64}, {Items.ENDER_EYE, 64}, {Items.ENDER_PEARL, 16}};
        // Scattered, the way it was before sorting
        for (int i = 0; i < contents.length; i++)
            container.setItem((i * 5) % DOUBLE_CHEST, new ItemStack((Item) contents[i][0], (Integer) contents[i][1]));

        Map<Item, Integer> before = tally(container);
        ContainerSortHandler.sortContainer(container, SortMode.VERTICAL);
        check(helper, before.equals(tally(container)), "item counts changed");

        Item[][] expected = {
                {Items.NETHERITE_BLOCK, Items.DIAMOND_BLOCK, Items.EMERALD, Items.GOLD_INGOT,
                        Items.IRON_BLOCK, Items.COPPER_INGOT, Items.LAPIS_LAZULI, Items.AMETHYST_SHARD, null},
                {Items.NETHERITE_INGOT, Items.DIAMOND, null, Items.GOLD_INGOT,
                        Items.IRON_INGOT, Items.COPPER_INGOT, null, null, null},
                {Items.NETHERITE_INGOT, null, null, Items.GOLD_INGOT,
                        Items.IRON_NUGGET, Items.COPPER_INGOT, null, null, null},
                {Items.NETHERITE_INGOT, null, null, Items.GOLD_NUGGET,
                        Items.IRON_NUGGET, Items.RAW_COPPER_BLOCK, null, null, null},
                // Iron is full, so its raw block spills under copper
                {Items.ANCIENT_DEBRIS, null, null, Items.GOLD_NUGGET,
                        Items.IRON_NUGGET, Items.RAW_IRON_BLOCK, null, null, null},
                // The second netherite scrap drops under diamond, the raw gold block under
                // emerald, the raw iron under copper; the loose items take the last slots
                {Items.NETHERITE_SCRAP, Items.NETHERITE_SCRAP, Items.RAW_GOLD_BLOCK, Items.RAW_GOLD,
                        Items.IRON_NUGGET, Items.RAW_IRON, null, Items.ENDER_EYE, Items.ENDER_PEARL}};

        for (int row = 0; row < expected.length; row++)
            for (int column = 0; column < WIDTH; column++)
            {
                if (expected[row][column] == null)
                    assertEmptyAt(helper, container, row, column);
                else
                    assertAt(helper, container, row, column, expected[row][column]);
            }

        helper.succeed();
    }

    /** Two sets of the same armor stand side by side instead of the spare pieces going astray. */
    public static void verticalGivesEachArmorSetItsOwnColumn(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        container.setItem(3, new ItemStack(Items.DIAMOND_HELMET));
        container.setItem(17, new ItemStack(Items.DIAMOND_HELMET));
        container.setItem(40, new ItemStack(Items.DIAMOND_BOOTS));

        ContainerSortHandler.sortContainer(container, SortMode.VERTICAL);

        assertAt(helper, container, 0, 0, Items.DIAMOND_HELMET);
        assertAt(helper, container, 3, 0, Items.DIAMOND_BOOTS);
        assertAt(helper, container, 0, 1, Items.DIAMOND_HELMET);
        assertEmptyAt(helper, container, 3, 1); // the second set has no boots, and keeps that gap

        helper.succeed();
    }

    /** Three rows cannot hold a full armor set, so those containers stay alphabetical. */
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
        check(helper, container.getItem(3).isEmpty(), "expected no gaps in the alphabetical fallback");

        helper.succeed();
    }

    /**
     * When the leftovers outgrow the free slots, the reserved gaps get used rather than
     * items getting dropped. A nearly full chest with one armor column forces that path.
     */
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

        check(helper, before.equals(after),
                "item counts changed: " + before + " -> " + after);
        // The sort really ran rather than bailing out
        assertAt(helper, container, 0, 0, Items.DIAMOND_HELMET);

        helper.succeed();
    }

    /** Alphabetical mode still merges partial stacks and packs them from the top. */
    public static void alphabeticalMergesStacks(GameTestHelper helper)
    {
        SimpleContainer container = new SimpleContainer(DOUBLE_CHEST);
        container.setItem(3, new ItemStack(Items.IRON_INGOT, 20));
        container.setItem(17, new ItemStack(Items.IRON_INGOT, 30));
        container.setItem(44, new ItemStack(Items.DIAMOND, 2));

        ContainerSortHandler.sortContainer(container, SortMode.ALPHABETICAL);

        assertAt(helper, container, 0, 0, Items.DIAMOND);
        assertAt(helper, container, 0, 1, Items.IRON_INGOT);
        check(helper, container.getItem(1).getCount() == 50,
                "expected the two ingot stacks merged into 50, got " + container.getItem(1).getCount());

        helper.succeed();
    }

    private static void assertAt(GameTestHelper helper, SimpleContainer container, int row, int column, Item expected)
    {
        int slot = row * WIDTH + column;
        Item actual = container.getItem(slot).getItem();
        check(helper, actual == expected,
                "row " + row + " column " + column + ": expected " + expected + " but found " + actual);
    }

    private static void assertEmptyAt(GameTestHelper helper, SimpleContainer container, int row, int column)
    {
        int slot = row * WIDTH + column;
        check(helper, container.getItem(slot).isEmpty(),
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

    /** Game test assertions take a Component since 1.21.5. */
    private static void check(GameTestHelper helper, boolean condition, String message)
    {
        helper.assertTrue(condition, Component.literal(message));
    }
}
