package me.jetabyte.heimdall.LODDistrubution;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.ChunkPos;

public class SendLODToClient {
    public static void SendLodToClient(ServerPlayerEntity player, ChunkPos pos, byte[] meshData, int scale) {
        PacketHandler payload = new PacketHandler(pos, meshData, scale);
        ServerPlayNetworking.send(player, payload);
    }
}