package com.whitemo.blockdurability.init;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.whitemo.blockdurability.config.ModClientConfig;
import com.whitemo.blockdurability.compat.worldedit.WorldEditCompat;
import com.whitemo.blockdurability.event.DurabilityDataManager;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;

import com.mojang.brigadier.arguments.BoolArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod.EventBusSubscriber
public class ModCommandsRegistry {
    // command root: /bd
    private static final String ROOT_CMD = "blockdurability";
    private static final String ALIAS_CMD = "bd";

    private static final Logger Logger = LoggerFactory.getLogger(ModCommandsRegistry.class);

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // main command
        var rootCommand = Commands.literal(ROOT_CMD)
                .requires(source -> source.hasPermission(2)) // OP level 2 required
                // set durability subcommand: /bd set <x> <y> <z> <durability>
                .then(Commands.literal("set")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("durability", IntegerArgumentType.integer(-1))
                                        .executes(context -> {
                                            BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                            int durability = IntegerArgumentType.getInteger(context, "durability");
                                            ServerLevel level = context.getSource().getLevel();
                                            DurabilityDataManager data = DurabilityDataManager.get(level);

                                            if (data.setDurability(pos, durability)) {
                                                String key = durability == -1
                                                        ? "command.blockdurability.set.unbreakable"
                                                        : "command.blockdurability.set.success";
                                                context.getSource().sendSuccess(() -> Component.translatable(
                                                    key, pos.getX(), pos.getY(), pos.getZ(), durability
                                                ), true);
                                                } else {
                                                    context.getSource().sendFailure(Component.translatable(
                                                            "command.blockdurability.set.failed", pos.getX(), pos.getY(), pos.getZ()
                                                    ));
                                                }
                                            return 1;
                                        })
                                )
                        )
                )
                // remove custom durability setting subcommand: /bd remove <x> <y> <z>
                .then(Commands.literal("remove")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                    ServerLevel level = context.getSource().getLevel();
                                    DurabilityDataManager data = DurabilityDataManager.get(level);

                                    if (data.removeDurability(pos)) {
                                        context.getSource().sendSuccess(() -> Component.translatable(
                                                "command.blockdurability.remove.success", pos.getX(), pos.getY(), pos.getZ()
                                        ), true);
                                    } else {
                                        context.getSource().sendFailure(Component.translatable(
                                                "command.blockdurability.remove.failed", pos.getX(), pos.getY(), pos.getZ()
                                        ));
                                    }
                                    return 1;
                                })
                        )
                )
                // get custom durability setting subcommand: /bd get <x> <y> <z>
                .then(Commands.literal("get")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(context -> {
                                    BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
                                    ServerLevel level = context.getSource().getLevel();
                                    DurabilityDataManager data = DurabilityDataManager.get(level);
                                    Integer durability = data.getDurability(pos);

                                    String key = durability == null
                                            ? "command.blockdurability.get.none"
                                            : durability == -1
                                            ? "command.blockdurability.get.unbreakable"
                                            : "command.blockdurability.get.custom";
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            key, pos.getX(), pos.getY(), pos.getZ(), durability
                                    ), false);
                                    return 1;
                                })
                        )
                )
                // set custom durability setting for a diagonal area subcommand: /bd set <pos1> <pos2> <durability>
                .then(Commands.literal("set")
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                        .then(Commands.argument("durability", IntegerArgumentType.integer(-1))
                                                .executes(context -> {
                                                    BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
                                                    BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");
                                                    int durability = IntegerArgumentType.getInteger(context, "durability");
                                                    ServerLevel level = context.getSource().getLevel();
                                                    DurabilityDataManager data = DurabilityDataManager.get(level);
                                                    int minX = Math.min(pos1.getX(), pos2.getX());
                                                    int maxX = Math.max(pos1.getX(), pos2.getX());
                                                    int minY = Math.min(pos1.getY(), pos2.getY());
                                                    int maxY = Math.max(pos1.getY(), pos2.getY());
                                                    int minZ = Math.min(pos1.getZ(), pos2.getZ());
                                                    int maxZ = Math.max(pos1.getZ(), pos2.getZ());

                                                    // scan all blocks in the area
                                                    final int[] count = {0};
                                                    for (int x = minX; x <= maxX; x++) {
                                                        for (int y = minY; y <= maxY; y++) {
                                                            for (int z = minZ; z <= maxZ; z++) {
                                                                BlockPos currentPos = new BlockPos(x, y, z);
                                                                if (data.setDurability(currentPos, durability)) {
                                                                    count[0]++;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    // send success message
                                                    String key = durability == -1
                                                            ? "command.blockdurability.set.area.unbreakable"
                                                            : "command.blockdurability.set.area.custom";
                                                    context.getSource().sendSuccess(() -> Component.translatable(
                                                            key, pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), count[0], durability
                                                    ), true);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                )
                // remove custom durability setting for a diagonal area subcommand: /bd diagonal remove <pos1> <pos2>
                .then(Commands.literal("remove")
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                            .executes(context -> {
                                                BlockPos pos1 = BlockPosArgument.getLoadedBlockPos(context, "pos1");
                                                BlockPos pos2 = BlockPosArgument.getLoadedBlockPos(context, "pos2");
                                                ServerLevel level = context.getSource().getLevel();
                                                DurabilityDataManager data = DurabilityDataManager.get(level);
                                                int minX = Math.min(pos1.getX(), pos2.getX());
                                                int maxX = Math.max(pos1.getX(), pos2.getX());
                                                int minY = Math.min(pos1.getY(), pos2.getY());
                                                int maxY = Math.max(pos1.getY(), pos2.getY());
                                                int minZ = Math.min(pos1.getZ(), pos2.getZ());
                                                int maxZ = Math.max(pos1.getZ(), pos2.getZ());

                                                // scan all blocks in the area
                                                final int[] count = {0};
                                                for (int x = minX; x <= maxX; x++) {
                                                    for (int y = minY; y <= maxY; y++) {
                                                        for (int z = minZ; z <= maxZ; z++) {
                                                            BlockPos currentPos = new BlockPos(x, y, z);
                                                            if (data.removeDurability(currentPos)) {
                                                                count[0]++;
                                                            }
                                                        }
                                                    }
                                                }
                                                // send success message
                                                context.getSource().sendSuccess(() -> Component.translatable(
                                                        "command.blockdurability.remove.area.success", pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ(), count[0]
                                                ), true);
                                                return 1;
                                            })
                                    )
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
                                            String key = targetState ? "command.blockdurability.visual.toggle.on" : "command.blockdurability.visual.toggle.off";
                                            context.getSource().sendSuccess(() -> Component.translatable(key), true);
                                            return 1;
                                        })
                                )
                        )
                        // set visualization render range subcommand: /bd visual range <range>
                        .then(Commands.literal("range")
                                .then(Commands.argument("range", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> {
                                            int range = IntegerArgumentType.getInteger(context, "range");
                                            ModClientConfig.RENDER_RANGE.set(range);
                                            ModClientConfig.SPEC.save();
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    "command.blockdurability.visual.range", range
                                            ), true);
                                            return 1;
                                        })
                                )
                        )
                        // toggle visualization display only with stick subcommand: /bd visual stick
                        .then(Commands.literal("stick")
                                .then(Commands.argument("state", BoolArgumentType.bool())
                                        .executes(context -> {
                                            boolean targetState = BoolArgumentType.getBool(context, "state");
                                            ModClientConfig.ONLY_SHOW_WITH_STICK.set(targetState);
                                            ModClientConfig.SPEC.save();
                                            String key = targetState ? "command.blockdurability.visual.stick.on" : "command.blockdurability.visual.stick.off";
                                            context.getSource().sendSuccess(() -> Component.translatable(key), true);
                                            return 1;
                                        })
                                )
                        )
                );

        // registry commands for world edit
        WorldEditCompat.init();
        if (WorldEditCompat.isInstalled()) {
            rootCommand.then(Commands.literal("we")
                    .then(Commands.literal("set")
                            .then(Commands.argument("durability", IntegerArgumentType.integer(-1))
                                    .executes(context -> {
                                        ServerPlayer player = context.getSource().getPlayer();
                                        BlockPos[] pos = WorldEditCompat.getRegionCorners(player);

                                        if (pos == null || pos.length < 2) {
                                            String key = "command.blockdurability.we.failed";
                                            context.getSource().sendFailure(Component.translatable(key));
                                        } else {
                                            int durability = IntegerArgumentType.getInteger(context, "durability");
                                            ServerLevel level = context.getSource().getLevel();
                                            DurabilityDataManager data = DurabilityDataManager.get(level);
                                            final int[] count = {0};
                                            for (BlockPos currentPos : pos) {
                                                if (data.setDurability(currentPos, durability)) {
                                                    count[0]++;
                                                }
                                            }
                                            String key = durability == -1
                                                    ? "command.blockdurability.we.set.area.unbreakable"
                                                    : "command.blockdurability.we.set.area.custom";
                                            context.getSource().sendSuccess(() -> Component.translatable(
                                                    key, count[0], durability
                                            ), true);
                                        }
                                        return 1;
                                    })
                            )
                    )
                    .then(Commands.literal("remove")
                            .executes(context -> {
                                ServerPlayer player = context.getSource().getPlayer();
                                BlockPos[] pos = WorldEditCompat.getRegionCorners(player);

                                if (pos == null || pos.length < 2) {
                                    String key = "command.blockdurability.we.failed";
                                    context.getSource().sendFailure(Component.translatable(key));
                                } else {
                                    ServerLevel level = context.getSource().getLevel();
                                    DurabilityDataManager data = DurabilityDataManager.get(level);
                                    final int[] count = {0};
                                    for (BlockPos currentPos : pos) {
                                        if (data.removeDurability(currentPos)) {
                                            count[0]++;
                                        }
                                    }
                                    context.getSource().sendSuccess(() -> Component.translatable(
                                            "command.blockdurability.we.remove.area.success", count[0]
                                    ), true);
                                }
                                return 1;
                            })
                    )
            );
            Logger.info("Register WorldEdit subcommand: /{} we", ALIAS_CMD);
        }

        var rootCommandBuilt = rootCommand.build();
        event.getDispatcher().register(Commands.literal(ALIAS_CMD).redirect(rootCommandBuilt));
        Logger.info("Register command: /{}", ALIAS_CMD);
    }
}
