package com.facusora.stackstorage.quickstack;

/**
 * How the sort button arranges a container. The player cycles between modes with
 * a right click; the choice lives on the client and travels with every sort request.
 */
public enum SortMode
{
    /** Merge partial stacks and lay everything out by item id, filling row by row. */
    ALPHABETICAL,
    /**
     * Give each material family its own column, one row per tier: iron block on top,
     * then ingot, nugget, raw block, raw ore. Armor sets work the same way with
     * helmet / chestplate / leggings / boots. Missing pieces leave their row empty so
     * the columns stay aligned and the gap shows what is missing.
     */
    VERTICAL;

    private static final SortMode[] VALUES = values();

    public SortMode next()
    {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static SortMode byId(int id)
    {
        return id >= 0 && id < VALUES.length ? VALUES[id] : ALPHABETICAL;
    }
}
