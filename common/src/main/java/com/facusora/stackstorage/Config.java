package com.facusora.stackstorage;

/**
 * Current config values, as plain fields the shared code can read. Each loader owns
 * the actual config file and copies its values in here whenever it (re)loads.
 */
public final class Config
{
    public static final String QUICK_STACK_RADIUS_KEY = "quickStackRadius";
    public static final String[] QUICK_STACK_RADIUS_COMMENT = {
            "How far (in blocks) quick stack reaches for chests/barrels/shulker boxes, measured along the way",
            "round through open space: walls, floors and closed doors have to be gone around, not through.",
            "Capped at 16 because the search visits every open block within this distance."
    };
    public static final int QUICK_STACK_RADIUS_DEFAULT = 5;
    public static final int QUICK_STACK_RADIUS_MIN = 1;
    public static final int QUICK_STACK_RADIUS_MAX = 16;

    public static int quickStackRadius = QUICK_STACK_RADIUS_DEFAULT;

    private Config()
    {
    }
}
