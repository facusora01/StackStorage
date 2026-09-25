package com.facusora.stackstorage.neoforge.gametest;

import com.facusora.stackstorage.StackStorage;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;
import java.util.function.Consumer;

/**
 * Since 1.21.5 a game test is a registered function plus a test instance naming its
 * structure. Both are registered here, and only in a dev run with game tests on, so
 * the shipped jar adds nothing to anyone's registries.
 */
public final class NeoForgeGameTests
{
    private record Test(String name, String structure, Consumer<GameTestHelper> body)
    {
    }

    private static final List<Test> TESTS = List.of(
            new Test("open_space_matches_straight_line", "open_room", PathDistanceGameTests::openSpaceMatchesStraightLine),
            new Test("wall_puts_container_out_of_reach", "open_room", PathDistanceGameTests::wallPutsContainerOutOfReach),
            new Test("closed_door_blocks_open_door_passes", "open_room", PathDistanceGameTests::closedDoorBlocksOpenDoorPasses),
            new Test("diagonal_cannot_cut_a_corner", "open_room", PathDistanceGameTests::diagonalCannotCutACorner),
            new Test("vertical_builds_columns", "empty", SortGameTests::verticalBuildsColumns),
            new Test("vertical_orders_single_tier_minerals", "empty", SortGameTests::verticalOrdersSingleTierMinerals),
            new Test("vertical_uses_the_mineral_value_order", "empty", SortGameTests::verticalUsesTheMineralValueOrder),
            new Test("vertical_spills_full_columns_into_neighbours", "empty", SortGameTests::verticalSpillsFullColumnsIntoNeighbours),
            new Test("vertical_gives_each_armor_set_its_own_column", "empty", SortGameTests::verticalGivesEachArmorSetItsOwnColumn),
            new Test("vertical_falls_back_on_short_container", "empty", SortGameTests::verticalFallsBackOnShortContainer),
            new Test("vertical_never_loses_items", "empty", SortGameTests::verticalNeverLosesItems),
            new Test("alphabetical_merges_stacks", "empty", SortGameTests::alphabeticalMergesStacks));

    private static final int MAX_TICKS = 100;

    private NeoForgeGameTests()
    {
    }

    public static void register(IEventBus modBus)
    {
        if (!GameTestHooks.isGametestEnabled())
            return;
        modBus.addListener(NeoForgeGameTests::registerFunctions);
        modBus.addListener(NeoForgeGameTests::registerTests);
    }

    private static void registerFunctions(RegisterEvent event)
    {
        event.register(Registries.TEST_FUNCTION, helper -> TESTS.forEach(test -> helper.register(id(test.name()), test.body())));
    }

    private static void registerTests(RegisterGameTestsEvent event)
    {
        Holder<TestEnvironmentDefinition> environment = event.registerEnvironment(id("default"));
        for (Test test : TESTS)
        {
            event.registerTest(id(test.name()), new FunctionGameTestInstance(
                    ResourceKey.create(Registries.TEST_FUNCTION, id(test.name())),
                    // Sky access: without it the test area gets a barrier ceiling, and the
                    // wall test needs the long way round over the top to exist
                    new TestData<>(environment, id(test.structure()), MAX_TICKS, 0, true,
                            Rotation.NONE, false, 1, 1, true)));
        }
    }

    private static ResourceLocation id(String path)
    {
        return ResourceLocation.fromNamespaceAndPath(StackStorage.MODID, path);
    }
}
