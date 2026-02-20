package com.whitemo.blockdurability.event;

import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class DurabilityDataManager extends SavedData {
    private static final String DATA_NAME = BlockDurabilityMod.MOD_ID + "_block_data";
    // core data: pos -> durability (-1=unbreakable, null=use default hardness)
    private static final Map<BlockPos, Integer> durabilityMap = new HashMap<>();

    // get instance of current world
    private ServerLevel level;

    public static DurabilityDataManager get(ServerLevel level) {
        DurabilityDataManager data = level.getDataStorage().computeIfAbsent(
                DurabilityDataManager::load,
                DurabilityDataManager::new,
                DATA_NAME
        );

        data.level = level;
        return data;
    }

    // load data from NBT
    public static DurabilityDataManager load(CompoundTag tag) {
        DurabilityDataManager data = new DurabilityDataManager();
        ListTag list = tag.getList("BlockDurabilityList", CompoundTag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            int durability = entry.getInt("Durability");
            durabilityMap.put(pos, durability);
        }
        return data;
    }

    // save data to NBT
    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : durabilityMap.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("Pos", entry.getKey().asLong());
            entryTag.putInt("Durability", entry.getValue());
            list.add(entryTag);
        }
        tag.put("BlockDurabilityList", list);
        return tag;
    }

    // set durability of block at pos
    public boolean setDurability(BlockPos pos, int durability) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState == Blocks.AIR.defaultBlockState()) {
            return false;
        }

        durabilityMap.put(pos, durability);
        setDirty();

        if (this.level != null) {
            NetworkHandler.syncUpdateToDimension(this.level, pos, durability);
        }
        return true;
    }

    // get durability of block at pos (null=use default hardness)
    public Integer getDurability(BlockPos pos) {
        return durabilityMap.get(pos);
    }

    // remove custom durability setting of block at pos
    public boolean removeDurability(BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState == Blocks.AIR.defaultBlockState()) {
            return false;
        }
        durabilityMap.remove(pos);
        setDirty();

        if (this.level != null) {
            NetworkHandler.syncRemoveToDimension(this.level, pos);
        }
        return true;
    }

    // check if block at pos is unbreakable
    public boolean isUnbreakable(BlockPos pos) {
        Integer durability = getDurability(pos);
        return durability != null && durability == -1;
    }

    public Map<BlockPos, Integer> getAllDurabilityData() {
        return durabilityMap;
    }
}
