package com.facusora.stackstorage.gametest;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.quickstack.PathDistance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Covers the open-space distance quick stack uses. The room is cleared to air before
 * each test and is 11 x 5 x 11; walls span its full height, so the only way round them
 * is over the top, which is always longer than the five block radius.
 */
@GameTestHolder(StackStorage.MODID)
@PrefixGameTestTemplate(false)
public class PathDistanceGameTests
{
    private static final int RADIUS = 5;
    private static final int ROOM_HEIGHT = 5;
    private static final int ROOM_SIDE = 11;

    private static final BlockPos START = new BlockPos(1, 1, 5);

    /** Across open floor the distance is the straight line. */
    @GameTest(template = "open_room")
    public static void openSpaceMatchesStraightLine(GameTestHelper helper)
    {
        BlockPos goal = new BlockPos(4, 1, 5);
        Double cost = measure(helper, goal, RADIUS);

        helper.assertTrue(cost != null && Math.abs(cost - 3.0) < 1e-6,
                "expected a clear distance of 3, got " + cost);
        helper.succeed();
    }

    /** A chest three blocks away through a wall is out of reach; the detour is far longer. */
    @GameTest(template = "open_room")
    public static void wallPutsContainerOutOfReach(GameTestHelper helper)
    {
        wallAt(helper, 2);
        BlockPos goal = new BlockPos(4, 1, 5);

        helper.assertTrue(measure(helper, goal, RADIUS) == null,
                "a container behind a wall should not be within " + RADIUS);

        Double detour = measure(helper, goal, 20);
        helper.assertTrue(detour != null && detour > RADIUS,
                "the route over the wall should exist but be longer than " + RADIUS + ", got " + detour);
        helper.succeed();
    }

    /** A doorway in the wall only counts while the door is open. */
    @GameTest(template = "open_room")
    public static void closedDoorBlocksOpenDoorPasses(GameTestHelper helper)
    {
        wallAt(helper, 3);
        BlockPos goal = new BlockPos(5, 1, 5);

        placeDoor(helper, new BlockPos(3, 1, 5), false);
        helper.assertTrue(measure(helper, goal, RADIUS) == null,
                "a closed door should block the way");

        placeDoor(helper, new BlockPos(3, 1, 5), true);
        Double cost = measure(helper, goal, RADIUS);
        helper.assertTrue(cost != null && Math.abs(cost - 4.0) < 1e-6,
                "an open door should let the flood straight through, got " + cost);
        helper.succeed();
    }

    /** Two blocks meeting at an edge cannot be squeezed between diagonally. */
    @GameTest(template = "open_room")
    public static void diagonalCannotCutACorner(GameTestHelper helper)
    {
        BlockPos start = new BlockPos(4, 1, 4);
        BlockPos goal = new BlockPos(5, 1, 5);
        column(helper, 5, 4);
        column(helper, 4, 5);

        Double cost = PathDistance.measure(helper.getLevel(),
                List.of(helper.absolutePos(start)), Set.of(helper.absolutePos(goal)), 10)
                .get(helper.absolutePos(goal));

        helper.assertTrue(cost != null && cost > Math.sqrt(2) + 0.5,
                "the flood slipped through the corner: cost " + cost);
        helper.succeed();
    }

    private static Double measure(GameTestHelper helper, BlockPos goal, double radius)
    {
        BlockPos absoluteGoal = helper.absolutePos(goal);
        Map<BlockPos, Double> reached = PathDistance.measure(helper.getLevel(),
                List.of(helper.absolutePos(START), helper.absolutePos(START.above())),
                Set.of(absoluteGoal), radius);
        return reached.get(absoluteGoal);
    }

    /** A full height stone wall across the room at the given x. */
    private static void wallAt(GameTestHelper helper, int x)
    {
        for (int y = 0; y < ROOM_HEIGHT; y++)
            for (int z = 0; z < ROOM_SIDE; z++)
                helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    }

    private static void column(GameTestHelper helper, int x, int z)
    {
        for (int y = 0; y < ROOM_HEIGHT; y++)
            helper.setBlock(new BlockPos(x, y, z), Blocks.STONE);
    }

    /**
     * Both halves go in without neighbour updates, since placing one half alone would
     * have the door break itself for missing its other half.
     */
    private static void placeDoor(GameTestHelper helper, BlockPos lower, boolean open)
    {
        BlockState door = Blocks.OAK_DOOR.defaultBlockState()
                .setValue(DoorBlock.FACING, Direction.EAST)
                .setValue(DoorBlock.OPEN, open);
        int flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        helper.getLevel().setBlock(helper.absolutePos(lower),
                door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), flags);
        helper.getLevel().setBlock(helper.absolutePos(lower.above()),
                door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), flags);
    }
}
