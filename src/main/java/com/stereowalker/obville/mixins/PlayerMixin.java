package com.stereowalker.obville.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stereowalker.obville.core.ModdedStats;
import com.stereowalker.obville.dat.OVModData;
import com.stereowalker.obville.interfaces.IModdedEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

@Mixin(Player.class)
public abstract class PlayerMixin implements IModdedEntity {
	@Shadow private int sleepCounter;
	private OVModData modData = new OVModData();

	@Inject(method = "<init>", at = @At("TAIL"))
	public void initInject(CallbackInfo ci) {
	}

	@Inject(method = "tick", at = @At(value = "INVOKE", shift = Shift.AFTER, target = "Lnet/minecraft/world/entity/player/Player;updateIsUnderwater()Z"))
	public void tickInject(CallbackInfo ci) {
		Player player = (Player)(Object)this;
		if (player != null) {
			CompoundTag compound = ModdedStats.getOrCreateModNBT(player);
			if(player.isAlive()) {
				if (!compound.contains("obvData")) {
					CompoundTag compound2 = new CompoundTag();
					modData.write(compound2, player.level().registryAccess());
					ModdedStats.getModNBT(player).put("obvData", compound2);
				}
			}
		}
		
		if (player != null && !player.level().isClientSide() && player instanceof ServerPlayer serverplayer) {
			getData().baseTick(serverplayer);
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
	public void readInject(CompoundTag pCompound, CallbackInfo ci) {
		Player player = (Player)(Object)this;
		if (player != null && !player.level().isClientSide()) {
			OVModData stats = new OVModData();
			if (ModdedStats.getModNBT(player) != null && ModdedStats.getModNBT(player).contains("obvData", 10)) {
				stats.read(ModdedStats.getModNBT(player).getCompound("obvData"), player.level().registryAccess());
			}
			modData = stats;
		}
	}
	
	@Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
	public void writeInject(CompoundTag pCompound, CallbackInfo ci) {
		Player player = (Player)(Object)this;
		if (player != null && !player.level().isClientSide()) {
			CompoundTag compound2 = new CompoundTag();
			modData.write(compound2, player.level().registryAccess());
			ModdedStats.getModNBT(player).put("obvData", compound2);
		}
	}

	public OVModData getData(){
		return modData;
	}
	
	@Override
	public void setData(OVModData data) {
		modData = data;
	}
}
