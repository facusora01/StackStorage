package com.facusora.stackstorage;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = StackStorageMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.IntValue QUICK_STACK_RADIUS = BUILDER
            .comment("Radius (in blocks) around the player to search for chests/barrels/shulker boxes when quick stacking")
            .defineInRange("quickStackRadius", 8, 1, 64);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static int quickStackRadius;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        quickStackRadius = QUICK_STACK_RADIUS.get();
    }
}
