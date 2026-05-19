package com.stereowalker.obville.dat;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import com.stereowalker.obville.interfaces.IModdedEntity;
import com.stereowalker.obville.world.PlacedBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class VillageData {
	public Pair<BlockPos, BlockPos> bounds = Pair.of(new BlockPos(0, 0, 0), new BlockPos(0, 0, 0));
	public List<ItemStack> generatedBounties = new ArrayList<>();

	public static VillageData read(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
		VillageData village = new VillageData();
		village.bounds = Pair.of(
				new BlockPos(tag.getInt("X1"), tag.getInt("Y1"), tag.getInt("Z1")), 
				new BlockPos(tag.getInt("X2"), tag.getInt("Y2"), tag.getInt("Z2")));
		ListTag list1 = tag.getList("GeneratedBounties", 10);
		for (int i = 0; i < list1.size(); i++) {
			ItemStack.parse(registries, list1.getCompound(i)).ifPresent(village.generatedBounties::add);
		}
		return village;
	}

	public CompoundTag write(net.minecraft.core.HolderLookup.Provider registries) {
		CompoundTag tag = new CompoundTag();
		tag.putInt("X1", bounds.getLeft().getX());
		tag.putInt("Y1", bounds.getLeft().getY());
		tag.putInt("Z1", bounds.getLeft().getZ());
		tag.putInt("X2", bounds.getRight().getX());
		tag.putInt("Y2", bounds.getRight().getY());
		tag.putInt("Z2", bounds.getRight().getZ());

		ListTag listTag = new ListTag();
		generatedBounties.forEach(crime -> listTag.add(crime.save(registries, new CompoundTag())));
		tag.put("GeneratedBounties", listTag);

		return tag;
	}

	public static boolean isBounty(ItemStack stack) {
		net.minecraft.world.item.component.CustomData customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
		if (customData != null) {
			return customData.copyTag().getBoolean("is_bounty");
		}
		return false;
	}

	public static ItemStack bounty(ServerPlayer victim, int village) {
		ItemStack stack = new ItemStack(Items.PAPER);
		CompoundTag tag = new CompoundTag();
		tag.putBoolean("is_bounty", true);
		stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
		if (village == 0)
			stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Bounty On ")
					.append(victim.getDisplayName()));
		else
			stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal("Bounty On ")
					.append(victim.getDisplayName())
					.append(Component.literal(" In Village "+village)));
		return stack;
	}

	public static void invalidateGounty(ServerPlayer serverplayer) {
		PlacedBlocks pb = PlacedBlocks.getInstance(serverplayer.serverLevel());
		OVModData modEn = ((IModdedEntity)serverplayer).getData();
		if (!modEn.IsExiled() && modEn.reput().generatedBounty && modEn.reput().droppedBounty) {
			modEn.reput().generatedBounty = false;
			pb.villages.get(modEn.currentVillage()).generatedBounties.removeIf((stack) -> ItemStack.matches(stack, VillageData.bounty(serverplayer, modEn.currentVillage())));
		}
	}
}
