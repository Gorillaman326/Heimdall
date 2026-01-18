package me.jetabyte.heimdall.LODRegistory;

import me.jetabyte.heimdall.LODGeneration.LOD;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

public class LODSaving {
    public static void saveToFile(MinecraftServer server, int LevelFolder ,int LODLevel, LOD lod) {
        Path worldPath = server.getSavePath(WorldSavePath.ROOT);

        File lodDir = worldPath.resolve("heimdall/level"+ +LevelFolder+"/" + LODLevel).toFile();

        if (!lodDir.exists()) {
            lodDir.mkdirs();
        }

        File file = new File(lodDir, lod.getPos().x + "." + lod.getPos().z + ".hmdl");

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            Map<BlockPos, BlockState> blocks = lod.Skin();

            out.writeInt(blocks.size());

            for (Map.Entry<BlockPos, BlockState> entry : blocks.entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();

                int relativePos = (int) (((pos.getX() & 15) << 14) | ((pos.getZ() & 15) << 10) | (pos.getY()+64 & 1023));
                System.out.println(relativePos);
                out.writeInt(relativePos);

                out.writeInt(Block.getRawIdFromState(state));
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
