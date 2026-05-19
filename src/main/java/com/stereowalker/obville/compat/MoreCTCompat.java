package com.stereowalker.obville.compat;

import com.stereowalker.obville.Law;
import com.stereowalker.obville.Laws;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class MoreCTCompat {
	public static boolean fromMod(Block block){
		ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
		return key.getNamespace().equals("morecraftingtables") && key.getPath().endsWith("_crafting_table");
	}
	
	public static Item equivalentItem(Block block){
		ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
		if (key.getNamespace().equals("morecraftingtables") && key.getPath().endsWith("_crafting_table")) {
			return BuiltInRegistries.ITEM.get(key);
		}
		return null;
	}
	
	public static Law equivalentLaw(Block block){
		ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
		if (!key.getNamespace().equals("morecraftingtables")) return null;
		switch (key.getPath()) {
			case "acacia_crafting_table": return Laws.BREAKING_ACACIA_CRAFTING_TABLES;
			case "azalea_crafting_table": return Laws.BREAKING_AZALEA_CRAFTING_TABLES;
			case "birch_crafting_table": return Laws.BREAKING_BIRCH_CRAFTING_TABLES;
			case "blossom_crafting_table": return Laws.BREAKING_BLOSSOM_CRAFTING_TABLES;
			case "cherry_crafting_table": return Laws.BREAKING_CHERRY_CRAFTING_TABLES;
			case "crimson_crafting_table": return Laws.BREAKING_CRIMSON_CRAFTING_TABLES;
			case "dark_oak_crafting_table": return Laws.BREAKING_DARK_OAK_CRAFTING_TABLES;
			case "dead_crafting_table": return Laws.BREAKING_DEAD_CRAFTING_TABLES;
			case "fir_crafting_table": return Laws.BREAKING_FIR_CRAFTING_TABLES;
			case "hellbark_crafting_table": return Laws.BREAKING_HELLBARK_CRAFTING_TABLES;
			case "jacaranda_crafting_table": return Laws.BREAKING_JACARANDA_CRAFTING_TABLES;
			case "jungle_crafting_table": return Laws.BREAKING_JUNGLE_CRAFTING_TABLES;
			case "magic_crafting_table": return Laws.BREAKING_MAGIC_CRAFTING_TABLES;
			case "mahogany_crafting_table": return Laws.BREAKING_MAHOGANY_CRAFTING_TABLES;
			case "palm_crafting_table": return Laws.BREAKING_PALM_CRAFTING_TABLES;
			case "redwood_crafting_table": return Laws.BREAKING_REDWOOD_CRAFTING_TABLES;
			case "spruce_crafting_table": return Laws.BREAKING_SPRUCE_CRAFTING_TABLES;
			case "umbran_crafting_table": return Laws.BREAKING_UMBRAN_CRAFTING_TABLES;
			case "warped_crafting_table": return Laws.BREAKING_WARPED_CRAFTING_TABLES;
			case "willow_crafting_table": return Laws.BREAKING_WILLOW_CRAFTING_TABLES;
			default: return null;
		}
	}
}
