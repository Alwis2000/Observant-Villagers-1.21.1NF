package com.stereowalker.obville.interfaces;

import com.stereowalker.obville.dat.OVModData;

import net.minecraft.world.entity.LivingEntity;

public interface IModdedEntity {
	public OVModData getData();

	public void setData(OVModData data);

	private LivingEntity self() {
		return (LivingEntity) this;
	}
}
