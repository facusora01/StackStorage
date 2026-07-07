package com.facusora.stackstorage;

import com.facusora.stackstorage.network.ModNetworking;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(StackStorageMod.MODID)
public class StackStorageMod
{
    public static final String MODID = "stackstorage";

    public StackStorageMod(FMLJavaModLoadingContext context)
    {
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        ModNetworking.register();
    }
}
