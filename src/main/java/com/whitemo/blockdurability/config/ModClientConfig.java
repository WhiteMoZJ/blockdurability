package com.whitemo.blockdurability.config;

import com.whitemo.blockdurability.BlockDurabilityMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // visibility switch
    public static final ForgeConfigSpec.BooleanValue VISUAL_ENABLED;
    // rendering range
    public static final ForgeConfigSpec.IntValue RENDER_RANGE;
    // only show with stick
    public static final ForgeConfigSpec.BooleanValue ONLY_SHOW_WITH_STICK;
    // highlight line width
    public static final ForgeConfigSpec.DoubleValue LINE_WIDTH;
    // see through wall
    public static final ForgeConfigSpec.BooleanValue SEE_THROUGH_WALLS;
    // text font size
    public static final ForgeConfigSpec.DoubleValue TEXT_SCALE;
    // unbreakable color （RGB）
    public static final ForgeConfigSpec.ConfigValue<String> UNBREAKABLE_COLOR;
    // custom durability color（RGB）
    public static final ForgeConfigSpec.ConfigValue<String> CUSTOM_DURABILITY_COLOR;
    // show text
    public static final ForgeConfigSpec.BooleanValue SHOW_DURABILITY_TEXT;

    static {
        BUILDER.push("Block Durability Visual Settings");

        VISUAL_ENABLED = BUILDER.comment("blockdurability.config.visual_enabled")
                .define("visual_enabled", true);
        RENDER_RANGE = BUILDER.comment("blockdurability.config.render_range")
                .defineInRange("render_range", 16, 1, 64);
        ONLY_SHOW_WITH_STICK = BUILDER.comment("blockdurability.config.only_show_with_stick")
                .define("only_show_with_stick", false);
        LINE_WIDTH = BUILDER.comment("blockdurability.config.line_width")
                .defineInRange("line_width", 2.0, 0.5, 10.0);
        SEE_THROUGH_WALLS = BUILDER.comment("blockdurability.config.see_through_walls")
                .define("see_through_walls", true);
        TEXT_SCALE = BUILDER.comment("blockdurability.config.text_scale")
                .defineInRange("text_scale", 0.025, 0.01, 0.1);
        UNBREAKABLE_COLOR = BUILDER.comment("blockdurability.config.unbreakable_color")
                .define("unbreakable_color", "255,0,0");
        CUSTOM_DURABILITY_COLOR = BUILDER.comment("blockdurability.config.custom_durability_color")
                .define("custom_durability_color", "0,100,255");
        SHOW_DURABILITY_TEXT = BUILDER.comment("blockdurability.config.show_durability_text")
                .define("show_durability_text", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int[] getColorArray(ForgeConfigSpec.ConfigValue<String> colorConfig) {
        try {
            return Arrays.stream(colorConfig.get().split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (Exception e) {
            return colorConfig == UNBREAKABLE_COLOR ? new int[]{255, 0, 0} : new int[]{0, 100, 255};
        }
    }
}
