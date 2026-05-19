package com.stereowalker.obville.mixins;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.stereowalker.obville.interfaces.ILootableBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootTable;

@Mixin(RandomizableContainerBlockEntity.class)
public abstract class RandomizableMixin extends BaseContainerBlockEntity implements ILootableBlock, RandomizableContainer {

	protected RandomizableMixin(BlockEntityType<?> p_155076_, BlockPos p_155077_, BlockState p_155078_) {
		super(p_155076_, p_155077_, p_155078_);
	}

	@Shadow @Nullable protected ResourceKey<LootTable> lootTable;
	protected ResourceLocation lootTable1;
	protected List<UUID> playersOpened;

	@Override
	public ResourceLocation getLoot() {
		return lootTable1;
	}
	
	@Override
	public List<UUID> playersOpened() {
		return playersOpened;
	}
	
	@Override
	public void addPlayer(Player player) {
		if (playersOpened == null)
			playersOpened = new ArrayList<>();
		playersOpened.add(player.getUUID());
	}

	@Override
	public boolean tryLoadLootTable(CompoundTag pTag) {
		if (pTag.contains("ContainerLootTable", 8)) {				
			this.lootTable1 = ResourceLocation.parse(pTag.getString("ContainerLootTable"));
		}
		else if (pTag.contains("LootTable", 8)) {
			this.lootTable1 = ResourceLocation.parse(pTag.getString("LootTable"));
		}
		
		if (pTag.contains("PlayersOpened")) {		
			ListTag list = pTag.getList("PlayersOpened", 11);
			this.playersOpened = new ArrayList<>();
			for (int i = 0; i < list.size(); i++) {
				this.playersOpened.add(NbtUtils.loadUUID(list.get(i)));
			}
		}
		return RandomizableContainer.super.tryLoadLootTable(pTag);
	}

	@Override
	public boolean trySaveLootTable(CompoundTag pTag) {
		if (lootTable1 != null) {
			pTag.putString("ContainerLootTable", this.lootTable1.toString());
		}
		
		if (playersOpened != null && playersOpened.size() > 0) {
			ListTag list = new ListTag();
			playersOpened.forEach(player -> list.add(NbtUtils.createUUID(player)));
			pTag.put("PlayersOpened", list);
		}
		return RandomizableContainer.super.trySaveLootTable(pTag);
	}

	@Inject(method = "setLootTable(Lnet/minecraft/resources/ResourceKey;)V", at = @At("HEAD"))
	public void setLootTableInject(ResourceKey<LootTable> pLootTable, CallbackInfo ci) {
		if (pLootTable != null) {
			this.lootTable1 = pLootTable.location();
		}
	}

	@Override
	public void unpackLootTable(@Nullable Player pPlayer) {
		if (this.lootTable != null && this.level.getServer() != null) {
			System.out.println("Generating "+lootTable);
			System.out.println("Saving "+lootTable);
			this.lootTable1 = lootTable.location();
		}
		RandomizableContainer.super.unpackLootTable(pPlayer);
	}

}