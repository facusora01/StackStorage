package com.facusora.stackstorage.network;

import com.facusora.stackstorage.StackStorageMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Optional;

public class ModNetworking
{
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(StackStorageMod.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    public static void register()
    {
        CHANNEL.registerMessage(0, QuickStackPacket.class,
                QuickStackPacket::encode,
                QuickStackPacket::decode,
                QuickStackPacket::handle);
        CHANNEL.registerMessage(1, SortContainerPacket.class,
                SortContainerPacket::encode,
                SortContainerPacket::decode,
                SortContainerPacket::handle);
        CHANNEL.registerMessage(2, QuickStackAnimationPacket.class,
                QuickStackAnimationPacket::encode,
                QuickStackAnimationPacket::decode,
                QuickStackAnimationPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
}
