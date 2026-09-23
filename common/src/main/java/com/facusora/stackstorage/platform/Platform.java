package com.facusora.stackstorage.platform;

import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * What the shared code needs from the mod loader. Each loader ships one
 * implementation, found through {@link Services}.
 */
public interface Platform
{
    /** The loader name as Modrinth tags it, for the update check. */
    String loaderName();

    /** The installed version of this mod, as the loader read it from the jar. */
    String modVersion();

    void sendToServer(QuickStackPacket packet);

    void sendToServer(SortContainerPacket packet);

    /** Sends to every player within {@code radius} blocks of {@code center}. */
    void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, QuickStackAnimationPacket packet);
}
