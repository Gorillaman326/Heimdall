package me.jetabyte.heimdall;

import me.jetabyte.heimdall.LODGeneration.LOD;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.renderer.v1.mesh.Mesh;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Collection;

public class DebugRender {
    public static void render(WorldRenderContext context, LOD lod, int scale) {
        if (lod == null || lod.Skin().isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();
        BlockRenderManager manager = client.getBlockRenderManager();
        Random random = Random.create();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getCutout());

        float scaleFloat = (float) scale;

        for (var entry : lod.Skin().entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState state = entry.getValue();

            matrices.push();

            // 1. Translate to the world position relative to camera
            // We subtract camPos to get the "Relative" position for rendering
            matrices.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

            // 2. Apply the Scale (1x1, 2x2, 4x4, etc.)
            matrices.scale(scaleFloat, scaleFloat, scaleFloat);

            // 3. Render the block model
            manager.getModelRenderer().render(
                    client.world,
                    manager.getModel(state),
                    state,
                    pos,
                    matrices,
                    consumer,
                    false,
                    random,
                    state.getRenderingSeed(pos),
                    OverlayTexture.DEFAULT_UV
            );

            matrices.pop();
        }
    }
    public static void renderBatch(WorldRenderContext context, Collection<LOD> lods, int scale) {
        if (lods == null || lods.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();
        BlockRenderManager manager = client.getBlockRenderManager();
        net.minecraft.util.math.random.Random random = net.minecraft.util.math.random.Random.create();
        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getCutout());

        for (LOD lod : lods) {
            for (var entry : lod.Skin().entrySet()) {
                BlockPos pos = entry.getKey();
                BlockState state = entry.getValue();

                matrices.push();
                // Translate relative to camera
                matrices.translate(pos.getX() - camPos.x, pos.getY() - camPos.y, pos.getZ() - camPos.z);

                if (scale > 1) {
                    matrices.scale((float)scale, (float)scale, (float)scale);
                }

                manager.getModelRenderer().render(
                        client.world,
                        manager.getModel(state),
                        state,
                        pos,
                        matrices,
                        consumer,
                        false,
                        random,
                        state.getRenderingSeed(pos),
                        OverlayTexture.DEFAULT_UV
                );

                matrices.pop();
            }
        }
    }
    public static void renderMesh(WorldRenderContext context, ChunkPos pos) {
        Mesh mesh = LODMeshHandler.MESH_CACHE.get(pos);
        if (mesh == null) return;

        MatrixStack matrices = context.matrixStack();
        Vec3d camPos = context.camera().getPos();

        matrices.push();
        matrices.translate(pos.getStartX() - camPos.x, -camPos.y, pos.getStartZ() - camPos.z);

        VertexConsumer consumer = context.consumers().getBuffer(RenderLayer.getCutout());
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f posMat = entry.getPositionMatrix();
        Matrix3f normMat = entry.getNormalMatrix();

        mesh.forEach(quad -> {
            // Apply Minecraft-style directional shading
            float shade = getFaceShade(quad.lightFace());
            int c = (int)(255 * shade);

            for (int i = 0; i < 4; i++) {
                consumer.vertex(posMat, quad.x(i), quad.y(i), quad.z(i))
                        .color(c, c, c, 255) // RGB shaded by face direction
                        .texture(quad.u(i), quad.v(i))
                        .overlay(OverlayTexture.DEFAULT_UV)
                        .light(15728880) // Full bright so it's visible at distance
                        .normal(normMat, quad.normalX(i), quad.normalY(i), quad.normalZ(i))
                        .next();
            }
        });

        matrices.pop();
    }

    private static float getFaceShade(Direction dir) {
        if (dir == null) return 1.0f;
        return switch (dir) {
            case UP -> 1.0f;
            case NORTH, SOUTH -> 0.8f;
            case EAST, WEST -> 0.6f;
            case DOWN -> 0.5f;
        };
    }
}