package com.stereowalker.obville.interfaces;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.stereowalker.obville.Crime;
import com.stereowalker.obville.ObVille;
import com.stereowalker.obville.world.effect.Effects;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.Vec3;

public interface IInvestigator {
	public BlockPos investigatePos();
	public Crime crimeToInvestigate();
	

	public void setInvestigatePos(BlockPos pos);
	public void crimeToInvestigate(Crime crime);
	
	/**
	 * Called when a panicking villager reaches this authority and reports a crime.
	 * This triggers the actual consequences (reputation hit, blacklist, guard follow).
	 */
	public default void onReportReceived(Mob self, Villager reporter, Player criminal, Crime crime) {
		if (criminal instanceof ServerPlayer serverPlayer) {
			// Apply the actual consequences now that the report has landed
			ObVille.upsetNearby(criminal, false, () -> crime);
			
			// Authority says a response line
			String responseLine = ObVille.LINES_CONFIG.authority_report_received.get(
				self.getRandom().nextInt(ObVille.LINES_CONFIG.authority_report_received.size()));
			Component cleanMessage = Component.literal(responseLine);
			Component message;
			if (self.getTags().contains("villagernames.named") && self.getCustomName() != null)
				message = self.getCustomName().copy().append(": ").append(cleanMessage);
			else
				message = self.getName().copy().append(": ").append(cleanMessage);
			new com.stereowalker.obville.network.protocol.game.ClientboundVillagerMessagePacket(
				message, criminal.getUUID(), self, cleanMessage).send(serverPlayer);
			
			// If this is a guard/leader with IPlayerFollower, start following the criminal
			if (self instanceof IPlayerFollower follower) {
				follower.follow(criminal);
			}
		}
	}
	
	/**
	 * Attack the specified entity using a ranged attack.
	 */
	public default void performRangedAttack(LivingEntity owner, BlockPos pos, float pDistanceFactor) {
		Vec3 vec3 = Vec3.ZERO;
		double d0 = pos.getX() + vec3.x - owner.getX();
		double d1 = pos.getY() - (double)1.1F - owner.getY();
		double d2 = pos.getZ() + vec3.z - owner.getZ();
		double d3 = Math.sqrt(d0 * d0 + d2 * d2);

		ThrownPotion thrownpotion = new ThrownPotion(owner.level(), owner);
		List<MobEffectInstance> effects = new ArrayList<>();
		effects.add(new MobEffectInstance(Effects.INVESTIGATE.holder()));
		
		ItemStack potionStack = new ItemStack(Items.LINGERING_POTION);
		potionStack.set(
			net.minecraft.core.component.DataComponents.POTION_CONTENTS,
			new PotionContents(Optional.empty(), Optional.empty(), effects)
		);
		thrownpotion.setItem(potionStack);
		
		thrownpotion.setXRot(thrownpotion.getXRot() - -20.0F);
		thrownpotion.shoot(d0, d1 + d3 * 0.2D, d2, 0.75F, 8.0F);
		if (!owner.isSilent()) {
			owner.level().playSound((Player)null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.WITCH_THROW, owner.getSoundSource(), 1.0F, 0.8F + owner.getRandom().nextFloat() * 0.4F);
		}

		owner.level().addFreshEntity(thrownpotion);
	}
}
