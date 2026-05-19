package com.stereowalker.obville.mixins;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.entity.BellBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(BellBlockEntity.class)
public interface BellBlockEntityAccessor {
	@Accessor("nearbyEntities")
	List<LivingEntity> getNearbyEntities();

	@Invoker("areRaidersNearby")
	static boolean callAreRaidersNearby(BlockPos pos, List<LivingEntity> entities) {
		throw new UnsupportedOperationException();
	}
}
