package com.facusora.stackstorage.client;

import com.facusora.stackstorage.quickstack.SortMode;

/**
 * Client-side choice of sort layout, cycled with a right click on the sort button and
 * sent along with every sort request. Session-scoped, like {@link SlotLocks}.
 */
public class SortPreferences
{
    private static SortMode mode = SortMode.ALPHABETICAL;

    public static SortMode current()
    {
        return mode;
    }

    /** Moves to the next layout and returns it. */
    public static SortMode cycle()
    {
        mode = mode.next();
        return mode;
    }
}
