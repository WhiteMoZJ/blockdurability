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

    // block break event
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return; // 仅服务端处理

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        DurabilityDataManager data = DurabilityDataManager.get(level);
        Player player = event.getPlayer();

        // available player creative mode block break event
        if (player != null && player.isCreative()) {
            if (data.getDurability(pos) != null) {
                data.removeDurability(pos);
            }
            return;
        }

        // unbreakable block break event
        if (data.isUnbreakable(pos)) {
            event.setCanceled(true);
            return;
        }

        // optional: remove custom durability after block break
        Integer durability = data.getDurability(pos);
        if (durability != null && !event.isCanceled()) {
            data.removeDurability(pos);
        }
    }

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

        // remove unbreakable block from explosion affect list
        event.getAffectedBlocks().removeIf(pos -> data.isUnbreakable(pos));
    }

    @SubscribeEvent
    public static void onPistonPush(BlockEvent event) {
        if (event.getLevel().isClientSide()) return;

        ServerLevel level = (ServerLevel) event.getLevel();
        DurabilityDataManager data = DurabilityDataManager.get(level);
        BlockPos targetPos = event.getPos();

        // cancel piston push if target block is unbreakable
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
