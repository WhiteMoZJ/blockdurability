package com.whitemo.blockdurability.compat.tacztweaks;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaCZTweaksCompat {
    private static boolean INSTALLED;
    private static final Logger Logger = LoggerFactory.getLogger(TaCZTweaksCompat.class);

    public static void init() {
        INSTALLED = ModList.get().isLoaded("tacztweaks");
        if (INSTALLED) {
            Logger.info("TaCZ Tweaks found");
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static boolean isTaCZProjectile(Entity entity) {
        if (!INSTALLED) return false;
        if (entity == null) return false;

        String entityName = entity.getType().getDescriptionId();
        return entityName.contains("tacz");
    }
}
