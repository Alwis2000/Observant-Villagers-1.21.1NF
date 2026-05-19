package com.stereowalker.obville.compat;

import com.mojang.datafixers.util.Pair;
import com.stereowalker.obville.world.entity.VillageLeader;

import net.minecraft.network.chat.Component;
import java.lang.reflect.Field;

public class VillagerNamesCompat {
	public static void overrideMerchantScreen(Component title, VillageLeader leader){
		try {
			Class<?> clazz = Class.forName("com.natamus.villagernames.data.Variables");
			Field field = clazz.getDeclaredField("tradedVillagerPair");
			field.setAccessible(true);
			field.set(null, new Pair<Component, Component>(leader.getName(), title));
		} catch (Exception e1) {
			try {
				Class<?> clazz = Class.forName("com.natamus.villagernames_common_forge.data.Variables");
				Field field = clazz.getDeclaredField("tradedVillagerPair");
				field.setAccessible(true);
				field.set(null, new Pair<Component, Component>(leader.getName(), title));
			} catch (Exception e2) {
				// Villager Names is not installed or has a different structure
			}
		}
	}
}
