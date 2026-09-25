package com.facusora.stackstorage.neoforge;

import com.facusora.stackstorage.StackStorage;
import com.facusora.stackstorage.neoforge.gametest.NeoForgeGameTests;
import com.facusora.stackstorage.platform.Services;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

import java.util.Objects;

@Mod(StackStorage.MODID)
public class StackStorageNeoForge
{
    public StackStorageNeoForge(IEventBus modBus, ModContainer container)
    {
        // Resolve the platform now, so a packaging mistake fails at startup and not on the first click
        Objects.requireNonNull(Services.PLATFORM);
        container.registerConfig(ModConfig.Type.COMMON, NeoForgeConfig.SPEC);
        modBus.addListener(NeoForgeConfig::onLoad);
        modBus.addListener(NeoForgeNetworking::register);
        NeoForgeGameTests.register(modBus);
    }
}
