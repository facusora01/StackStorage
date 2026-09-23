package com.facusora.stackstorage.quickstack;

/**
 * How the sort button arranges a container: left click is alphabetical, right click
 * is vertical. The client picks it per click and sends it with the sort request.
 */
public enum SortMode
{
    /** Merge partial stacks and lay everything out by item id, filling row by row. */
    ALPHABETICAL,
    /**
     * Give each material its own column, in value order, filled from the top with every
     * stack it has, heaviest form first: iron blocks, then ingots, nuggets, raw ore, raw
     * blocks. A column that runs out of room spills into the nearest neighbour with
     * space. Armor sets get a row per piece instead, helmet to boots, and a missing
     * piece leaves its row empty so the gap shows what is missing.
     */
    VERTICAL;

    private static final SortMode[] VALUES = values();

    public static SortMode byId(int id)
    {
        return id >= 0 && id < VALUES.length ? VALUES[id] : ALPHABETICAL;
    }
}
