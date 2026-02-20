package com.whitemo.blockdurability;

import com.whitemo.blockdurability.config.ModClientConfig;
import com.whitemo.blockdurability.event.BlockProtectEvents;
import com.whitemo.blockdurability.network.NetworkHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BlockDurabilityMod.MOD_ID)
public class BlockDurabilityMod {
    public static final String MOD_ID = "blockdurability";

    public BlockDurabilityMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        MinecraftForge.EVENT_BUS.register(new BlockProtectEvents());

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);
        NetworkHandler.register();
    }
}