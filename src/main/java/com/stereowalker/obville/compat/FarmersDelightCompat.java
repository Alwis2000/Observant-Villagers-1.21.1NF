package com.stereowalker.obville.compat;

import com.stereowalker.obville.Crime;
import com.stereowalker.obville.Law;
import com.stereowalker.obville.Laws;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class FarmersDelightCompat {
	
	public static boolean plantFromMod(Block block){
		String key = BuiltInRegistries.BLOCK.getKey(block).toString();
		return key.equals("farmersdelight:tomato_crop");
	}
	
	public static Crime equivalentCrime(Block block){
		String key = BuiltInRegistries.BLOCK.getKey(block).toString();
		if (key.equals("farmersdelight:tomato_crop")) {
			Item tomato = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("farmersdelight", "tomato"));
			Item seeds = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("farmersdelight", "tomato_seeds"));
			return new Crime(Laws.BREAKING_TOMATO, new ItemStack(tomato, 2), new ItemStack(seeds, 1));
		}
		return null;
	}
	
	public static boolean fromMod(Block block){
		String key = BuiltInRegistries.BLOCK.getKey(block).toString();
		return key.equals("farmersdelight:rich_soil") || 
		       key.equals("farmersdelight:rich_soil_farmland") || 
		       key.equals("farmersdelight:organic_compost");
	}
	
	public static Item equivalentItem(Block block){
		String key = BuiltInRegistries.BLOCK.getKey(block).toString();
		if (key.equals("farmersdelight:rich_soil") || key.equals("farmersdelight:rich_soil_farmland")) {
			return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("farmersdelight", "rich_soil"));
		} else if (key.equals("farmersdelight:organic_compost")) {
			return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("farmersdelight", "organic_compost"));
		}
		return null;
	}
	
	public static Law equivalentLaw(Block block){
		String key = BuiltInRegistries.BLOCK.getKey(block).toString();
		if (key.equals("farmersdelight:rich_soil") || key.equals("farmersdelight:rich_soil_farmland")) {
			return Laws.BREAKING_RICH_SOIL;
		} else if (key.equals("farmersdelight:organic_compost")) {
			return Laws.BREAKING_ORGANIC_COMPOST;
		}
		return null;
	}
}
