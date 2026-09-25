package com.facusora.stackstorage.forge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.forge.client.ForgeClientEvents;
import com.facusora.stackstorage.platform.Services;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.Objects;

@Mod(StackStorage.MODID)
public class StackStorageForge
{
    public StackStorageForge(FMLJavaModLoadingContext context)
    {
        // Resolve the platform now, so a packaging mistake fails at startup and not on the first click
        Objects.requireNonNull(Services.PLATFORM);
        BusGroup modBusGroup = context.getModBusGroup();

        context.registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC);
        ForgeConfig.register(modBusGroup);
        ForgeNetworking.register();

        if (FMLEnvironment.dist == Dist.CLIENT)
            ForgeClientEvents.register(modBusGroup);
    }
}
