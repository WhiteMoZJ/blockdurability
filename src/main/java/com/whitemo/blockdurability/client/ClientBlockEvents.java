package com.whitemo.blockdurability.client;

import com.mojang.logging.LogUtils;
import com.whitemo.blockdurability.BlockDurabilityMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

import java.lang.reflect.Field;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID, value = Dist.CLIENT)
public class ClientBlockEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static Field destroySpeedField;

    static {
        try {
            // Try to find destroy speed field in Player class
            destroySpeedField = Player.class.getDeclaredField("destroySpeed");
            destroySpeedField.setAccessible(true);
            // LOGGER.info("[客户端调试] 在 Player 类中找到 destroySpeed 字段");
        } catch (NoSuchFieldException e) {
            // LOGGER.error("无法在 Player 类中找到 destroySpeed 字段", e);
            // List all fields in Player class for debugging
            // LOGGER.info("[客户端调试] Player 类所有字段:");
            for (Field field : Player.class.getDeclaredFields()) {
                // LOGGER.info("  - {}", field.getName());
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().level().isClientSide()) return;

        BlockPos pos = event.getPosition().get();
        Integer customDurability = ClientDurabilityCache.getAllData().get(pos);

        if (customDurability == null) return;

        // LOGGER.info("[客户端调试] BreakSpeed事件触发 - 位置: {}, 自定义耐久: {}", pos, customDurability);

        if (customDurability == -1) {
            event.setNewSpeed(0);
            // LOGGER.info("[客户端调试] 设置为不可破坏");
            return;
        }

        float blockDestroySpeed = event.getState().getDestroySpeed(event.getEntity().level(), pos);
        // LOGGER.info("[客户端调试] 方块破坏速度: {}", blockDestroySpeed);

        if (blockDestroySpeed <= 0) {
            // LOGGER.info("[客户端调试] 方块破坏速度为0，跳过");
            return;
        }

        float vanillaBreakTime = 30.0f / blockDestroySpeed;
        // LOGGER.info("[客户端调试] 原版破坏时间: {} 秒", vanillaBreakTime);

        float newSpeed = 30.0f / customDurability;

        // LOGGER.info("[客户端调试] 新破坏速度: {}", newSpeed);

        event.setNewSpeed(newSpeed);
        // LOGGER.info("[客户端调试] 已设置新破坏速度");
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;

        // Get the block player is currently breaking
        if (mc.hitResult == null || !mc.hitResult.getType().equals(net.minecraft.world.phys.HitResult.Type.BLOCK)) {
            return;
        }

        BlockPos pos = ((net.minecraft.world.phys.BlockHitResult) mc.hitResult).getBlockPos();
        Integer customDurability = ClientDurabilityCache.getAllData().get(pos);
        
        if (customDurability == null) {
            // No custom durability at this position
            return;
        }
        
        if (customDurability == -1) {
            // LOGGER.info("[客户端调试] Tick: 不可破坏方块");
            return;
        }

        BlockState state = mc.level.getBlockState(pos);
        float blockDestroySpeed = state.getDestroySpeed(mc.level, pos);
        
        if (blockDestroySpeed <= 0) {
            // LOGGER.info("[客户端调试] Tick: 方块破坏速度为0");
            return;
        }

        float newSpeed = 30.0f / customDurability;
        // LOGGER.info("[客户端调试] Tick: 尝试更新破坏速度为 {}", newSpeed);
        
        // LOGGER.info("[客户端调试] destroySpeedField 是否为 null: {}", destroySpeedField == null);
        
        // Update player's destroy speed using reflection
        if (destroySpeedField != null) {
            try {
                float currentSpeed = destroySpeedField.getFloat(player);
                if (Math.abs(currentSpeed - newSpeed) > 0.01f) {
                    destroySpeedField.setFloat(player, newSpeed);
                    // LOGGER.info("[客户端调试] 更新破坏速度: {} -> {}", currentSpeed, newSpeed);
                } else {
                    // LOGGER.info("[客户端调试] 破坏速度已是: {}", currentSpeed);
                }
            } catch (IllegalAccessException e) {
                // LOGGER.error("无法设置 destroySpeed", e);
            }
        } else {
            // LOGGER.error("[客户端调试] destroySpeedField 为 null，无法更新破坏速度");
        }
    }
}
