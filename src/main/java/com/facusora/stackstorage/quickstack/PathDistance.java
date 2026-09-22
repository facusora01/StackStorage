package com.facusora.stackstorage.quickstack;

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Measures how far away containers are through open space rather than in a straight
 * line, so a chest on the other side of a wall or under the floor is as far as the
 * way round to it, not as far as it looks.
 *
 * One bounded Dijkstra flood from the player settles every container at once, which is
 * why this is not A*: A* earns its keep chasing a single target, and here there are many.
 */
public final class PathDistance
{
    private static final int[][] STEPS;
    private static final double[] STEP_COSTS;

    static
    {
        // All 26 neighbours; each step costs its real length (1, root 2 or root 3), so
        // across an open room the result matches the straight line distance
        STEPS = new int[26][];
        STEP_COSTS = new double[26];
        int i = 0;
        for (int dx = -1; dx <= 1; dx++)
            for (int dy = -1; dy <= 1; dy++)
                for (int dz = -1; dz <= 1; dz++)
                {
                    if (dx == 0 && dy == 0 && dz == 0)
                        continue;
                    STEPS[i] = new int[]{dx, dy, dz};
                    STEP_COSTS[i] = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    i++;
                }
    }

    private PathDistance()
    {
    }

    /**
     * @param starts cells the flood begins from at no cost (the player's feet and head)
     * @param goals  container cells; they are destinations, never passed through
     * @return the cost of reaching each goal within {@code maxCost}; unreachable goals are absent
     */
    public static Map<BlockPos, Double> measure(ServerLevel level, Collection<BlockPos> starts,
                                                Set<BlockPos> goals, double maxCost)
    {
        Map<BlockPos, Double> reached = new HashMap<>();
        if (goals.isEmpty())
            return reached;

        LongSet goalCells = new LongOpenHashSet();
        for (BlockPos goal : goals)
            goalCells.add(goal.asLong());

        Terrain terrain = new Terrain(level);
        Long2DoubleOpenHashMap best = new Long2DoubleOpenHashMap();
        best.defaultReturnValue(Double.POSITIVE_INFINITY);
        PriorityQueue<Node> open = new PriorityQueue<>();

        for (BlockPos start : starts)
        {
            best.put(start.asLong(), 0.0);
            open.add(new Node(start.asLong(), 0.0));
        }

        while (!open.isEmpty())
        {
            Node node = open.poll();
            if (node.cost > best.get(node.cell))
                continue; // superseded by a cheaper route found after it was queued

            int x = BlockPos.getX(node.cell);
            int y = BlockPos.getY(node.cell);
            int z = BlockPos.getZ(node.cell);

            for (int i = 0; i < STEPS.length; i++)
            {
                double cost = node.cost + STEP_COSTS[i];
                if (cost > maxCost)
                    continue;

                int[] step = STEPS[i];
                long next = BlockPos.asLong(x + step[0], y + step[1], z + step[2]);
                boolean isGoal = goalCells.contains(next);

                if (cost >= best.get(next))
                    continue;
                if (!isGoal && !terrain.isOpen(next))
                    continue;
                if (!cornersClear(terrain, x, y, z, step))
                    continue;

                best.put(next, cost);
                if (isGoal)
                    reached.put(BlockPos.of(next), cost); // a container is where you stop, not a corridor
                else
                    open.add(new Node(next, cost));
            }
        }
        return reached;
    }

    /**
     * A diagonal step is only allowed when every cell it sweeps past is open too, so the
     * flood cannot squeeze between two blocks that only touch at an edge or corner.
     */
    private static boolean cornersClear(Terrain terrain, int x, int y, int z, int[] step)
    {
        // Each bit picks one moving axis; every proper, non-empty subset of the moving
        // axes is an intermediate cell the step brushes against
        int moving = (step[0] != 0 ? 1 : 0) | (step[1] != 0 ? 2 : 0) | (step[2] != 0 ? 4 : 0);
        for (int subset = (moving - 1) & moving; subset > 0; subset = (subset - 1) & moving)
        {
            long cell = BlockPos.asLong(
                    x + ((subset & 1) != 0 ? step[0] : 0),
                    y + ((subset & 2) != 0 ? step[1] : 0),
                    z + ((subset & 4) != 0 ? step[2] : 0));
            if (!terrain.isOpen(cell))
                return false;
        }
        return true;
    }

    /** Block lookups for one flood: cached, and never loading a chunk. */
    private static final class Terrain
    {
        private final ServerLevel level;
        private final Map<Long, LevelChunk> chunks = new HashMap<>();
        private final Long2BooleanOpenHashMap open = new Long2BooleanOpenHashMap();

        Terrain(ServerLevel level)
        {
            this.level = level;
        }

        boolean isOpen(long cell)
        {
            if (open.containsKey(cell))
                return open.get(cell);

            boolean result = computeOpen(cell);
            open.put(cell, result);
            return result;
        }

        private boolean computeOpen(long cell)
        {
            int y = BlockPos.getY(cell);
            if (level.isOutsideBuildHeight(y))
                return false;

            int x = BlockPos.getX(cell);
            int z = BlockPos.getZ(cell);
            LevelChunk chunk = chunks.computeIfAbsent(ChunkPos.asLong(x >> 4, z >> 4),
                    key -> level.getChunkSource().getChunkNow(x >> 4, z >> 4));
            if (chunk == null)
                return false; // unloaded: treat as solid rather than load it

            BlockPos pos = new BlockPos(x, y, z);
            BlockState state = chunk.getBlockState(pos);
            Block block = state.getBlock();

            // These have a thin collision box even when open, so their shape says nothing
            // useful: what matters is whether they are open right now
            if (block instanceof DoorBlock || block instanceof TrapDoorBlock || block instanceof FenceGateBlock)
                return state.getValue(BlockStateProperties.OPEN);

            // The chunk as the getter keeps any shape lookup from reaching into other chunks
            return state.getCollisionShape(chunk, pos).isEmpty();
        }
    }

    private record Node(long cell, double cost) implements Comparable<Node>
    {
        @Override
        public int compareTo(Node other)
        {
            return Double.compare(cost, other.cost);
        }
    }
}
