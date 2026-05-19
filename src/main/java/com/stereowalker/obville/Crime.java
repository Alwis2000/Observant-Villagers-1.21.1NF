package com.stereowalker.obville;

import java.util.Optional;
import com.stereowalker.obville.dat.OVModData;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

public class Crime {
	public Law lawBroken;
	public ItemStack reparation1, reparation2;
	
	public Crime(Law lawBroken, ItemStack reparation1, ItemStack reparation2) {
		this.lawBroken = lawBroken;
		this.reparation1 = reparation1;
		this.reparation2 = reparation2;
	}
	
	public Crime(Law lawBroken, ItemStack reparation1) {
		this(lawBroken, reparation1, ItemStack.EMPTY);
	}
	
	public Crime(Law lawBroken, Item reparation) {
		this(lawBroken, new ItemStack(reparation, 2));
	}
	
	public static Crime read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
		return new Crime(Laws.lawsToUphold.get(tag.getString("lawBroken")), 
				tag.contains("reparation1") ? ItemStack.parse(registries, tag.getCompound("reparation1")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY, 
				tag.contains("reparation2") ? ItemStack.parse(registries, tag.getCompound("reparation2")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY);
	}
	
	public CompoundTag write(net.minecraft.core.HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		if (this.lawBroken != null) tag.putString("lawBroken", lawBroken.crimeIdentifier);
		if (!reparation1.isEmpty()) {
			tag.put("reparation1", reparation1.save(registries, new CompoundTag()));
		}
		if (!reparation2.isEmpty()) {
			tag.put("reparation2", reparation2.save(registries, new CompoundTag()));
		}
		return tag;
	}

	public static ItemStack forgive(int amount, String forWhat, int whichVillage) {
		ItemStack stack = new ItemStack(Items.PAPER);
		stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.translatable("crime."+forWhat));
		CompoundTag give = new CompoundTag();
		give.putInt("amount", amount);
		give.putString("forWhat", forWhat);
		give.putInt("whichVillage", whichVillage);
		CompoundTag customTag = new CompoundTag();
		customTag.put("obville:to_forgive", give);
		stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(customTag));
		return stack;
	}
	
	public MerchantOffer frogive(OVModData data, int village) {
		Optional<ItemCost> costB = reparation2.isEmpty() ? Optional.empty() : Optional.of(new ItemCost(reparation2.getItem(), reparation2.getCount()));
		return new MerchantOffer(
				new ItemCost(reparation1.getItem(), reparation1.getCount()),
				costB,
				forgive(-lawBroken.getRepHit(), lawBroken.crimeIdentifier, village),
				data.crimesCommitedOfType(village, lawBroken),
				0, // xp
				1.0F // priceMultiplier
		);
	}
}
