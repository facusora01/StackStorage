package com.facusora.stackstorage.forge;

import com.facusora.stackstorage.Config;
import com.facusora.stackstorage.StackStorage;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = StackStorage.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue QUICK_STACK_RADIUS = BUILDER
            .comment(Config.QUICK_STACK_RADIUS_COMMENT)
            .defineInRange(Config.QUICK_STACK_RADIUS_KEY, Config.QUICK_STACK_RADIUS_DEFAULT,
                    Config.QUICK_STACK_RADIUS_MIN, Config.QUICK_STACK_RADIUS_MAX);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        Config.quickStackRadius = QUICK_STACK_RADIUS.get();
    }
}
