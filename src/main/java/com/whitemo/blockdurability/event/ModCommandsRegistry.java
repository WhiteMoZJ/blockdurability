package com.whitemo.blockdurability.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.config.ModClientConfig;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID)
public class ModCommandsRegistry {
    // 指令根节点：/blockdurability （可缩写为/bd）
    private static final String ROOT_CMD = "blockdurability";
    private static final String ALIAS_CMD = "bd";

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // 主指令注册
        var rootCommand = Commands.literal(ROOT_CMD)
                .requires(source -> source.hasPermission(2)) // OP等级2以上可使用
                // 设置耐久子命令：/bd set <x> <y> <z> <耐久值>
                .then(Commands.literal("set")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("durability", IntegerArgumentType.integer(-1))
                                        .executes(context -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                            int durability = IntegerArgumentType.getInteger(context, "durability");
                                            ServerLevel level = context.getSource().getLevel();
                                            DurabilityDataManager data = DurabilityDataManager.get(level);

                                            data.setDurability(pos, durability);
                                            String msg = durability == -1
                                                    ? "已设置坐标 [%s, %s, %s] 为不可破坏"
                                                    : "已设置坐标 [%s, %s, %s] 的方块耐久为 %s";
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    String.format(msg, pos.getX(), pos.getY(), pos.getZ(), durability)
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                )
                // 删除设置子命令：/bd remove <x> <y> <z>
                .then(Commands.literal("remove")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                    ServerLevel level = context.getSource().getLevel();
                                    DurabilityDataManager data = DurabilityDataManager.get(level);

                                    data.removeDurability(pos);
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            String.format("已删除坐标 [%s, %s, %s] 的自定义耐久设置", pos.getX(), pos.getY(), pos.getZ())
                                    ), true);
                                    return 1;
                                })
                        )
                )
                // 查询设置子命令：/bd get <x> <y> <z>
                .then(Commands.literal("get")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                    ServerLevel level = context.getSource().getLevel();
                                    DurabilityDataManager data = DurabilityDataManager.get(level);
                                    Integer durability = data.getDurability(pos);

                                    String msg = durability == null
                                            ? "坐标 [%s, %s, %s] 无自定义耐久设置，使用原版属性"
                                            : durability == -1
                                            ? "坐标 [%s, %s, %s] 已设置为不可破坏"
                                            : "坐标 [%s, %s, %s] 的自定义耐久为 %s";
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            String.format(msg, pos.getX(), pos.getY(), pos.getZ(), durability)
                                    ), false);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("visual")
                        // 开关可视化
                        .then(Commands.literal("toggle")
                                .then(Commands.argument("state", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean targetState = BoolArgumentType.getBool(context, "state"); // 获取true/false参数
                                            ModClientConfig.VISUAL_ENABLED.set(targetState);
                                            ModClientConfig.SPEC.save();
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    targetState ? "已开启方块保护可视化" : "已关闭方块保护可视化"
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                        // 设置渲染范围
                        .then(Commands.literal("range")
                                .then(Commands.argument("range", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int range = IntegerArgumentType.getInteger(context, "range");
                                            ModClientConfig.RENDER_RANGE.set(range);
                                            ModClientConfig.SPEC.save();
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    String.format("已设置可视化渲染范围为 %s 格", range)
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                        // 切换仅手持木棍显示
                        .then(Commands.literal("stick")
                                .executes(context -> {
                                    boolean newState = !ModClientConfig.ONLY_SHOW_WITH_STICK.get();
                                    ModClientConfig.ONLY_SHOW_WITH_STICK.set(newState);
                                    ModClientConfig.SPEC.save();
                                    context.getSource().sendSuccess(() -> Component.literal(
                                            newState ? "已开启仅手持木棍时显示可视化" : "已关闭仅手持木棍显示限制"
                                    ), true);
                                    return 1;
                                })
                        )
                )
                .build();

        // 注册主指令和别名
        event.getDispatcher().register(rootCommand.createBuilder());
        event.getDispatcher().register(Commands.literal(ALIAS_CMD).redirect(rootCommand));
    }
}
