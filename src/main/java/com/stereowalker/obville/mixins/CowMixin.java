package com.stereowalker.obville.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

@Mixin(Cow.class)
public abstract class CowMixin {

	@Overwrite
	protected void registerGoals() {
		Cow cow = (Cow) (Object) this;
		cow.goalSelector.addGoal(0, new FloatGoal(cow));
		cow.goalSelector.addGoal(1, new PanicGoal(cow, 2.0D));
		cow.goalSelector.addGoal(3, new BreedGoal(cow, 1.0D));
		cow.goalSelector.addGoal(4, new TemptGoal(cow, 1.25D, Ingredient.of(Items.WHEAT), false));
		cow.goalSelector.addGoal(5, new FollowParentGoal(cow, 1.25D));
		cow.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(cow, 1.0D));
		cow.goalSelector.addGoal(7, new LookAtPlayerGoal(cow, Player.class, 6.0F));
		cow.goalSelector.addGoal(8, new RandomLookAroundGoal(cow));
	}

}
