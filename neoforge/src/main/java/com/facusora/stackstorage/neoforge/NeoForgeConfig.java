package com.facusora.stackstorage.neoforge;

import com.facusora.stackstorage.Config;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

public class NeoForgeConfig
{
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.IntValue QUICK_STACK_RADIUS = BUILDER
            .comment(Config.QUICK_STACK_RADIUS_COMMENT)
            .defineInRange(Config.QUICK_STACK_RADIUS_KEY, Config.QUICK_STACK_RADIUS_DEFAULT,
                    Config.QUICK_STACK_RADIUS_MIN, Config.QUICK_STACK_RADIUS_MAX);

    static final ModConfigSpec SPEC = BUILDER.build();

    static void onLoad(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() == SPEC)
            Config.quickStackRadius = QUICK_STACK_RADIUS.get();
    }
}
