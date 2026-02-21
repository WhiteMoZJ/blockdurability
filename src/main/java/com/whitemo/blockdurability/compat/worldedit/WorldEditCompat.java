package com.whitemo.blockdurability.compat.worldedit;

import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.forge.ForgeAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger; 
import org.slf4j.LoggerFactory;

public class WorldEditCompat {
    private static boolean INSTALLED;
    private static final Logger Logger = LoggerFactory.getLogger(WorldEditCompat.class);
    
    public static void init() {
        INSTALLED = ModList.get().isLoaded("worldedit");
        if (INSTALLED) {
            Logger.info("WorldEdit found");
        }
    }

    public static boolean isInstalled() {
        return INSTALLED;
    }

    public static BlockPos[] getRegionCorners(ServerPlayer player) {
        Player actor = ForgeAdapter.adaptPlayer(player);
        LocalSession session = WorldEdit.getInstance().getSessionManager().get(actor);
        World selectionWorld = session.getSelectionWorld();
        try {
            if (selectionWorld == null) {
                return new BlockPos[0];
            }
            Region region = session.getSelection(selectionWorld);
            if (region == null) {
                return new BlockPos[0];
            }
            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            return new BlockPos[] {
                new BlockPos(min.getX(), min.getY(), min.getZ()),
                new BlockPos(max.getX(), max.getY(), max.getZ())
            };
        } catch (Exception e) {
            return new BlockPos[0];
        }
    }
}
