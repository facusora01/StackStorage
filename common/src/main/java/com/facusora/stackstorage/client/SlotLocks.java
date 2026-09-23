package com.facusora.stackstorage.client;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-side set of locked item types. Locked items are skipped by quick stack
 * and marked with a star wherever they sit in the player's inventory, so the
 * marker follows the item when it is moved between slots. Session-scoped.
 */
public class SlotLocks
{
    private static final Set<Item> LOCKED = new HashSet<>();

    public static boolean isLocked(ItemStack stack)
    {
        return !stack.isEmpty() && LOCKED.contains(stack.getItem());
    }

    public static void toggle(ItemStack stack)
    {
        if (stack.isEmpty())
            return;
        if (!LOCKED.remove(stack.getItem()))
            LOCKED.add(stack.getItem());
    }

    /** Snapshot of the locked item types, sent to the server with the quick stack request. */
    public static Set<Item> snapshot()
    {
        return new HashSet<>(LOCKED);
    }
}
