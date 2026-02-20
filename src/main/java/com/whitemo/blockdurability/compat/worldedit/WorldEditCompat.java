package com.whitemo.blockdurability.compat.worldedit;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.session.SessionOwner;
import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.event.DurabilityDataManager;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID)
public class WorldEditCompat {
    private static final String ROOT_CMD = "blockdurability";
    private static final String ALIAS_CMD = "bd";

    public static final String MOD_ID = "worldedit";
    public static boolean INSTALLED  = false;

    @SubscribeEvent
    public static void onRegisterCommandsWE(RegisterCommandsEvent event) {
        INSTALLED  = ModList.get().isLoaded(MOD_ID);

        if (!INSTALLED) {
            return;
        }
        var rootCommand = Commands.literal(ROOT_CMD)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("we") // /bd we 主指令
                        .then(Commands.literal("set") // /bd we set <耐久值>
                                .then(Commands.argument("durability", IntegerArgumentType.integer(-1))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            int durability = IntegerArgumentType.getInteger(context, "durability");
                                            ServerLevel level = context.getSource().getLevel();
                                            DurabilityDataManager data = DurabilityDataManager.get(level);
                                            return batchSetDurabilityByWESelection(player, durability, data);
                                        })
                                )
                        )
                        .then(Commands.literal("remove") // /bd we remove
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    ServerLevel level = context.getSource().getLevel();
                                    DurabilityDataManager data = DurabilityDataManager.get(level);
                                    return batchRemoveDurabilityByWESelection(player, data);
                                })
                        )
                )
                .build();

        event.getDispatcher().register(Commands.literal(ALIAS_CMD).redirect(rootCommand));
    }

    // ========== 核心：通过 WorldEdit 选区批量设置耐久 ==========
    private static int batchSetDurabilityByWESelection(ServerPlayer player, int durability, DurabilityDataManager data) {
        // 获取玩家的 WorldEdit 选区
        Region selection = getPlayerWESelection(player);
        if (selection == null) {
            player.sendSystemMessage(Component.literal("[BD] 错误：未检测到你的 WorldEdit 选区！请先用 //wand 选择区域。"));
            return 0;
        }

        // 遍历选区内所有方块（转为 Cuboid 选区，兼容矩形/立方体）
        CuboidRegion cuboid = (CuboidRegion) selection;
        int count = 0;

        for (BlockVector3 vec3 : cuboid) {
            BlockPos pos = new BlockPos(vec3.getBlockX(), vec3.getBlockY(), vec3.getBlockZ());
            data.setDurability(pos, durability); // 批量设置耐久
            count++;
        }

        // 4. 给玩家反馈
        String msg = durability == -1
                ? String.format("[BD] 已批量设置选区内 %d 个方块为不可破坏", count)
                : String.format("[BD] 已批量设置选区内 %d 个方块的耐久为 %d", count, durability);
        player.sendSystemMessage(Component.literal(msg));
        return 1;
    }

    private static int batchRemoveDurabilityByWESelection(ServerPlayer player, DurabilityDataManager data) {
        Region selection = getPlayerWESelection(player);
        if (selection == null) {
            player.sendSystemMessage(Component.literal("[BD] 错误：未检测到你的 WorldEdit 选区！请先用 //wand 选择区域。"));
            return 0;
        }

        // 遍历选区内所有方块（转为 Cuboid 选区，兼容矩形/立方体）
        CuboidRegion cuboid = (CuboidRegion) selection;
        int count = 0;

        for (BlockVector3 vec3 : cuboid) {
            BlockPos pos = new BlockPos(vec3.getBlockX(), vec3.getBlockY(), vec3.getBlockZ());
            data.removeDurability(pos); // 批量设置耐久
            count++;
        }
        return 1;
    }

    // ========== 工具方法：获取玩家的 WorldEdit 选区 ==========
    private static Region getPlayerWESelection(ServerPlayer player) {
        try {
            // 适配 WorldEdit Forge 版 API：获取玩家的 WorldEdit 会话
            LocalSession session = WorldEdit.getInstance().getSessionManager().get((SessionOwner) player);
            // 获取选区（若未选择则抛出 IncompleteRegionException）
            return session.getSelection(session.getSelectionWorld());
        } catch (IncompleteRegionException e) {
            return null; // 选区未完成/为空
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("[BD] 错误：获取 WorldEdit 选区失败！" + e.getMessage()));
            return null;
        }
    }
}
