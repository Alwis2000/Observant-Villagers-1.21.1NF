package com.stereowalker.obville.compat;

import com.stereowalker.obville.Law;
import com.stereowalker.obville.Laws;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class QuarkCompat {
	public static boolean isTable(Block block){
		ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
		return key.getNamespace().equals("quark") && key.getPath().contains("bookshelf");
	}
	
	public static Item equivalentItem(Block block){
		ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
		if (key.getNamespace().equals("quark") && key.getPath().contains("bookshelf")) {
			return BuiltInRegistries.ITEM.get(key);
		}
		return net.minecraft.world.item.Items.AIR;
	}
	
	public static Law equivalentLaw(Block block){
		return Laws.BREAKING_QUARK_BOOKSHELVES;
	}
}
