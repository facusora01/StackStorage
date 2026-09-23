package com.facusora.stackstorage.forge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import com.facusora.stackstorage.platform.Platform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.PacketDistributor;

public class ForgePlatform implements Platform
{
    @Override
    public String loaderName()
    {
        // NeoForge 1.20.1 loads this same jar, which Modrinth lists under "forge"
        return "forge";
    }

    @Override
    public String modVersion()
    {
        return ModList.get().getModContainerById(StackStorage.MODID)
                .orElseThrow().getModInfo().getVersion().toString();
    }

    @Override
    public void sendToServer(QuickStackPacket packet)
    {
        ForgeNetworking.CHANNEL.sendToServer(packet);
    }

    @Override
    public void sendToServer(SortContainerPacket packet)
    {
        ForgeNetworking.CHANNEL.sendToServer(packet);
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, QuickStackAnimationPacket packet)
    {
        ForgeNetworking.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, radius, level.dimension())),
                packet);
    }
}
