package com.facusora.stackstorage.client;

/**
 * Version ordering for the update notice. Kept free of Minecraft and loader types,
 * since the loaders each bring a different version class.
 */
public final class VersionCompare
{
    private VersionCompare()
    {
    }

    /**
     * Numeric compare of "a.b.c" cores; a stable release beats the same-numbered
     * prerelease (1.3.0 is newer than 1.3.0-beta). Non-numeric noise is ignored.
     */
    public static boolean isNewer(String remote, String local)
    {
        int[] r = parseCore(remote);
        int[] l = parseCore(local);
        for (int i = 0; i < 3; i++)
        {
            if (r[i] != l[i])
                return r[i] > l[i];
        }
        return local.contains("-") && !remote.contains("-");
    }

    // Everything from the first - or + on is outside the core: a version number
    // carrying the game version as build metadata ("1.2.0+1.21") would otherwise
    // parse as 1.2.1 and tell every client to update forever.
    private static int[] parseCore(String version)
    {
        String[] parts = version.split("[-+]")[0].split("\\.");
        int[] out = new int[3];
        for (int i = 0; i < 3 && i < parts.length; i++)
        {
            String digits = parts[i].replaceAll("\\D", "");
            if (!digits.isEmpty())
            {
                try
                {
                    out[i] = Integer.parseInt(digits);
                }
                catch (NumberFormatException ignored)
                {
                }
            }
        }
        return out;
    }
}
