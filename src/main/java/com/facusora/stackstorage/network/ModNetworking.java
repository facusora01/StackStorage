package com.facusora.stackstorage.network;

import com.facusora.stackstorage.StackStorageMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

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
    }
}
