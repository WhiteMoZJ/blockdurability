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
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = BlockDurabilityMod.MOD_ID, value = Dist.CLIENT)
public class ClientBlockEvents {

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        if (!event.getEntity().level().isClientSide()) return;

        BlockPos pos = event.getPosition().get();
        Integer customDurability = ClientDurabilityCache.getAllData().get(pos);

        if (customDurability == null) return;

        if (customDurability == -1) {
            event.setNewSpeed(0);
            return;
        }
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
            return;
        }
    }
}
