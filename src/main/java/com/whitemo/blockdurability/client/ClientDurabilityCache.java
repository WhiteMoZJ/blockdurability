package com.whitemo.blockdurability.client;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientDurabilityCache {
    private static final Map<BlockPos, Integer> CLIENT_DURABILITY_MAP = new HashMap<>();

    // 全量更新数据
    public static void updateFullData(Map<BlockPos, Integer> data) {
        if (data == null) return;
        CLIENT_DURABILITY_MAP.clear();
        CLIENT_DURABILITY_MAP.putAll(data);
    }

    // 更新单个方块
    public static void updateBlock(BlockPos pos, int durability) {
        CLIENT_DURABILITY_MAP.put(pos, durability);
    }

    // 删除单个方块
    public static void removeBlock(BlockPos pos) {
        CLIENT_DURABILITY_MAP.remove(pos);
    }

    // 获取所有缓存数据
    public static Map<BlockPos, Integer> getAllData() {
        return Collections.unmodifiableMap(CLIENT_DURABILITY_MAP);
    }

    // 清空缓存（切换世界时调用）
    public static void clear() {
        CLIENT_DURABILITY_MAP.clear();
    }
}
