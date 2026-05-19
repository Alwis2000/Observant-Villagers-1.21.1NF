package com.stereowalker.obville.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stereowalker.obville.world.entity.VillageLeader;

import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

@Mixin(BellBlockEntity.class)
public class BellBlockEntityMixin {

	@Inject(method = "serverTick", at = @At("HEAD"))
	private static void makeLeaderGlow(Level pLevel, BlockPos pPos, BlockState pState, BellBlockEntity pBlockEntity, CallbackInfo ci) {
		BellBlockEntityAccessor accessor = (BellBlockEntityAccessor) pBlockEntity;
		List<net.minecraft.world.entity.LivingEntity> nearby = accessor.getNearbyEntities();
		if (nearby != null && pBlockEntity.shaking && !BellBlockEntityAccessor.callAreRaidersNearby(pPos, nearby)) {
			nearby.stream().filter((entity) -> {
				return entity instanceof VillageLeader && !entity.hasEffect(MobEffects.GLOWING);
			}).forEach((entity) -> entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, false, false)));
		}
	}
}
