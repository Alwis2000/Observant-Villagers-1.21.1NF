package com.stereowalker.obville.interfaces;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.stereowalker.obville.world.entity.ModEntities;
import com.stereowalker.obville.world.entity.VillageLeader;
import com.stereowalker.obville.world.entity.ai.memories.ModMemories;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public interface ISheep {
	public Villager villager();

	private boolean leaderSpawnConditionsMet(long pGameTime) {
		return true;
	}

	public default boolean wantsToSpawnLeader(long pGameTime) {
		if (!leaderSpawnConditionsMet(villager().level().getGameTime())) {
			return false;
		} else {
			return !villager().getBrain().hasMemoryValue(ModMemories.LEADER_DETECTED_RECENTLY.value().get());
		}
	}

	public default boolean spawnLeaderIfNeeded(ServerLevel pServerLevel, long pGameTime, int pMinVillagerAmount) {
		if (this.wantsToSpawnLeader(pGameTime)) {
			AABB aabb = villager().getBoundingBox().inflate(10.0D, 10.0D, 10.0D);
			List<Villager> list = pServerLevel.getEntitiesOfClass(Villager.class, aabb);
			List<Villager> list1 = list.stream().filter((p_186293_) -> {
				return ((ISheep) p_186293_).wantsToSpawnLeader(pGameTime);
			}).limit(5L).collect(Collectors.toList());
			if (list1.size() >= pMinVillagerAmount) {
				VillageLeader irongolem = trySpawnLeader(pServerLevel);
				if (irongolem != null) {
					return true;
				}
			}
		}
		return false;
	}

	@Nullable
	private VillageLeader trySpawnLeader(ServerLevel pServerLevel) {
		BlockPos blockpos = villager().blockPosition();

		for (int i = 0; i < 10; ++i) {
			double d0 = (double) (pServerLevel.random.nextInt(16) - 8);
			double d1 = (double) (pServerLevel.random.nextInt(16) - 8);
			BlockPos blockpos1 = this.findSpawnPositionForLeaderInColumn(blockpos, d0, d1);
			if (blockpos1 != null) {
				VillageLeader irongolem = ModEntities.VILLAGE_CHIEF.value().get().create(pServerLevel,
						(java.util.function.Consumer<VillageLeader>) null, blockpos1, MobSpawnType.MOB_SUMMONED, false,
						false);
				if (irongolem != null) {
					if (irongolem.checkSpawnRules(pServerLevel, MobSpawnType.MOB_SUMMONED)
							&& irongolem.checkSpawnObstruction(pServerLevel)) {
						pServerLevel.addFreshEntityWithPassengers(irongolem);
						return irongolem;
					}

					irongolem.discard();
				}
			}
		}

		return null;
	}

	@Nullable
	private BlockPos findSpawnPositionForLeaderInColumn(BlockPos pVillagerPos, double pOffsetX, double pOffsetZ) {
		BlockPos blockpos = pVillagerPos.offset((int) pOffsetX, 6, (int) pOffsetZ);
		BlockState blockstate = villager().level().getBlockState(blockpos);

		for (int j = 6; j >= -6; --j) {
			BlockPos blockpos1 = blockpos;
			BlockState blockstate1 = blockstate;
			blockpos = blockpos.below();
			blockstate = villager().level().getBlockState(blockpos);
			if ((blockstate1.isAir() || !blockstate1.getFluidState().isEmpty()) && blockstate.isSolidRender(villager().level(), blockpos)) {
				return blockpos1;
			}
		}

		return null;
	}
}
