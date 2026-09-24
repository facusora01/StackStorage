package com.facusora.stackstorage.neoforge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.network.QuickStackAnimationPacket;
import com.facusora.stackstorage.network.QuickStackPacket;
import com.facusora.stackstorage.network.SortContainerPacket;
import com.facusora.stackstorage.platform.Platform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.network.PacketDistributor;

public class NeoForgePlatform implements Platform
{
    @Override
    public String loaderName()
    {
        return "neoforge";
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
        PacketDistributor.SERVER.noArg().send(new NeoForgeNetworking.QuickStack(packet));
    }

    @Override
    public void sendToServer(SortContainerPacket packet)
    {
        PacketDistributor.SERVER.noArg().send(new NeoForgeNetworking.Sort(packet));
    }

    @Override
    public void sendToPlayersNear(ServerLevel level, Vec3 center, double radius, QuickStackAnimationPacket packet)
    {
        PacketDistributor.NEAR.with(new PacketDistributor.TargetPoint(
                        center.x, center.y, center.z, radius, level.dimension()))
                .send(new NeoForgeNetworking.Animation(packet));
    }
}
