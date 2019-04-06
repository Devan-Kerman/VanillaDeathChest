package com.therandomlabs.vanilladeathchest.handler;

import java.util.Map;
import com.therandomlabs.vanilladeathchest.api.deathchest.DeathChest;
import com.therandomlabs.vanilladeathchest.api.deathchest.DeathChestManager;
import com.therandomlabs.vanilladeathchest.config.VDCConfig;
import com.therandomlabs.vanilladeathchest.world.storage.VDCSavedData;
import net.fabricmc.fabric.api.event.server.ServerTickCallback;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkManager;

public class DeathChestContentsChecker implements ServerTickCallback {
	@Override
	public void tick(MinecraftServer server) {
		for(ServerWorld world : server.getWorlds()) {
			worldTick(world);
		}
	}

	private static void worldTick(ServerWorld world) {
		if(!VDCConfig.misc.deathChestsDisappearWhenEmptied) {
			return;
		}

		final VDCSavedData savedData = VDCSavedData.get(world);
		final ChunkManager provider = world.getChunkManager();

		for(Map.Entry<BlockPos, DeathChest> entry : savedData.getDeathChests().entrySet()) {
			final BlockPos pos = entry.getKey();

			//Make sure we don't unnecessarily load any chunks
			if(!provider.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
				continue;
			}

			final BlockEntity blockEntity = world.getBlockEntity(pos);

			if(!(blockEntity instanceof LockableContainerBlockEntity)) {
				continue;
			}

			if(((LockableContainerBlockEntity) blockEntity).isInvEmpty()) {
				DeathChestManager.removeDeathChest(world, pos);

				world.setBlockState(pos, Blocks.AIR.getDefaultState());

				if(entry.getValue().isDoubleChest()) {
					world.setBlockState(pos.east(), Blocks.AIR.getDefaultState());
				}
			}
		}
	}
}
