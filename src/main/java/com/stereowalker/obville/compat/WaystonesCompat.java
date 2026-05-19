package com.stereowalker.obville.compat;

import com.stereowalker.obville.Law;
import com.stereowalker.obville.Laws;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

public class WaystonesCompat {
	public static boolean isTable(BlockEntity block){
		try {
			Class<?> clazz = Class.forName("net.blay09.mods.waystones.block.entity.WaystoneBlockEntityBase");
			if (clazz.isInstance(block)) {
				java.lang.reflect.Method getWaystone = clazz.getMethod("getWaystone");
				Object waystone = getWaystone.invoke(block);
				if (waystone != null) {
					java.lang.reflect.Method wasGenerated = waystone.getClass().getMethod("wasGenerated");
					return (Boolean) wasGenerated.invoke(waystone);
				}
			}
		} catch (Exception e) {
			// Waystones is not installed or has a different structure
		}
		return false;
	}
	
	public static Item equivalentItem(Block block){
		return block.asItem();
	}
	
	public static Law equivalentLaw(Block block){
		return Laws.BREAKING_WAYSTONES;
	}
}
