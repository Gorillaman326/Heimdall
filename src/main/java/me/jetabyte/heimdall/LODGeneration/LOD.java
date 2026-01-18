package me.jetabyte.heimdall.LODGeneration;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

public class LOD {
    private final ChunkPos pos;
    public final Map<BlockPos, BlockState> Skin = new HashMap<>();

    public LOD(ChunkPos pos) { this.pos = pos; }
    public ChunkPos getPos() { return pos; }
    public Map<BlockPos, BlockState> Skin() { return Skin; }

    public void addBlock(BlockPos pos, BlockState state) {
        Skin.put(pos.toImmutable(), state);
    }

    public int getBlockCount() {
        return Skin.size();
    }
    public byte[] getBytes() {
        // 1. Create a temporary buffer to hold the data 📥
        PacketByteBuf buf = PacketByteBufs.create();

        // 2. Write the size of the map so the client knows how many blocks to read 📏
        buf.writeInt(Skin.size());

        // 3. Loop through the map and write each entry
        Skin.forEach((pos, state) -> {
            buf.writeBlockPos(pos);
            // Note: For 1.20.4, we usually send the raw ID or use a registry codec
            buf.writeVarInt(Block.getRawIdFromState(state));
        });

        // 4. Return the internal byte array
        return buf.array();
    }

    public static LOD fromBytes(ChunkPos pos, byte[] data) {
        LOD lod = new LOD(pos);

        // 1. Wrap the byte array into a Netty ByteBuf, then into a PacketByteBuf
        PacketByteBuf buf = new PacketByteBuf(Unpooled.wrappedBuffer(data));

        try {
            // 2. Read the block count
            int blockCount = buf.readInt();

            // 3. Loop and reconstruct the map
            for (int i = 0; i < blockCount; i++) {
                BlockPos blockPos = buf.readBlockPos();
                int stateId = buf.readVarInt();

                BlockState state = Block.STATE_IDS.get(stateId);
                if (state != null) {
                    lod.addBlock(blockPos, state);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            buf.release();
        }

        return lod;
    }
}
