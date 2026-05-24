package com.stereowalker.obville.world.entity.ai.goal;

import java.util.EnumSet;
// import java.util.Optional;

import com.stereowalker.obville.interfaces.IVillager;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;

public class VillagerSandboxGoal extends Goal {
	private final Villager villager;
	private final double speedModifier;
	private BlockPos targetPos;
	private int ticksRunning;
	private ArmorStand seat;
	private SandboxType activeType;
	private static final int MAX_DURATION = 600; // 30 seconds max sitting
	private int cooldown = 0;

	public enum SandboxType {
		SIT_STAIRS,
		WARM_CAMPFIRE,
		INSPECT_BELL
	}

	public VillagerSandboxGoal(Villager villager, double speedModifier) {
		this.villager = villager;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
	}

	@Override
	public boolean canUse() {
		if (this.cooldown > 0) {
			this.cooldown--;
			return false;
		}
		if (this.villager.getUnhappyCounter() > 0) return false;
		if (this.villager.isSleeping() || this.villager.getTradingPlayer() != null) return false;
		if (this.villager instanceof IVillager<?> iVillager && iVillager.isPanicking()) return false;
		
		// If they have a job to do, we shouldn't interrupt too much, but during "idle" times they don't have paths.
		if (this.villager.getNavigation().isInProgress()) return false;
		
		// Only trigger occasionally (roughly once every 15-30 seconds if idle)
		if (this.villager.getRandom().nextInt(100) != 0) return false;
		
		BlockPos found = findInteractable();
		if (found != null) {
			this.targetPos = found;
			return true;
		}
		
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.ticksRunning > MAX_DURATION) return false;
		if (this.villager.getUnhappyCounter() > 0) return false;
		if (this.villager.getTradingPlayer() != null) return false;
		if (this.villager instanceof IVillager<?> iVillager && iVillager.isPanicking()) return false;
		if (this.villager.hurtTime > 0) return false;
		
		// Check if the block is still valid
		if (this.targetPos != null && this.villager.level().isLoaded(this.targetPos)) {
			BlockState state = this.villager.level().getBlockState(this.targetPos);
			if (activeType == SandboxType.SIT_STAIRS && !(state.getBlock() instanceof StairBlock)) return false;
			if (activeType == SandboxType.WARM_CAMPFIRE && !(state.getBlock() instanceof CampfireBlock)) return false;
			if (activeType == SandboxType.INSPECT_BELL && !(state.getBlock() instanceof BellBlock)) return false;
			return true;
		}
		
		return false;
	}

	@Override
	public void start() {
		this.ticksRunning = 0;
		if (this.targetPos != null) {
			this.villager.getNavigation().moveTo(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D, this.speedModifier);
		}
	}

	@Override
	public void tick() {
		this.ticksRunning++;
		if (this.targetPos == null) return;

		double distSq = this.villager.distanceToSqr(this.targetPos.getX() + 0.5D, this.targetPos.getY(), this.targetPos.getZ() + 0.5D);
		
		if (activeType == SandboxType.SIT_STAIRS) {
			if (distSq < 2.0D) {
				this.villager.getNavigation().stop();
				if (this.seat == null && this.villager.getVehicle() == null) {
					sitDown();
				}
			}
		} else if (activeType == SandboxType.WARM_CAMPFIRE || activeType == SandboxType.INSPECT_BELL) {
			if (distSq < 9.0D) { // Stay a bit away from campfire/bell
				this.villager.getNavigation().stop();
				this.villager.getLookControl().setLookAt(this.targetPos.getX() + 0.5D, this.targetPos.getY() + 0.5D, this.targetPos.getZ() + 0.5D, 30.0F, 30.0F);
				
				if (this.ticksRunning % 60 == 0 && activeType == SandboxType.WARM_CAMPFIRE) {
					// Play warming animation or sound (for now just look around happily)
					this.villager.level().broadcastEntityEvent(this.villager, (byte)14); // Happy villager particles
				}
			}
		}
	}

	@Override
	public void stop() {
		this.targetPos = null;
		this.villager.getNavigation().stop();
		this.cooldown = 200 + this.villager.getRandom().nextInt(200); // Wait 10-20 seconds before doing it again
		
		if (this.seat != null) {
			this.villager.stopRiding();
			this.seat.discard();
			this.seat = null;
		}
	}

	private BlockPos findInteractable() {
		Level level = this.villager.level();
		BlockPos villagerPos = this.villager.blockPosition();
		BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
		
		int radius = 10;
		for (int x = -radius; x <= radius; x++) {
			for (int y = -3; y <= 3; y++) {
				for (int z = -radius; z <= radius; z++) {
					mutable.set(villagerPos.getX() + x, villagerPos.getY() + y, villagerPos.getZ() + z);
					if (level.isLoaded(mutable)) {
						BlockState state = level.getBlockState(mutable);
						if (state.getBlock() instanceof StairBlock) {
							// Check if it's right side up
							if (state.getValue(StairBlock.HALF) == Half.BOTTOM) {
								// Ensure there is air above it to sit
								if (level.getBlockState(mutable.above()).isAir()) {
									// Check if occupied by another seat
									boolean occupied = !level.getEntitiesOfClass(ArmorStand.class, new net.minecraft.world.phys.AABB(mutable)).isEmpty();
									if (!occupied) {
										this.activeType = SandboxType.SIT_STAIRS;
										return mutable.immutable();
									}
								}
							}
						} else if (state.getBlock() instanceof CampfireBlock) {
							this.activeType = SandboxType.WARM_CAMPFIRE;
							return mutable.immutable();
						} else if (state.getBlock() instanceof BellBlock) {
							this.activeType = SandboxType.INSPECT_BELL;
							return mutable.immutable();
						}
					}
				}
			}
		}
		return null;
	}

	private void sitDown() {
		Level level = this.villager.level();
		if (level instanceof ServerLevel serverLevel) {
			BlockState state = level.getBlockState(this.targetPos);
			if (state.getBlock() instanceof StairBlock) {
				ArmorStand armorStand = EntityType.ARMOR_STAND.create(serverLevel);
				if (armorStand != null) {
					// Position armor stand inside the stair block, low down so the villager sits properly
					armorStand.setPos(this.targetPos.getX() + 0.5D, this.targetPos.getY() - 1.5D, this.targetPos.getZ() + 0.5D);
					armorStand.setInvisible(true);
					armorStand.setInvulnerable(true);
					armorStand.setNoGravity(true);
					armorStand.setNoBasePlate(true);
					armorStand.addTag("ObVilleSandboxSeat");
					
					// Face the opposite direction of the stairs' back
					net.minecraft.core.Direction dir = state.getValue(StairBlock.FACING);
					armorStand.setYRot(dir.getOpposite().toYRot());
					
					serverLevel.addFreshEntity(armorStand);
					this.seat = armorStand;
					this.villager.startRiding(armorStand);
				}
			}
		}
	}
}
