package me.jetabyte.heimdall.LODRegistory;

import me.jetabyte.heimdall.LODGeneration.LOD;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

import java.io.*;
import java.nio.file.Path;

public class LODLoading {

    public static LOD loadFromFile(MinecraftServer server, ChunkPos pos, int level) {
        // 1. Point to the same world-specific folder we saved to
        Path worldPath = server.getSavePath(WorldSavePath.ROOT);
        File file = worldPath.resolve("heimdall/level"+ 1 +"/" + level + "/" + pos.x + "." + pos.z + ".hmdl").toFile();

        if (!file.exists()) return null;

        LOD lod = new LOD(pos);

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            int count = in.readInt();

            for (int i = 0; i < count; i++) {
                int packedPos = in.readInt();

                int x = (packedPos >> 14) & 15;
                int z = (packedPos >> 10) & 15;
                int y = (packedPos & 1023) - 64;

                int rawId = in.readInt();
                BlockState state = Block.getStateFromRawId(rawId);

                BlockPos worldPos = new BlockPos(pos.getStartX() + x, y, pos.getStartZ() + z);

                lod.addBlock(worldPos, state);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return lod;
    }
}