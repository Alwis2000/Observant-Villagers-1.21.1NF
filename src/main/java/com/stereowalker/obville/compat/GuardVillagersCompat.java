package com.stereowalker.obville.compat;

import java.util.List;
import java.util.function.Predicate;

import com.stereowalker.obville.Crime;
import com.stereowalker.obville.ObVille;
import com.stereowalker.obville.interfaces.IPlayerFollower;
import com.stereowalker.obville.interfaces.IVillager;
import com.stereowalker.obville.network.protocol.game.ClientboundVillagerMessagePacket;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import tallestegg.guardvillagers.common.entities.Guard;

public class GuardVillagersCompat {
	
	public static boolean isGuard(Entity entity){
		return entity instanceof Guard;
	}
	
	public static void tryToAnger(Player player, boolean angerOnlyIfCanSee, List<LivingEntity> angeredEntities, List<Villager> villagers) {
		Predicate<Guard> shouldAngerGuard = guard -> villagers.size() > 0 || !angerOnlyIfCanSee || ObVille.isLookingAtPlayer(guard, player);
		List<Guard> guards = player.level().getEntitiesOfClass(Guard.class, player.getBoundingBox().inflate(16.0));
		guards.stream().filter(shouldAngerGuard).forEach(guard -> {
			angeredEntities.add(guard);
		});
	}
	
	public static void wit(Player player, List<LivingEntity> angeredEntities, Crime crimeCommited) {
		if (angeredEntities.get(0) instanceof Guard guard && crimeCommited != null && crimeCommited.lawBroken.isPardonable()) {
			@SuppressWarnings("unchecked")
			IVillager<Guard> vg = (IVillager<Guard>)guard;
			if (!vg.crimesWitnessed().containsKey(player.getUUID()))
				vg.crimesWitnessed().put(player.getUUID(), 0);
			vg.crimesWitnessed().put(player.getUUID(), vg.crimesWitnessed().get(player.getUUID())+1);
			if (vg.crimesWitnessed().get(player.getUUID()) >= 3) {
				target(guard, player);
			} else {
				String cleanLine = ObVille.LINES_CONFIG.guardCaught.get(guard.getRandom().nextInt(ObVille.LINES_CONFIG.guardCaught.size()));
				Component cleanMessage = Component.literal(cleanLine);
				new ClientboundVillagerMessagePacket(vg.fromVillager(cleanMessage), player.getUUID(), guard, cleanMessage).send((ServerPlayer)player);
				if (guard instanceof IPlayerFollower follower && guard.getRandom().nextFloat() < ObVille.MOD_CONFIG.guardFollow) {
					follower.setFollowedCriminal(player);
				}
			}
		}
	}
	
	public static void target(LivingEntity liv, Player player) {
		if (liv instanceof Guard guard) {
			guard.setTarget(player);
		}
	}
	
	public static List<? extends LivingEntity> findNearbyGuards(LivingEntity searcher) {
		return searcher.level().getEntitiesOfClass(Guard.class, searcher.getBoundingBox().inflate(32.0));
	}
}
