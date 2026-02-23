package com.whitemo.blockdurability;

import com.whitemo.blockdurability.compat.tacztweaks.TaCZTweaksCompat;
import com.whitemo.blockdurability.config.ModClientConfig;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(BlockDurabilityMod.MOD_ID)
public class BlockDurabilityMod {
    public static final String MOD_ID = "blockdurability";

    public BlockDurabilityMod(FMLJavaModLoadingContext context) {
        TaCZTweaksCompat.init();
        MinecraftForge.EVENT_BUS.register(this);
        context.registerConfig(ModConfig.Type.CLIENT, ModClientConfig.SPEC);
    }
}
