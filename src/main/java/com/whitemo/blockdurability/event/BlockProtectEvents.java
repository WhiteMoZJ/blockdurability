package com.whitemo.blockdurability.event;

import com.mojang.logging.LogUtils;
import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.client.ClientDurabilityCache;
import com.whitemo.blockdurability.compat.tacztweaks.TaCZTweaksCompat;
import com.whitemo.blockdurability.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Objects;

@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID)
public class BlockProtectEvents {
    private static final Logger LOGGER = LogUtils.getLogger();

    // block break event
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return; // only server side processing

        ServerLevel level = (ServerLevel) event.getLevel();
        BlockPos pos = event.getPos();
        DurabilityDataManager data = DurabilityDataManager.get(level);
        Player player = event.getPlayer();

        // allow creative mode player to break blocks and remove custom durability
        if (player != null && player.isCreative()) {
            if (data.getDurability(pos) != null) {
                data.removeDurability(pos);
            }
            return;
        }

        Integer durability = data.getDurability(pos);
        if (durability == null) return; // no custom durability, use vanilla behavior

        // unbreakable block break event
        if (durability == -1) {
            event.setCanceled(true);
            return;
        }

        // remove custom durability after block break
        if (!event.isCanceled()) {
            data.removeDurability(pos);
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
        
        // Delay network sync to avoid registration lock
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


    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {

        if (event.getEntity().level().isClientSide()) return;

        Projectile projectile = event.getProjectile();
        HitResult hitResult = event.getRayTraceResult();

        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) return;

        ServerLevel level = (ServerLevel) event.getEntity().level();
        BlockPos pos = BlockPos.containing(hitResult.getLocation());
        DurabilityDataManager data = DurabilityDataManager.get(level);

        Integer durability = data.getDurability(pos);
        if (durability == null) return;

        if (durability == -1) {
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            return;
        }

        if (TaCZTweaksCompat.isInstalled() && TaCZTweaksCompat.isTaCZProjectile(projectile)) {
            if (durability > 0) {
                data.removeDurability(pos);
            }
        }
    }
}
