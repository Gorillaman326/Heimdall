package me.jetabyte.heimdall;

import me.jetabyte.heimdall.LODGeneration.LOD;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.fabricmc.fabric.api.renderer.v1.mesh.MeshBuilder;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LODMeshHandler {
    public static final Map<ChunkPos, Mesh> MESH_CACHE = new ConcurrentHashMap<>();

    public static void buildMesh(LOD lod, int scale) {
        Renderer renderer = RendererAccess.INSTANCE.getRenderer();
        if (renderer == null) return;

        MeshBuilder builder = renderer.meshBuilder();
        QuadEmitter emitter = builder.getEmitter();
        BlockRenderManager manager = MinecraftClient.getInstance().getBlockRenderManager();
        Random random = Random.create();

        for (var entry : lod.Skin().entrySet()) {
            bakeQuads(emitter, manager.getModel(entry.getValue()), entry.getValue(), entry.getKey(), random, scale);
        }

        MESH_CACHE.put(lod.getPos(), builder.build());
    }

    private static void bakeQuads(QuadEmitter emitter, BakedModel model, BlockState state, BlockPos pos, Random random, int scale) {
        float s = (float) scale;
        for (Direction face : Direction.values()) {
            for (BakedQuad quad : model.getQuads(state, face, random)) {
                emitter.fromVanilla(quad, RendererAccess.INSTANCE.getRenderer().materialFinder().find(), face);

                float xBase = (float)(pos.getX() & 15);
                float yBase = (float)pos.getY();
                float zBase = (float)(pos.getZ() & 15);

                for (int i = 0; i < 4; i++) {
                    emitter.pos(i, xBase + (emitter.x(i) * s), yBase + (emitter.y(i) * s), zBase + (emitter.z(i) * s));
                }
                emitter.emit();
            }
        }
    }
}