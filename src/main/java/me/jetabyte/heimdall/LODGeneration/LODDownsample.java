package me.jetabyte.heimdall.LODGeneration;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import java.util.Map;

public class LODDownsample {

    public static LOD simplify(LOD source, int scale) {
        LOD simplified = new LOD(source.getPos());

        // Scale 2 = shift 1, Scale 4 = shift 2, Scale 8 = shift 3
        int shift = (int) (Math.log(scale) / Math.log(2));

        for (Map.Entry<BlockPos, BlockState> entry : source.Skin().entrySet()) {
            BlockPos originalPos = entry.getKey();

            int newX = (originalPos.getX() >> shift) << shift;
            int newY = (originalPos.getY() >> shift) << shift;
            int newZ = (originalPos.getZ() >> shift) << shift;

            BlockPos simplifiedPos = new BlockPos(newX, newY, newZ);

            if (!simplified.Skin().containsKey(simplifiedPos) ||
                    originalPos.getY() > simplifiedPos.getY()) {
                simplified.addBlock(simplifiedPos, entry.getValue());
            }
        }

        return simplified;
    }
}