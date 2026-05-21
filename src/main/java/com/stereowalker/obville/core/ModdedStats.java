package com.stereowalker.obville.core;

import com.stereowalker.obville.ObVille;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

public class ModdedStats {
	public static String getModDataString() {
		return ObVille.MOD_ID + ":PlayerData";
	}

	public static CompoundTag getModNBT(Entity entity) {
		return entity.getPersistentData().getCompound(getModDataString());
	}

	public static CompoundTag getOrCreateModNBT(Entity entity) {
		if (!entity.getPersistentData().contains(getModDataString(), 10)) {
			entity.getPersistentData().put(getModDataString(), new CompoundTag());
		}
		return entity.getPersistentData().getCompound(getModDataString());
	}

	public static void setModNBT(CompoundTag nbt, Entity entity) {
		entity.getPersistentData().put(getModDataString(), nbt);
	}
}
