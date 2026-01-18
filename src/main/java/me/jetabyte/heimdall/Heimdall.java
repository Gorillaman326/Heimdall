package me.jetabyte.heimdall;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import me.jetabyte.heimdall.LODDistrubution.PacketHandler;
import me.jetabyte.heimdall.LODDistrubution.SendLODToClient;
import me.jetabyte.heimdall.LODGeneration.LODDownsample;
import me.jetabyte.heimdall.LODRegistory.LODLoading;
import me.jetabyte.heimdall.LODRegistory.Level1.Generation;
import me.jetabyte.heimdall.LODGeneration.LOD;
import me.jetabyte.heimdall.LODRegistory.LODSaving;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import static net.minecraft.server.command.CommandManager.*;

import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Heimdall implements ModInitializer {
	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("heimdall")
					.then(literal("scan")
							.executes(context -> {
								// 1. Get the player who ran the command
								var player = context.getSource().getPlayerOrThrow();
								var world = player.getWorld();

								// 2. Get the chunk the player is standing in
								Chunk chunk = world.getChunk(player.getBlockPos());

								// 3. Run your Generation logic
								Generation gen = new Generation();
								LOD result = gen.generateLOD(world, chunk);

								// 4. Send feedback to the player
								int count = result.getBlockCount();
								context.getSource().sendFeedback(() ->
										Text.literal("§a[Heimdall]§f Scanned chunk " + chunk.getPos() +
												". Found §6" + count + "§f visible blocks."), false);

								return 1;
							})
					)
					.then(literal("save")
							.executes(context -> {
							var player = context.getSource().getPlayerOrThrow();
								var server = context.getSource().getServer();
							var world = player.getWorld();

							Chunk chunk = world.getChunk(player.getBlockPos());
							Generation gen = new Generation();
							LOD result = gen.generateLOD(world, chunk);

							File worldDir = context.getSource().getServer().getRunDirectory().toPath().resolve("world").toFile();
							LODSaving.saveToFile(server, 1,1,result);
							LODSaving.saveToFile(server,1,2, LODDownsample.simplify(result, 2));
							LODSaving.saveToFile(server,1,4, LODDownsample.simplify(result, 4));
							LODSaving.saveToFile(server,1,8, LODDownsample.simplify(result, 8));
							context.getSource().sendFeedback(() -> Text.literal("§a[Heimdall]§f Saved to disk!"), false);
							return	1;
							})
					)
					.then(literal("load")
							.then(argument("scale", IntegerArgumentType.integer(1))
								.executes(context -> {
									int currentScale = IntegerArgumentType.getInteger(context, "scale");
									// Use the command source instead of MinecraftClient
									var player = context.getSource().getPlayerOrThrow();
									var server = context.getSource().getServer();
									var world = player.getServerWorld();
	
									// 1. Get current chunk position from player
									ChunkPos pos = new ChunkPos(player.getBlockPos());
	
									// 2. Load the LOD object from your Loading class
									var lod = LODLoading.loadFromFile(server, pos, currentScale);
	
									if (lod != null) {
										// 3. Physically place the blocks
										lod.Skin().forEach((blockPos, state) -> {
											System.out.println("Placing " + state + " at " + blockPos.toShortString());
											world.setBlockState(blockPos, state, 3);
										});
	
										context.getSource().sendFeedback(() ->
												Text.literal("§a[Heimdall]§f Physical paste complete! " + lod.getBlockCount() + " blocks added."), false);
									} else {
										context.getSource().sendFeedback(() ->
												Text.literal("§c[Heimdall]§f No saved LOD found for chunk " + pos), false);
									}
	
									return 1;
								})
							)
					)
					.then(literal("request")
							.then(argument("scale", IntegerArgumentType.integer(1))
									.then(argument("radius", IntegerArgumentType.integer(1))
										.executes(context -> {
											int currentScale = IntegerArgumentType.getInteger(context, "scale");
											int radius = IntegerArgumentType.getInteger(context, "radius");
											var server = context.getSource().getServer();

											ServerPlayerEntity player = context.getSource().getPlayer();
											if (player != null) {
												ChunkPos center = new ChunkPos(player.getBlockPos());

												for (int dx = -radius; dx <= radius; dx++) {
													for (int dz = -radius; dz <= radius; dz++) {
														ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);

														var lod = me.jetabyte.heimdall.LODRegistory.LODLoading
																.loadFromFile(server, pos, currentScale);

														if (lod != null) {
															byte[] data = lod.getBytes();
															SendLODToClient.SendLodToClient(player, pos, data, currentScale);
														} else {
															context.getSource().sendError(
																	Text.literal("LOD file not found for chunk " + pos.x + ", " + pos.z)
															);
														}
													}
												}

												context.getSource().sendFeedback(
														() -> Text.literal("Sent LOD packets for radius " + radius + " (" + ((radius * 2 + 1) * (radius * 2 + 1)) + " chunks)"),
														false
												);
											}
											return 1;
										})
									)
							)
					)
					.then(literal("area_test")
							.then(argument("scale", IntegerArgumentType.integer(1))
									.executes(context -> {
										int currentScale = IntegerArgumentType.getInteger(context, "scale");
										var server = context.getSource().getServer();
										ServerPlayerEntity player = context.getSource().getPlayer();
										if (player != null) {
											ChunkPos center = new ChunkPos(player.getBlockPos());
											int cx = center.x;
											int cz = center.z;
											List<ChunkPos> area = new ArrayList<>();
											for (int dx = -1; dx <= 1; dx++) {
												for (int dz = -1; dz <= 1; dz++) {
													area.add(new ChunkPos(cx + dx, cz + dz));

													ChunkPos pos = new ChunkPos(center.x + dx, center.z + dz);

													var lod = me.jetabyte.heimdall.LODRegistory.LODLoading
															.loadFromFile(server, pos, currentScale);

													if (lod != null) {
														byte[] data = lod.getBytes();
														SendLODToClient.SendLodToClient(player, pos, data, currentScale);
													} else {
														context.getSource().sendError(
																Text.literal("LOD file not found for chunk " + pos.x + ", " + pos.z)
														);
													}
												}
											}
											System.out.print(area);
										}
										return 1;
									})
							)
					)
			);
		});
	}
}