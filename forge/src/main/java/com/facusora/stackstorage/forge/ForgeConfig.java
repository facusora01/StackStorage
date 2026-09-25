package com.facusora.stackstorage.forge;

import com.facusora.stackstorage.Config;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ForgeConfig
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue QUICK_STACK_RADIUS = BUILDER
            .comment(Config.QUICK_STACK_RADIUS_COMMENT)
            .defineInRange(Config.QUICK_STACK_RADIUS_KEY, Config.QUICK_STACK_RADIUS_DEFAULT,
                    Config.QUICK_STACK_RADIUS_MIN, Config.QUICK_STACK_RADIUS_MAX);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    static void register(BusGroup modBusGroup)
    {
        ModConfigEvent.Loading.getBus(modBusGroup).addListener(ForgeConfig::onLoad);
        ModConfigEvent.Reloading.getBus(modBusGroup).addListener(ForgeConfig::onLoad);
    }

    private static void onLoad(ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == SPEC)
            Config.quickStackRadius = QUICK_STACK_RADIUS.get();
    }
}
