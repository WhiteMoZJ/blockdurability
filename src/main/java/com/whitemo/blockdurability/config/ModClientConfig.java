package com.whitemo.blockdurability.config;

import com.whitemo.blockdurability.BlockDurabilityMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;

@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModClientConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 可视化总开关
    public static final ForgeConfigSpec.BooleanValue VISUAL_ENABLED;
    // 渲染范围（格）
    public static final ForgeConfigSpec.IntValue RENDER_RANGE;
    // 仅手持木棍时显示
    public static final ForgeConfigSpec.BooleanValue ONLY_SHOW_WITH_STICK;
    // 高亮线宽
    public static final ForgeConfigSpec.DoubleValue LINE_WIDTH;
    // 穿墙显示
    public static final ForgeConfigSpec.BooleanValue SEE_THROUGH_WALLS;
    // 文本大小
    public static final ForgeConfigSpec.DoubleValue TEXT_SCALE;
    // 不可破坏方块颜色（RGB）
    public static final ForgeConfigSpec.ConfigValue<String> UNBREAKABLE_COLOR;
    // 自定义耐久方块颜色（RGB）
    public static final ForgeConfigSpec.ConfigValue<String> CUSTOM_DURABILITY_COLOR;
    // 显示耐久文本
    public static final ForgeConfigSpec.BooleanValue SHOW_DURABILITY_TEXT;

    static {
        BUILDER.push("Block Durability Visual Settings");

        VISUAL_ENABLED = BUILDER.comment("可视化功能默认开关")
                .define("visual_enabled", true);
        RENDER_RANGE = BUILDER.comment("可视化渲染范围（单位：格，最大64）")
                .defineInRange("render_range", 16, 1, 64);
        ONLY_SHOW_WITH_STICK = BUILDER.comment("仅手持木棍时显示可视化效果")
                .define("only_show_with_stick", false);
        LINE_WIDTH = BUILDER.comment("高亮边框线宽")
                .defineInRange("line_width", 2.0, 0.5, 10.0);
        SEE_THROUGH_WALLS = BUILDER.comment("高亮是否穿墙可见")
                .define("see_through_walls", true);
        TEXT_SCALE = BUILDER.comment("耐久文本缩放大小")
                .defineInRange("text_scale", 0.025, 0.01, 0.1);
        UNBREAKABLE_COLOR = BUILDER.comment("不可破坏方块高亮颜色（RGB）")
                .define("unbreakable_color", "255,0,0");
        CUSTOM_DURABILITY_COLOR = BUILDER.comment("自定义耐久方块高亮颜色（RGB）")
                .define("custom_durability_color", "0,100,255");
        SHOW_DURABILITY_TEXT = BUILDER.comment("是否在方块上方显示耐久文本")
                .define("show_durability_text", true);

        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static int[] getColorArray(ForgeConfigSpec.ConfigValue<String> colorConfig) {
        try {
            // 将 "r,g,b" 字符串解析为 int 数组
            return Arrays.stream(colorConfig.get().split(","))
                    .map(String::trim)
                    .mapToInt(Integer::parseInt)
                    .toArray();
        } catch (Exception e) {
            // 解析失败时返回默认值
            return colorConfig == UNBREAKABLE_COLOR ? new int[]{255, 0, 0} : new int[]{0, 100, 255};
        }
    }
}
