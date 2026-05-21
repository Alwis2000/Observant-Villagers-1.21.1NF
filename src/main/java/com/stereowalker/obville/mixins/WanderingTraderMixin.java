package com.stereowalker.obville.mixins;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.stereowalker.obville.ObVille;
import com.stereowalker.obville.dat.OVModData;
import com.stereowalker.obville.interfaces.IModdedEntity;
import com.stereowalker.obville.interfaces.IVillager;
import com.stereowalker.obville.network.protocol.game.ClientboundVillagerMessagePacket;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin implements com.stereowalker.obville.interfaces.IWanderingTrader {

	@Unique
	private final Map<UUID, Integer> obville$rumoredReputations = new HashMap<>();

	@Unique
	private int obville$gossipCooldown = 0;

	@Override
	public void obville$tickTrader() {
		WanderingTrader trader = (WanderingTrader) (Object) this;
		if (trader.level().isClientSide()) {
			return;
		}

		if (obville$gossipCooldown > 0) {
			obville$gossipCooldown--;
		}

		if (trader.tickCount % 100 == 0) {
			ServerLevel serverLevel = (ServerLevel) trader.level();
			int currentVillage = ObVille.determineVillage(serverLevel, trader.blockPosition());
			System.out.println("[ObVille Debug] Wandering Trader ticked. Village ID: " + currentVillage + ", Rumored Reputations: " + obville$rumoredReputations);
			
			// 1. Gather rumors from players in the village who have bad reputation
			if (currentVillage >= 0) {
				List<Player> nearbyPlayers = serverLevel.getEntitiesOfClass(Player.class, trader.getBoundingBox().inflate(24.0));
				for (Player player : nearbyPlayers) {
					OVModData data = ((IModdedEntity) player).getData();
					if (data.isInAnyVillage() && data.currentVillage() == currentVillage) {
						int rep = data.getReputation();
						if (rep <= -4) {
							int existing = obville$rumoredReputations.getOrDefault(player.getUUID(), 0);
							if (rep < existing) {
								obville$rumoredReputations.put(player.getUUID(), rep);
								System.out.println("[ObVille Debug] Wandering Trader gathered rumor: Player " + player.getName().getString() + " has reputation " + rep + " in Village ID " + currentVillage);
							}
						}
					}
				}
			}

			// 2. Spread rumors to other villagers in different villages
			if (currentVillage >= 0 && obville$gossipCooldown <= 0 && !obville$rumoredReputations.isEmpty() && ObVille.MOD_CONFIG.traders_spread_rumors) {
				List<Villager> nearbyVillagers = serverLevel.getEntitiesOfClass(Villager.class, trader.getBoundingBox().inflate(8.0));
				if (!nearbyVillagers.isEmpty()) {
					Villager localVillager = nearbyVillagers.get(trader.getRandom().nextInt(nearbyVillagers.size()));
					for (Map.Entry<UUID, Integer> entry : obville$rumoredReputations.entrySet()) {
						UUID playerUUID = entry.getKey();
						int rumoredRep = entry.getValue();
						
						ServerPlayer serverPlayer = serverLevel.getServer().getPlayerList().getPlayer(playerUUID);
						if (serverPlayer != null) {
							OVModData data = ((IModdedEntity) serverPlayer).getData();
							int localRep = data.getReputationIn(currentVillage);
							
							// If the player has better reputation in this village than the rumor, we spread it
							if (localRep > rumoredRep) {
								int penalty = ObVille.MOD_CONFIG.rumor_reputation_decrease;
								int newRep = Math.max(rumoredRep, localRep - penalty);
								data.setReputation(currentVillage, newRep);
								System.out.println("[ObVille Debug] Wandering Trader spread rumor to Villager " + localVillager.getName().getString() + " in Village ID " + currentVillage + ": Player reputation reduced to " + newRep + " (rumored was " + rumoredRep + ")");
								
								// Propagate untrustworthiness and blacklisted status
								IVillager<?> localVillagerCap = (IVillager<?>) localVillager;
								if (rumoredRep <= -8) {
									if (!localVillagerCap.obville$getUntrustworthy().contains(playerUUID)) {
										localVillagerCap.obville$getUntrustworthy().add(playerUUID);
										System.out.println("[ObVille Debug] Villager " + localVillager.getName().getString() + " marked Player " + playerUUID + " as untrustworthy due to trader rumor.");
									}
								}
								if (rumoredRep <= -14) {
									if (!localVillagerCap.obville$getBlacklisted().containsKey(playerUUID)) {
										localVillagerCap.obville$getBlacklisted().put(playerUUID, ObVille.MOD_CONFIG.blacklisted);
										System.out.println("[ObVille Debug] Villager " + localVillager.getName().getString() + " blacklisted Player " + playerUUID + " due to trader rumor.");
									}
								}
								
								// Spawn angry particles
								serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, trader.getX(), trader.getY() + 1.5D, trader.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.0D);
								serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER, localVillager.getX(), localVillager.getY() + 1.5D, localVillager.getZ(), 5, 0.2D, 0.2D, 0.2D, 0.0D);
								
								// Play sounds
								trader.level().playSound(null, trader.getX(), trader.getY(), trader.getZ(), net.minecraft.sounds.SoundEvents.WANDERING_TRADER_NO, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
								localVillager.level().playSound(null, localVillager.getX(), localVillager.getY(), localVillager.getZ(), net.minecraft.sounds.SoundEvents.VILLAGER_NO, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
								
								// Send dialogues/messages
								String wtLine = ObVille.LINES_CONFIG.wandering_trader_gossip.get(trader.getRandom().nextInt(ObVille.LINES_CONFIG.wandering_trader_gossip.size()));
								wtLine = String.format(wtLine, serverPlayer.getName().getString());
								String vLine = ObVille.LINES_CONFIG.villager_gossip_reaction.get(trader.getRandom().nextInt(ObVille.LINES_CONFIG.villager_gossip_reaction.size()));
								
								Component cleanWtMessage = Component.literal(wtLine);
								Component cleanVMessage = Component.literal(vLine);
								
								Component wtMessage = trader.getName().copy().append(": ").append(cleanWtMessage);
								Component vMessage = localVillager.getName().copy().append(": ").append(cleanVMessage);
								
								new ClientboundVillagerMessagePacket(wtMessage, serverPlayer.getUUID(), trader, cleanWtMessage).send(serverPlayer);
								new ClientboundVillagerMessagePacket(vMessage, serverPlayer.getUUID(), localVillager, cleanVMessage).send(serverPlayer);
								
								obville$gossipCooldown = ObVille.MOD_CONFIG.rumor_gossip_cooldown;
								break; // Spread one rumor at a time
							}
						}
					}
				}
			}
		}
	}

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	private void mobInteractInject(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
		WanderingTrader trader = (WanderingTrader) (Object) this;
		if (trader.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return;
		}

		// 1. Refuse trades if trader is carrying a bad rumor
		if (ObVille.MOD_CONFIG.traders_refuse_trades) {
			int rep = obville$rumoredReputations.getOrDefault(player.getUUID(), 0);
			if (rep <= -8) {
				trader.level().broadcastEntityEvent(trader, (byte) 13); // angry villager particles
				trader.level().playSound(null, trader.getX(), trader.getY(), trader.getZ(), net.minecraft.sounds.SoundEvents.WANDERING_TRADER_NO, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
				
				String line = ObVille.LINES_CONFIG.wandering_trader_refuse.get(trader.getRandom().nextInt(ObVille.LINES_CONFIG.wandering_trader_refuse.size()));
				Component cleanMessage = Component.literal(line);
				Component message = trader.getName().copy().append(": ").append(cleanMessage);
				new ClientboundVillagerMessagePacket(message, player.getUUID(), trader, cleanMessage).send(serverPlayer);
				
				cir.setReturnValue(InteractionResult.sidedSuccess(trader.level().isClientSide()));
				cir.cancel();
				return;
			}
		}

		// 2. Adjust prices depending on player status/rumors
		int currentVillage = ObVille.determineVillage((ServerLevel) trader.level(), trader.blockPosition());
		int repVal = obville$rumoredReputations.getOrDefault(player.getUUID(), 0);
		
		// Reset all prices to base cost first
		for (MerchantOffer offer : trader.getOffers()) {
			offer.setSpecialPriceDiff(0);
		}

		OVModData data = ((IModdedEntity) player).getData();
		if (repVal <= -4) {
			// Increase prices
			for (MerchantOffer offer : trader.getOffers()) {
				int k = -10;
				double d0 = 0.3D + 0.0625D * (double)k;
				int j = (int)Math.floor(d0 * (double)offer.getBaseCostA().getCount());
				offer.addToSpecialPriceDiff(-Math.min(j, 1));
			}
		} else if (currentVillage >= 0 && data.IsWelcomeAt(currentVillage)) {
			// Decrease prices
			for (MerchantOffer offer : trader.getOffers()) {
				int k = 10;
				double d0 = 0.3D + 0.0625D * (double)k;
				int j = (int)Math.floor(d0 * (double)offer.getBaseCostA().getCount());
				offer.addToSpecialPriceDiff(-Math.max(j, 1));
			}
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readAdditionalSaveDataInject(CompoundTag pCompound, CallbackInfo ci) {
		this.obville$rumoredReputations.clear();
		ListTag list = pCompound.getList("ObvilleRumors", 10);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag tag = list.getCompound(i);
			if (tag.contains("UUID") && tag.contains("Reputation")) {
				this.obville$rumoredReputations.put(NbtUtils.loadUUID(tag.get("UUID")), tag.getInt("Reputation"));
			}
		}
		this.obville$gossipCooldown = pCompound.getInt("ObvilleGossipCooldown");
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void addAdditionalSaveDataInject(CompoundTag pCompound, CallbackInfo ci) {
		ListTag list = new ListTag();
		this.obville$rumoredReputations.forEach((uuid, rep) -> {
			CompoundTag tag = new CompoundTag();
			tag.put("UUID", NbtUtils.createUUID(uuid));
			tag.putInt("Reputation", rep);
			list.add(tag);
		});
		pCompound.put("ObvilleRumors", list);
		pCompound.putInt("ObvilleGossipCooldown", this.obville$gossipCooldown);
	}
}
