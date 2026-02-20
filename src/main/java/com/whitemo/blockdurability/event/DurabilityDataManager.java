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
    // 核心存储：坐标 -> 自定义硬度（-1=不可破坏，null=使用原版硬度）
    private static final Map<BlockPos, Integer> durabilityMap = new HashMap<>();

    // 获取当前世界的实例
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

    // 从NBT加载数据
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

    // 保存数据到NBT
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

    // 设置坐标的耐久值
    public void setDurability(BlockPos pos, int durability) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState == Blocks.AIR.defaultBlockState()) {
            return;
        }

        durabilityMap.put(pos, durability);
        setDirty(); // 标记需要保存

        if (this.level != null) {
            NetworkHandler.syncUpdateToDimension(this.level, pos, durability);
        }
    }

    // 获取坐标的耐久值（null=无自定义）
    public Integer getDurability(BlockPos pos) {
        return durabilityMap.get(pos);
    }

    // 删除坐标的自定义设置
    public void removeDurability(BlockPos pos) {
        BlockState blockState = level.getBlockState(pos);
        if (blockState == Blocks.AIR.defaultBlockState()) {
            return;
        }
        durabilityMap.remove(pos);
        setDirty();

        if (this.level != null) {
            NetworkHandler.syncRemoveToDimension(this.level, pos);
        }
    }

    // 检查坐标是否为不可破坏
    public boolean isUnbreakable(BlockPos pos) {
        Integer durability = getDurability(pos);
        return durability != null && durability == -1;
    }

    public Map<BlockPos, Integer> getAllDurabilityData() {
        return durabilityMap;
    }
}
