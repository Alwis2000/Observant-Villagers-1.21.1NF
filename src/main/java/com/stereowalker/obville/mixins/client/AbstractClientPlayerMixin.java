package com.stereowalker.obville.mixins.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stereowalker.obville.interfaces.IModdedEntity;

import net.minecraft.client.player.AbstractClientPlayer;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements IModdedEntity {

	@Inject(method = "tick", at = @At("TAIL"))
	public void tickInject(CallbackInfo ci) {
		AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
		getData().baseClientTick(player);
	}
}
