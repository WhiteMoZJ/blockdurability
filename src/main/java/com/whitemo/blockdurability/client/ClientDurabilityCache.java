package com.whitemo.blockdurability.client;

import net.minecraft.core.BlockPos;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ClientDurabilityCache {
    private static final Map<BlockPos, Integer> CLIENT_DURABILITY_MAP = new HashMap<>();

    // global update
    public static void updateFullData(Map<BlockPos, Integer> data) {
        if (data == null) return;
        CLIENT_DURABILITY_MAP.clear();
        CLIENT_DURABILITY_MAP.putAll(data);
    }

    // Update single block durability
    public static void updateBlock(BlockPos pos, int durability) {
        CLIENT_DURABILITY_MAP.put(pos, durability);
    }

    // Remove single block from cache
    public static void removeBlock(BlockPos pos) {
        CLIENT_DURABILITY_MAP.remove(pos);
    }

    // Get all cached durability data
    public static Map<BlockPos, Integer> getAllData() {
        return Collections.unmodifiableMap(CLIENT_DURABILITY_MAP);
    }

    // Clear cache when switching worlds
    public static void clear() {
        CLIENT_DURABILITY_MAP.clear();
    }
}
