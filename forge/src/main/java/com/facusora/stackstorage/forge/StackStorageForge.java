package com.facusora.stackstorage.forge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.platform.Services;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Objects;

@Mod(StackStorage.MODID)
public class StackStorageForge
{
    // No-arg constructor and ModLoadingContext.get() on purpose: NeoForge 1.20.1 loads
    // this jar too, and it only knows the injected-context constructors Forge 47.4 added
    // as FMLModContainer, IEventBus or ModContainer, never FMLJavaModLoadingContext.
    @SuppressWarnings("removal")
    public StackStorageForge()
    {
        // Resolve the platform now, so a packaging mistake fails at startup and not on the first click
        Objects.requireNonNull(Services.PLATFORM);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ForgeConfig.SPEC);
        ForgeNetworking.register();
    }
}
