package com.stereowalker.obville.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stereowalker.obville.interfaces.IWanderingTrader;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.WanderingTrader;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

	@Inject(method = "tick", at = @At("TAIL"))
	private void tickInject(CallbackInfo ci) {
		LivingEntity entity = (LivingEntity) (Object) this;
		if (entity instanceof WanderingTrader trader) {
			((IWanderingTrader) trader).obville$tickTrader();
		}
	}
}
