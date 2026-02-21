package com.whitemo.blockdurability.network;

import com.whitemo.blockdurability.BlockDurabilityMod;
import com.whitemo.blockdurability.client.ClientDurabilityCache;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
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
        // full sync packet (Sent upon entering the world/switching dimensions)
        CHANNEL.messageBuilder(FullSyncPacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FullSyncPacket::encode)
                .decoder(FullSyncPacket::new)
                .consumerMainThread((packet, contextSupplier) -> FullSyncPacket.handle(packet, contextSupplier.get()))
                .add();

        // update packet (sent when setting block durability)
        CHANNEL.messageBuilder(UpdatePacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UpdatePacket::encode)
                .decoder(UpdatePacket::new)
                .consumerMainThread((packet, contextSupplier) -> UpdatePacket.handle(packet, contextSupplier.get()))
                .add();

        // remove packet (sent when removing block settings)
        CHANNEL.messageBuilder(RemovePacket.class, PACKET_ID++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RemovePacket::encode)
                .decoder(RemovePacket::new)
                .consumerMainThread((packet, contextSupplier) -> RemovePacket.handle(packet, contextSupplier.get()))
                .add();    
    }

    // full sync  for specified players
    public static void syncFullDataToPlayer(ServerPlayer player, Map<BlockPos, Integer> data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new FullSyncPacket(data));
    }

    // update for all player
    public static void syncUpdateToDimension(ServerLevel level, BlockPos pos, int durability) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new UpdatePacket(pos, durability));
    }

    // sync remove for all player
    public static void syncRemoveToDimension(ServerLevel level, BlockPos pos) {
        CHANNEL.send(PacketDistributor.DIMENSION.with(level::dimension), new RemovePacket(pos));
    }

    // custom full sync packet
    public record FullSyncPacket(Map<BlockPos, Integer> durabilityMap) {
        public void encode(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeInt(durabilityMap.size());
            for (Map.Entry<BlockPos, Integer> entry : durabilityMap.entrySet()) {
                buf.writeBlockPos(entry.getKey());
                buf.writeInt(entry.getValue());
            }
        }

        public FullSyncPacket(net.minecraft.network.FriendlyByteBuf buf) {
            this(decodeDurabilityMap(buf));
        }

        private static Map<BlockPos, Integer> decodeDurabilityMap(net.minecraft.network.FriendlyByteBuf buf) {
            int size = buf.readInt();
            Map<BlockPos, Integer> map = new java.util.HashMap<>();
            for (int i = 0; i < size; i++) {
                BlockPos pos = buf.readBlockPos();
                int durability = buf.readInt();
                map.put(pos, durability);
            }
            return map;
        }

        public static boolean handle(FullSyncPacket packet, net.minecraftforge.network.NetworkEvent.Context context) {
            context.enqueueWork(() -> ClientDurabilityCache.updateFullData(packet.durabilityMap));
            context.setPacketHandled(true);
            return true;
        }
    }

    // custom update packet
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
            context.setPacketHandled(true);
            return true;
        }
    }

    // custom remove packet
    public record RemovePacket(BlockPos pos) {
        public void encode(net.minecraft.network.FriendlyByteBuf buf) {
            buf.writeBlockPos(pos);
        }

        public RemovePacket(net.minecraft.network.FriendlyByteBuf buf) {
            this(buf.readBlockPos());
        }

        public static boolean handle(RemovePacket packet, net.minecraftforge.network.NetworkEvent.Context context) {
            context.enqueueWork(() -> ClientDurabilityCache.removeBlock(packet.pos));
            context.setPacketHandled(true);
            return true;
        }
    }
}
