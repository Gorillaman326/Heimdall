package me.jetabyte.heimdall;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.jetabyte.heimdall.LODDistrubution.PacketHandler;
import me.jetabyte.heimdall.LODGeneration.LOD;
import me.jetabyte.heimdall.LODRegistory.LODLoading;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.*;

public class HeimdallClient implements ClientModInitializer {
	// Change from a single LOD to a thread-safe List of LODs
	public static final List<LOD> activeLODs = new CopyOnWriteArrayList<>();
	public int currentScale = 1;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(PacketHandler.TYPE.getId(), (client, handler, buf, responseSender) -> {
			PacketHandler payload = new PacketHandler(buf);

			client.execute(() -> {
				LOD lod = LOD.fromBytes(payload.pos(), payload.data());
				currentScale = payload.scale();
				client.execute(() -> {
					System.out.println("LOD received for: " + payload.pos() + "at scale " + payload.scale());

					LODMeshHandler.buildMesh(lod, currentScale);
					activeLODs.add(lod);

					if (client.player != null) {
						client.player.sendMessage(Text.literal("§a[Heimdall]§f Received and Baked LOD for " + payload.pos()), false);
					}
				});
			});
		});

		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(literal("heimdall_client")
					.then(literal("load")
							.then(argument("scale", IntegerArgumentType.integer(1))
									.executes(context -> {
										currentScale = IntegerArgumentType.getInteger(context, "scale");
										var client = MinecraftClient.getInstance();
										var server = client.getServer();

										if (server == null || client.player == null) {
											client.player.sendMessage(Text.literal("§cOnly works in singleplayer for now!"), false);
											return 0;
										}

										ChunkPos pos = new ChunkPos(client.player.getBlockPos());
										LOD lod = LODLoading.loadFromFile(server, pos, currentScale);

										if (lod != null) {
											activeLODs.clear();
											activeLODs.add(lod);
											client.player.sendMessage(Text.literal("§a[Heimdall]§f Rendering chunk at scale " + currentScale), false);
										}
										return 1;
									})
							)
					)
					.then(literal("range")
							.then(argument("range", IntegerArgumentType.integer(1, 20))
									.then(argument("scale", IntegerArgumentType.integer(1))
											.executes(context -> {
												int range = IntegerArgumentType.getInteger(context, "range");
												int scale = IntegerArgumentType.getInteger(context, "scale");
												currentScale = scale;

												MinecraftClient client = MinecraftClient.getInstance();
												var server = client.getServer();
												if (server == null) return 0;

												ChunkPos center = new ChunkPos(client.player.getBlockPos());

												// ASYNC LOADING: This keeps your FPS at 120 while loading
												CompletableFuture.runAsync(() -> {
													activeLODs.clear();
													LODMeshHandler.MESH_CACHE.clear();

													for (int x = -range; x <= range; x++) {
														for (int z = -range; z <= range; z++) {
															ChunkPos target = new ChunkPos(center.x + x, center.z + z);
															LOD lod = LODLoading.loadFromFile(server, target, scale);
															if (lod != null) {
																LODMeshHandler.buildMesh(lod, scale);
																activeLODs.add(lod);
															}
														}
													}
												}).thenRun(() -> client.execute(() ->
														client.player.sendMessage(Text.literal("§a[Heimdall] Range loaded!"), false)));

												return 1;
											}))))
					.then(literal("clear").executes(context -> {
						activeLODs.clear();
						LODMeshHandler.MESH_CACHE.clear();
						return 1;
					}))
			);
		});

		WorldRenderEvents.LAST.register(context -> {
			// Use the pass-based renderer for the whole list
			renderLODPass(context, currentScale, 1.0f);
		});
	}

	private static void renderLODPass(WorldRenderContext context, int scale, float offset) {
		if (activeLODs.isEmpty()) return;

		RenderSystem.enablePolygonOffset();
		RenderSystem.polygonOffset(1.0f, offset);

		for (LOD lod : activeLODs) {
			if (LODMeshHandler.MESH_CACHE.containsKey(lod.getPos())) {
				DebugRender.renderMesh(context, lod.getPos());
			}
		}

		RenderSystem.disablePolygonOffset();
	}
}