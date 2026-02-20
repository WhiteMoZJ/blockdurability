package com.whitemo.blockdurability.event;

import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.client.ClientDurabilityCache;
import com.whitemo.blockdurability.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

import java.util.Objects;

@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID)
public class BlockProtectEvents {

    // 拦截方块破坏事件（核心：阻止不可破坏方块被玩家挖掉）
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return; // 仅服务端处理

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        DurabilityDataManager data = DurabilityDataManager.get(level);
        Player player = event.getPlayer();

        // 核心修改：创造模式玩家直接放行，不拦截
        if (player != null && player.isCreative()) {
            // 可选：创造模式破坏后自动删除自定义设置（和原版逻辑一致）
            if (data.getDurability(pos) != null) {
                data.removeDurability(pos);
            }
            return; // 创造模式不拦截
        }

        // 不可破坏方块直接取消破坏
        if (data.isUnbreakable(pos)) {
            event.setCanceled(true);
            return;
        }

        // 可选：方块被成功破坏后删除自定义设置（不需要可注释掉）
        Integer durability = data.getDurability(pos);
        if (durability != null && !event.isCanceled()) {
            data.removeDurability(pos);
        }
    }

    // 2. 修改挖掘速度（自定义耐久生效，和原版硬度逻辑一致）
    @SubscribeEvent
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (event.getEntity().level().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getEntity().level();
        BlockPos pos = event.getPosition().get();
        DurabilityDataManager data = DurabilityDataManager.get(level);
        Integer customDurability = data.getDurability(pos);

        if (customDurability == null) return;

        if (customDurability == -1) {
            event.setNewSpeed(0);
            return;
        }

        float originalHardness = event.getState().getDestroySpeed(level, pos);
        if (originalHardness > 0) {
            float newSpeed = event.getOriginalSpeed() * (originalHardness / customDurability);
            event.setNewSpeed(newSpeed);
        }
    }

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        DurabilityDataManager data = DurabilityDataManager.get(level);

        // 从爆炸影响列表中移除所有不可破坏的方块
        event.getAffectedBlocks().removeIf(pos -> data.isUnbreakable(pos));
    }

    @SubscribeEvent
    public static void onPistonPush(BlockEvent event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        DurabilityDataManager data = DurabilityDataManager.get(level);
        BlockPos targetPos = event.getPos();

        // 活塞要推动的方块是不可破坏的，直接取消活塞动作
        if (data.isUnbreakable(targetPos)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();
        Objects.requireNonNull(player.getServer()).execute(() -> {
            DurabilityDataManager data = DurabilityDataManager.get(level);
            NetworkHandler.syncFullDataToPlayer(player, data.getAllDurabilityData());
        });
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        ServerLevel level = player.serverLevel();

        Objects.requireNonNull(player.getServer()).execute(() -> {
            DurabilityDataManager data = DurabilityDataManager.get(level);
            NetworkHandler.syncFullDataToPlayer(player, data.getAllDurabilityData());
        });
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientDurabilityCache::clear);
    }
}
