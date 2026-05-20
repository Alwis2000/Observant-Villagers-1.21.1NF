package com.stereowalker.obville.world.entity.ai.goal;

import java.util.EnumSet;

import com.stereowalker.obville.dat.OVModData;
import com.stereowalker.obville.interfaces.IModdedEntity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class StareAtDistrustedPlayerGoal extends Goal {
	private static final TargetingConditions TEMP_TARGETING = TargetingConditions.forNonCombat().range(10.0D).ignoreLineOfSight();
	private final TargetingConditions targetingConditions;
	protected final Mob mob;
	protected Player player;
	private final float lookDistance;
	private int lookTime;
	private boolean shouldFollow;

	public StareAtDistrustedPlayerGoal(Mob mob, float lookDistance) {
		this.mob = mob;
		this.lookDistance = lookDistance;
		this.setFlags(EnumSet.of(Goal.Flag.LOOK, Goal.Flag.MOVE));
		this.targetingConditions = TEMP_TARGETING.copy().range(lookDistance).selector(this::isDistrustedPlayer);
	}

	private boolean isDistrustedPlayer(LivingEntity entity) {
		if (entity instanceof Player p) {
			OVModData data = ((IModdedEntity) p).getData();
			return data.isInAnyVillage() && data.IsDistrusted();
		}
		return false;
	}

	private boolean isMobInVillage() {
		if (this.mob.level() instanceof ServerLevel serverLevel) {
			return com.stereowalker.obville.ObVille.determineVillage(serverLevel, this.mob.blockPosition()) >= 0;
		}
		return false;
	}

	private boolean isPlayerInVillage(Player p) {
		OVModData data = ((IModdedEntity) p).getData();
		return data.isInAnyVillage();
	}

	@Override
	public boolean canUse() {
		this.player = this.mob.level().getNearestPlayer(this.targetingConditions, this.mob);
		return this.player != null;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.player == null || !this.player.isAlive()) {
			return false;
		} else if (this.mob.distanceToSqr(this.player) > (double) (this.lookDistance * this.lookDistance)) {
			return false;
		} else {
			return this.lookTime > 0 && isDistrustedPlayer(this.player);
		}
	}

	@Override
	public void start() {
		this.lookTime = this.adjustedTickDelay(80 + this.mob.getRandom().nextInt(80));
		if (this.player != null && isMobInVillage() && isPlayerInVillage(this.player)) {
			this.shouldFollow = this.mob.getRandom().nextFloat() < 0.35F; // 35% chance to follow
		} else {
			this.shouldFollow = false;
		}
	}

	@Override
	public void stop() {
		this.player = null;
		this.shouldFollow = false;
		this.mob.getNavigation().stop();
	}

	@Override
	public void tick() {
		if (this.player != null) {
			this.mob.getLookControl().setLookAt(this.player, (float) (this.mob.getMaxHeadYRot() + 20), (float) this.mob.getMaxHeadXRot());
			
			if (this.shouldFollow) {
				if (!isMobInVillage() || !isPlayerInVillage(this.player)) {
					this.shouldFollow = false;
					this.mob.getNavigation().stop();
				} else {
					double distSqr = this.mob.distanceToSqr(this.player);
					if (distSqr > 25.0D) { // More than 5 blocks away
						double speed = this.mob instanceof tallestegg.guardvillagers.common.entities.Guard ? 0.45D : 0.5D;
						this.mob.getNavigation().moveTo(this.player, speed);
					} else { // Less than 5 blocks away, stop and stare
						this.mob.getNavigation().stop();
					}
				}
			} else {
				this.mob.getNavigation().stop();
			}
			--this.lookTime;
		}
	}
}
