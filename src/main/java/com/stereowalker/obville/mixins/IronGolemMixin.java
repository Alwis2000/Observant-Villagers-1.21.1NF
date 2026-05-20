package com.stereowalker.obville.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stereowalker.obville.interfaces.IModdedEntity;
import com.stereowalker.obville.world.entity.ai.goal.StareAtDistrustedPlayerGoal;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;

@Mixin(IronGolem.class)
public abstract class IronGolemMixin {
	@Inject(method = "registerGoals", at = @At("HEAD"))
	private void registerGoalsInject(CallbackInfo ci) {
		IronGolem golem = (IronGolem) (Object) this;
		golem.goalSelector.addGoal(4, new StareAtDistrustedPlayerGoal(golem, 10.0F));
	}

	@Inject(method = "aiStep", at = @At("HEAD"))
	private void aiStepInject(CallbackInfo ci) {
		IronGolem golem = (IronGolem) (Object) this;
		if (!golem.level().isClientSide()) {
			net.minecraft.world.entity.LivingEntity target = golem.getTarget();
			if (target instanceof Player player) {
				IModdedEntity ent = (IModdedEntity) player;
				for (int village : ent.getData().vill()) {
					if (ent.getData().IsExiledAt(village)) {
						int playerVillage = ent.getData().currentVillage();
						int golemVillage = golem.level() instanceof ServerLevel serverLevel ? com.stereowalker.obville.ObVille.determineVillage(serverLevel, golem.blockPosition()) : -1;
						if (playerVillage != village || golemVillage != village) {
							golem.setTarget(null);
							golem.getNavigation().stop();
							break;
						}
					}
				}
			}
		}
	}
}
