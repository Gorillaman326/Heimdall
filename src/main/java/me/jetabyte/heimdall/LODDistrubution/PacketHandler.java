package me.jetabyte.heimdall.LODDistrubution;

import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;

public record PacketHandler(ChunkPos pos, byte[] data, int scale) implements FabricPacket {
    public static final PacketType<PacketHandler> TYPE = PacketType.create(
            new Identifier("heimdall", "lod_sync"),
            PacketHandler::new
    );

    public PacketHandler(PacketByteBuf buf) {
        this(buf.readChunkPos(), buf.readByteArray(), buf.readInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeChunkPos(pos);
        buf.writeByteArray(data);
        buf.writeInt(scale);
    }

    @Override
    public PacketType<PacketHandler> getType() {
        return TYPE;
    }
}