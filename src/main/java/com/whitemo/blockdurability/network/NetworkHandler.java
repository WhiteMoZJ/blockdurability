package com.whitemo.blockdurability.network;

import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.client.ClientDurabilityCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.Map;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(BlockDurabilityMod.MOD_ID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int PACKET_ID = 0;

    public static void register() {
        // 全量同步包（进入世界/切换维度时发送）
        CHANNEL.messageBuilder(FullSyncPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FullSyncPacket::encode)
                .decoder(FullSyncPacket::new)
                .consumerMainThread((packet, context) -> FullSyncPacket.handle(packet, (NetworkEvent.Context) context))
                .add();

        // 增量更新包（设置方块耐久时发送）
        CHANNEL.messageBuilder(UpdatePacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UpdatePacket::encode)
                .decoder(UpdatePacket::new)
                .consumerMainThread((packet, context) -> UpdatePacket.handle(packet, (NetworkEvent.Context) context))
                .add();

        // 删除包（移除方块设置时发送）
        CHANNEL.messageBuilder(RemovePacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RemovePacket::encode)
                .decoder(RemovePacket::new)
                .consumerMainThread((packet, context) -> RemovePacket.handle(packet, (NetworkEvent.Context) context))
                .add();
    }

    // 全量同步给指定玩家
    public static void syncFullDataToPlayer(ServerPlayer player, Map<BlockPos, Integer> data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FullSyncPacket(data));
    }

    // 增量同步给维度内所有玩家
    public static void syncUpdateToDimension(ServerLevel level, BlockPos pos, int durability) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new UpdatePacket(pos, durability));
    }

    // 同步删除给维度内所有玩家
    public static void syncRemoveToDimension(ServerLevel level, BlockPos pos) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new RemovePacket(pos));
    }

    // 全量同步包定义
    public record FullSyncPacket(Map<BlockPos, Integer> durabilityMap) {
        public void encode(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeInt(durabilityMap.size());
            for (Map.Entry<BlockPos, Integer> entry : durabilityMap.entrySet()) {
                buf.writeBlockPos(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }

        public FullSyncPacket(net.minecraft.network.FriendlyByteBuf buf) {
            this(buf.readMap(net.minecraft.network.FriendlyByteBuf::readBlockPos, net.minecraft.network.FriendlyByteBuf::readInt));
        }

        public static boolean handle(FullSyncPacket packet, net.minecraftforge.network.NetworkEvent.Context context) {
            context.enqueueWork(() -> ClientDurabilityCache.updateFullData(packet.durabilityMap));
            return true;
        }
    }

    // 增量更新包定义
    public record UpdatePacket(BlockPos pos, int durability) {
        public void encode(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
            buf.writeInt(durability);
        }

        public UpdatePacket(net.minecraft.network.FriendlyByteBuf buf) {
            this(buf.readBlockPos(), buf.readInt());
        }

        public static boolean handle(UpdatePacket packet, net.minecraftforge.network.NetworkEvent.Context context) {
            context.enqueueWork(() -> ClientDurabilityCache.updateBlock(packet.pos, packet.durability));
            return true;
        }
    }

    // 删除包定义
    public record RemovePacket(BlockPos pos) {
        public void encode(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public RemovePacket(net.minecraft.network.FriendlyByteBuf buf) {
            this(buf.readBlockPos());
        }

        public static boolean handle(RemovePacket packet, net.minecraftforge.network.NetworkEvent.Context context) {
            context.enqueueWork(() -> ClientDurabilityCache.removeBlock(packet.pos));
            return true;
        }
    }
}
