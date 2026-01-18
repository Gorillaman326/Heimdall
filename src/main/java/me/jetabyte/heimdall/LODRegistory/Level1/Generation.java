package me.jetabyte.heimdall.LODRegistory.Level1;

import me.jetabyte.heimdall.LODGeneration.LOD;
import net.minecraft.block.BlockState;
import net.minecraft.block.StainedGlassBlock;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

public class Generation {
    public LOD generateLOD(World world, Chunk chunk) {
        LOD lod = new LOD(chunk.getPos());

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int maxY = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE).get(x, z);

                for (int y = world.getBottomY(); y <= maxY; y++) {
                    BlockPos pos = new BlockPos(chunk.getPos().getStartX() + x, y, chunk.getPos().getStartZ() + z);
                    BlockState state = world.getBlockState(pos);

                    if (isImportant(state, pos, world)) {
                        if (isExposed(world, pos) && isExposedToSky(world, pos)) {
                            lod.addBlock(pos, state);
                        }
                    }
                }
            }
        }
        return lod;
    }

    public boolean isImportant(BlockState state, BlockPos pos, World world) {
        if (state.isAir()) return false;
        if (state.isFullCube(world, pos)) return true;
        if (state.isIn(BlockTags.STAIRS) || state.isIn(BlockTags.SLABS) || state.isIn(BlockTags.WALLS) || state.isIn(BlockTags.FENCES)) {
            return true;
        }
        if (state.isIn(BlockTags.LEAVES)) return true;
        if (state.isIn(BlockTags.IMPERMEABLE) || state.getBlock() instanceof StainedGlassBlock) {
            return true;
        }
        return !state.getFluidState().isEmpty();
    }

    private boolean isExposed(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.offset(dir);

            if (!world.isChunkLoaded(neighborPos.getX() >> 4, neighborPos.getZ() >> 4)) {
                return true;
            }

            BlockState neighborState = world.getBlockState(neighborPos);

            if (neighborState.isAir() || !neighborState.isOpaqueFullCube(world, neighborPos)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExposedToSky(World world, BlockPos pos) {
        return world.getLightLevel(LightType.SKY, pos.up()) > 0 ||
                world.getLightLevel(LightType.SKY, pos.down()) > 0 ||
                world.getLightLevel(LightType.SKY, pos.north()) > 0 ||
                world.getLightLevel(LightType.SKY, pos.east()) > 0 ||
                world.getLightLevel(LightType.SKY, pos.south()) > 0 ||
                world.getLightLevel(LightType.SKY, pos.west()) > 0;
    }
}
