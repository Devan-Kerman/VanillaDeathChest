package com.therandomlabs.vanilladeathchest.handler;

import java.util.UUID;
import com.therandomlabs.vanilladeathchest.api.deathchest.DeathChestDefenseEntity;
import com.therandomlabs.vanilladeathchest.api.event.livingentity.LivingEntityDropCallback;
import com.therandomlabs.vanilladeathchest.api.event.livingentity.LivingEntityDropExperienceCallback;
import com.therandomlabs.vanilladeathchest.api.event.livingentity.LivingEntityTickCallback;
import com.therandomlabs.vanilladeathchest.config.VDCConfig;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class DefenseEntityHandler implements
		LivingEntityTickCallback, LivingEntityDropCallback, LivingEntityDropExperienceCallback {
	@Override
	public void onLivingEntityTick(MobEntity entity, DeathChestDefenseEntity defenseEntity) {
		final World world = entity.getEntityWorld();

		if(world.isClient) {
			return;
		}

		final UUID playerUUID = defenseEntity.getDeathChestPlayer();

		if(playerUUID == null) {
			return;
		}

		final PlayerEntity player = world.getPlayerByUuid(playerUUID);

		if(player != null) {
			entity.setTarget(player);
		}

		if(VDCConfig.defense.defenseEntityMaxDistanceSquared == 0.0) {
			return;
		}

		final BlockPos pos = defenseEntity.getDeathChestPos();
		final Vec3d entityPos = entity.getPos();

		final double distanceSq = entityPos.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
		final double distanceSqFromPlayer;

		if(player == null) {
			distanceSqFromPlayer = Double.MAX_VALUE;
		} else {
			distanceSqFromPlayer = entityPos.squaredDistanceTo(player.getPos());
		}

		if(distanceSq > VDCConfig.defense.defenseEntityMaxDistanceSquared) {
			final double maxDistanceSqFromPlayer =
					VDCConfig.defense.defenseEntityMaxDistanceSquaredFromPlayer;

			if(maxDistanceSqFromPlayer == 0.0 || distanceSqFromPlayer > maxDistanceSqFromPlayer) {
				entity.setPosition(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
			}
		}
	}

	@Override
	public boolean onLivingEntityDrop(MobEntity entity,
			DeathChestDefenseEntity defenseEntity, boolean recentlyHit, int lootingModifier,
			DamageSource source) {
		return VDCConfig.defense.defenseEntityDropsItems ||
				defenseEntity.getDeathChestPlayer() == null;
	}

	@Override
	public int onLivingEntityDropExperience(MobEntity entity, DeathChestDefenseEntity defenseEntity,
			int experience) {
		if(!VDCConfig.defense.defenseEntityDropsExperience &&
				defenseEntity.getDeathChestPlayer() != null) {
			return 0;
		}

		return experience;
	}
}
