package com.stereowalker.obville.mixins;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.stereowalker.obville.Crime;
import com.stereowalker.obville.Law;
import com.stereowalker.obville.Laws;
import com.stereowalker.obville.ObVille;
import com.stereowalker.obville.dat.VillageData;
import com.stereowalker.obville.interfaces.IModdedEntity;
import com.stereowalker.obville.interfaces.ISheep;
import com.stereowalker.obville.interfaces.IVillager;
import com.stereowalker.obville.network.protocol.game.ClientboundSoundPacket;
import com.stereowalker.obville.network.protocol.game.ClientboundVillagerMessagePacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Tuple;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;

@Mixin(Villager.class)
public abstract class VillagerMixin implements VillagerDataHolder, IVillager<Villager>, ISheep {
	@Shadow private Player lastTradedPlayer;
	@Shadow private void setUnhappy() {}
	@Shadow private boolean shouldIncreaseLevel() {return false;}
	@Shadow public abstract net.minecraft.world.entity.npc.VillagerData getVillagerData();

	private Map<UUID, Integer> blacklisted = new HashMap<>();
	private List<UUID> untrustworthy = new ArrayList<>();
	private List<UUID> playersSaidLineTo = new ArrayList<>();
	private List<UUID> playersSaidUntrustworthyLineTo = new ArrayList<>();
	private List<UUID> playersSaidRecoverLineTo = new ArrayList<>();
	private Map<UUID, Tuple<Law, Integer>> recentlyWitnessedCrime = new HashMap<>();
	private Map<UUID, Integer> trustTimer = new HashMap<>();
	private Map<UUID, Integer> recentlyTakenBribe = new HashMap<>();
	private String tradesWithDistrustedPlayer = "";
	private boolean decidedOnTradingWithDistrusted = false;
	private boolean hasRewardedCustomer = false;
	private int affectedByWeary = 0;
	private Map<UUID, Integer> talkBackCooldown = new HashMap<>();

	@Inject(method = "tick", at = @At("TAIL"))
	private void ticker(CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		if (villager.level().isClientSide()) {
			return;
		}

		List<UUID> timersToRemove1 = new ArrayList<>();
		List<UUID> timersToRemove2 = new ArrayList<>();
		List<UUID> timersToRemove3 = new ArrayList<>();

		trustTimer.replaceAll((key,value) -> {
			if (value <= 0) {
				timersToRemove1.add(key);
				untrustworthy.add(key);
			}
			return value-1;
		});
		blacklisted.replaceAll((key,value) -> {
			if (value <= 0) timersToRemove2.add(key);
			return value-1;
		});

		recentlyTakenBribe.replaceAll((key,value) -> {
			if (value <= 0) timersToRemove3.add(key);
			return value-1;
		});

		if (invi > 0) invi--;

		if (this.talkBackCooldown == null) {
			this.talkBackCooldown = new HashMap<>();
		}
		List<UUID> cooldownsToRemove = new ArrayList<>();
		this.talkBackCooldown.replaceAll((key, value) -> {
			if (value <= 1) {
				cooldownsToRemove.add(key);
				this.playersSaidLineTo.remove(key);
				this.playersSaidUntrustworthyLineTo.remove(key);
				this.playersSaidRecoverLineTo.remove(key);
			}
			return value - 1;
		});
		cooldownsToRemove.forEach(this.talkBackCooldown::remove);

		timersToRemove1.forEach(trustTimer::remove);
		timersToRemove2.forEach(blacklisted::remove);
		timersToRemove3.forEach(recentlyTakenBribe::remove);
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	public void readAdditionalSaveDataInject(CompoundTag pCompound, CallbackInfo ci) {
		ListTag list1 = pCompound.getList("BlacklistedTimer", 10);
		this.trustTimer = new HashMap<>();
		for (int i = 0; i < list1.size(); i++) {
			CompoundTag tag = list1.getCompound(i);
			this.trustTimer.put(NbtUtils.loadUUID(tag.get("UUID")), tag.getInt("Timer"));
		}
		
		this.playersSaidLineTo = new ArrayList<>();
		pCompound.getList("SaidLineTo", 11).forEach(nbt -> this.playersSaidLineTo.add(NbtUtils.loadUUID(nbt)));
		
		this.playersSaidRecoverLineTo = new ArrayList<>();
		pCompound.getList("RecoverSaidLineTo", 11).forEach(nbt -> this.playersSaidRecoverLineTo.add(NbtUtils.loadUUID(nbt)));
		
		this.playersSaidUntrustworthyLineTo = new ArrayList<>();
		pCompound.getList("UntrustworthySaidLineTo", 11).forEach(nbt -> this.playersSaidUntrustworthyLineTo.add(NbtUtils.loadUUID(nbt)));
		
		ListTag list3 = pCompound.getList("WitnessedCrimes", 10);
		this.recentlyWitnessedCrime = new HashMap<>();
		for (int i = 0; i < list3.size(); i++) {
			CompoundTag tag = list3.getCompound(i);
			this.recentlyWitnessedCrime.put(NbtUtils.loadUUID(tag.get("UUID")), new Tuple<Law, Integer>(Laws.lawsToUphold.get(tag.getString("CrimeCommited")), tag.getInt("TimeSince")));
		}
		
		ListTag list2 = pCompound.getList("TrustTimers", 10);
		this.trustTimer = new HashMap<>();
		for (int i = 0; i < list2.size(); i++) {
			CompoundTag tag = list2.getCompound(i);
			this.trustTimer.put(NbtUtils.loadUUID(tag.get("UUID")), tag.getInt("Timer"));
		}
		
		ListTag list4 = pCompound.getList("Untrustworthy", 8);
		this.untrustworthy = new ArrayList<>();
		for (int i = 0; i < list4.size(); i++) {
			this.untrustworthy.add(NbtUtils.loadUUID(list4.get(i)));
		}
		
		ListTag list5 = pCompound.getList("RecentlyTakenBribe", 10);
		this.recentlyTakenBribe = new HashMap<>();
		for (int i = 0; i < list5.size(); i++) {
			CompoundTag tag = list5.getCompound(i);
			this.recentlyTakenBribe.put(NbtUtils.loadUUID(tag.get("UUID")), tag.getInt("Timer"));
		}
		this.decidedOnTradingWithDistrusted = pCompound.getBoolean("DecidedOnTradingWithDistrusted");
		this.tradesWithDistrustedPlayer = pCompound.getString("TradesWithDistrustedPlayer");
		this.hasRewardedCustomer = pCompound.getBoolean("HasRewardedCustomer");
		this.affectedByWeary = pCompound.getInt("AffectedByWeary");
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	public void addAdditionalSaveDataInject(CompoundTag pCompound, CallbackInfo ci) {
		ListTag list = new ListTag();
		trustTimer.forEach((player, timer) -> {
			CompoundTag tag = new CompoundTag();
			tag.put("UUID", NbtUtils.createUUID(player));
			tag.putInt("Timer", timer);
			list.add(tag);
		});
		pCompound.put("BlacklistedTimer", list);

		ListTag list1 = new ListTag();
		playersSaidLineTo.forEach(uuid -> {
			list1.add(NbtUtils.createUUID(uuid));
		});
		pCompound.put("SaidLineTo", list1);

		ListTag list12 = new ListTag();
		playersSaidRecoverLineTo.forEach(uuid -> {
			list12.add(NbtUtils.createUUID(uuid));
		});
		pCompound.put("RecoverSaidLineTo", list12);

		ListTag list13 = new ListTag();
		playersSaidUntrustworthyLineTo.forEach(uuid -> {
			list13.add(NbtUtils.createUUID(uuid));
		});
		pCompound.put("UntrustworthySaidLineTo", list13);

		ListTag list3 = new ListTag();
		recentlyWitnessedCrime.forEach((player, crime) -> {
			CompoundTag tag = new CompoundTag();
			tag.put("UUID", NbtUtils.createUUID(player));
			tag.putString("CrimeCommited", crime.getA().crimeIdentifier);
			tag.putInt("TimeSince", crime.getB());
			list3.add(tag);
		});
		pCompound.put("WitnessedCrimes", list3);

		ListTag list2 = new ListTag();
		trustTimer.forEach((player, timer) -> {
			CompoundTag tag = new CompoundTag();
			tag.put("UUID", NbtUtils.createUUID(player));
			tag.putInt("Timer", timer);
			list2.add(tag);
		});
		pCompound.put("TrustTimers", list2);

		ListTag list4 = new ListTag();
		untrustworthy.forEach(uuid -> {
			list4.add(NbtUtils.createUUID(uuid));
		});
		pCompound.put("Untrustworthy", list4);

		ListTag list6 = new ListTag();
		recentlyTakenBribe.forEach((player, timer) -> {
			CompoundTag tag = new CompoundTag();
			tag.put("UUID", NbtUtils.createUUID(player));
			tag.putInt("Timer", timer);
			list6.add(tag);
		});
		pCompound.put("RecentlyTakenBribe", list6);

		pCompound.putBoolean("DecidedOnTradingWithDistrusted", this.decidedOnTradingWithDistrusted);
		pCompound.putString("TradesWithDistrustedPlayer", this.tradesWithDistrustedPlayer);
		pCompound.putBoolean("HasRewardedCustomer", this.hasRewardedCustomer);
		pCompound.putInt("AffectedByWeary", this.affectedByWeary);
	}

	@Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
	public void mobInteractInject(Player pPlayer, InteractionHand pHand, CallbackInfoReturnable<InteractionResult> cir) {
		Villager villager = (Villager) (Object) this;
		if (!villager.isBaby()) {
			IModdedEntity modded = (IModdedEntity)pPlayer;
			switch (acceptBribe(pPlayer, pPlayer.getItemInHand(pHand), villager.getRandom())) {
			case Accepted:
				if (!villager.level().isClientSide()) {
					blacklisted.remove(pPlayer.getUUID());
					String cleanLine = ObVille.LINES_CONFIG.rare_bribe_success.get(villager.getRandom().nextInt(ObVille.LINES_CONFIG.rare_bribe_success.size()));
					Component cleanMessage = Component.literal(cleanLine);
					new ClientboundVillagerMessagePacket(fromVillager(cleanMessage), pPlayer.getUUID(), villager, cleanMessage).send((ServerPlayer)pPlayer);
				}
				cir.setReturnValue(InteractionResult.sidedSuccess(villager.level().isClientSide()));
				break;
			case Rejected:
				if (!villager.level().isClientSide()) {
					String cleanLine = ObVille.LINES_CONFIG.common_bribe_fail.get(villager.getRandom().nextInt(ObVille.LINES_CONFIG.common_bribe_fail.size()));
					Component cleanMessage = Component.literal(cleanLine);
					new ClientboundVillagerMessagePacket(fromVillager(cleanMessage), pPlayer.getUUID(), villager, cleanMessage).send((ServerPlayer)pPlayer);
				}
				setUnhappy();
				cir.setReturnValue(InteractionResult.sidedSuccess(villager.level().isClientSide()));
				break;
			default:
				if (!villager.level().isClientSide() && modded.getData().IsWeary())
					villager.level().broadcastEntityEvent(villager, (byte)13); //Does the angry particles
				if (modded.getData().IsDistrusted()) {
					if (!this.decidedOnTradingWithDistrusted) {
						if (villager.getRandom().nextInt(2) == 0) {
							this.tradesWithDistrustedPlayer = ObVille.LINES_CONFIG.distrusted_lines.get(villager.getRandom().nextInt(ObVille.LINES_CONFIG.distrusted_lines.size()));
						}
						this.decidedOnTradingWithDistrusted = true;
					}
					if (trustTimer.containsKey(pPlayer.getUUID())) {
						trustTimer.remove(pPlayer.getUUID());
						untrustworthy.add(pPlayer.getUUID());
					}
					if (playersSaidRecoverLineTo.contains(pPlayer.getUUID())) {
						playersSaidRecoverLineTo.remove(pPlayer.getUUID());
					}
					if (tradesWithDistrustedPlayer.length() > 0 || untrustworthy.contains(pPlayer.getUUID())) {
						if (!villager.level().isClientSide()) {
							if (untrustworthy.contains(pPlayer.getUUID())) {
								boolean shouldSend = !ObVille.MOD_CONFIG.one_liners || !playersSaidUntrustworthyLineTo.contains(pPlayer.getUUID());
								if (shouldSend) {
									String cleanLine = ObVille.LINES_CONFIG.distrustedAgain.get(villager.getRandom().nextInt(ObVille.LINES_CONFIG.distrustedAgain.size()));
									Component cleanMessage = Component.literal(cleanLine);
									new ClientboundVillagerMessagePacket(fromVillager(cleanMessage), pPlayer.getUUID(), villager, cleanMessage).send((ServerPlayer)pPlayer);
									playersSaidUntrustworthyLineTo.add(pPlayer.getUUID());
									if (this.talkBackCooldown == null) this.talkBackCooldown = new HashMap<>();
									this.talkBackCooldown.put(pPlayer.getUUID(), 100);
								}
							}
							else {
								if (!ObVille.MOD_CONFIG.one_liners) {
									String cleanLine = ObVille.LINES_CONFIG.distrusted_lines.get(villager.getRandom().nextInt(ObVille.LINES_CONFIG.distrusted_lines.size()));
									Component cleanMessage = Component.literal(cleanLine);
									new ClientboundVillagerMessagePacket(fromVillager(cleanMessage), pPlayer.getUUID(), villager, cleanMessage).send((ServerPlayer)pPlayer);
								}
								else if (!playersSaidLineTo.contains(pPlayer.getUUID())) {
									Component cleanMessage = Component.literal(tradesWithDistrustedPlayer);
									Component message = cleanMessage;
									if (villager.getTags().contains("villagernames.named"))
										message = villager.getCustomName().copy().append(": ").append(cleanMessage);
									else
										message = villager.getName().copy().append(": ").append(cleanMessage);
									new ClientboundVillagerMessagePacket(message, pPlayer.getUUID(), villager, cleanMessage).send((ServerPlayer)pPlayer);
									playersSaidLineTo.add(pPlayer.getUUID());
									if (this.talkBackCooldown == null) this.talkBackCooldown = new HashMap<>();
									this.talkBackCooldown.put(pPlayer.getUUID(), 100);
								}
							}
							setUnhappy();
						}
						cir.setReturnValue(InteractionResult.sidedSuccess(villager.level().isClientSide()));
					}
				}
				else if (modded.getData().IsExiled() || blacklisted.containsKey(pPlayer.getUUID())) {
					if (!villager.level().isClientSide()) {
						setUnhappy();
						if (blacklisted.containsKey(pPlayer.getUUID())) {
							String cleanLine = ObVille.LINES_CONFIG.blacklisted.get(villager.getRandom().nextInt(ObVille.LINES_CONFIG.blacklisted.size()));
							Component cleanMessage = Component.literal(cleanLine);
							new ClientboundVillagerMessagePacket(fromVillager(cleanMessage), pPlayer.getUUID(), villager, cleanMessage).send((ServerPlayer)pPlayer);
						}
					}
					cir.setReturnValue(InteractionResult.sidedSuccess(villager.level().isClientSide()));
				}
				else if (!modded.getData().IsExiled()) {
					if (playersSaidLineTo.contains(pPlayer.getUUID())) {
						trustTimer.put(pPlayer.getUUID(), ObVille.MOD_CONFIG.recovery);
						playersSaidLineTo.remove(pPlayer.getUUID());
					}
					if (playersSaidUntrustworthyLineTo.contains(pPlayer.getUUID())) {
						trustTimer.put(pPlayer.getUUID(), ObVille.MOD_CONFIG.recovery);
						playersSaidUntrustworthyLineTo.remove(pPlayer.getUUID());
					}

					if (trustTimer.containsKey(pPlayer.getUUID())) {
						if (!villager.level().isClientSide()) {
							if (!ObVille.MOD_CONFIG.one_liners || !playersSaidRecoverLineTo.contains(pPlayer.getUUID())) {
								String cleanLine = ObVille.LINES_CONFIG.recoverFromDistrusted.get(villager.getRandom().nextInt(ObVille.LINES_CONFIG.recoverFromDistrusted.size()));
								Component cleanMessage = Component.literal(cleanLine);
								new ClientboundVillagerMessagePacket(fromVillager(cleanMessage), pPlayer.getUUID(), villager, cleanMessage).send((ServerPlayer)pPlayer);
								playersSaidRecoverLineTo.add(pPlayer.getUUID());
								if (this.talkBackCooldown == null) this.talkBackCooldown = new HashMap<>();
								this.talkBackCooldown.put(pPlayer.getUUID(), 100);
							}
						}
						cir.setReturnValue(InteractionResult.sidedSuccess(villager.level().isClientSide()));
					} else {
						if (playersSaidRecoverLineTo.contains(pPlayer.getUUID())) {
							playersSaidRecoverLineTo.remove(pPlayer.getUUID());
						}
					}
				} else {
					if (playersSaidRecoverLineTo.contains(pPlayer.getUUID())) {
						playersSaidRecoverLineTo.remove(pPlayer.getUUID());
					}
				}
				if (modded.getData().IsWelcome() && blacklisted.containsKey(pPlayer.getUUID())) {
					blacklisted.remove(pPlayer.getUUID());
				}	
				break;
			}
		}
	}


	@Inject(method = "rewardTradeXp", at = @At("TAIL"))
	public void rewardTradeXpInject(CallbackInfo ci) {
		System.out.println(getVillagerData().getLevel()+" "+(lastTradedPlayer == null));
		if (!hasRewardedCustomer && getVillagerData().getLevel() == 4 && shouldIncreaseLevel() && lastTradedPlayer instanceof IModdedEntity player) {
			new ClientboundSoundPacket(true, lastTradedPlayer.getUUID()).send((ServerPlayer) lastTradedPlayer);
			player.getData().incrementReputation(ObVille.REPUTATION_CONFIG.max_trade);
			VillageData.invalidateGounty((ServerPlayer)lastTradedPlayer);
			hasRewardedCustomer = true;
		}
	}

	@Inject(method = "customServerAiStep", at = @At("TAIL"))
	public void customServerAiStepInject(CallbackInfo ci) {
		Map<UUID, Tuple<Law, Integer>> newMap = new HashMap<>();
		recentlyWitnessedCrime.forEach((uuid, tup) -> {
			if (tup.getB()+1 < ObVille.MOD_CONFIG.bribe_window)
				newMap.put(uuid, new Tuple<Law, Integer>(tup.getA(), tup.getB()+1));
		});
		recentlyWitnessedCrime = newMap;
	}

	@Inject(method = "updateSpecialPrices", at = @At("TAIL"))
	public void updateSpecialPricesInject(Player pPlayer, CallbackInfo ci) {
		Villager villager = (Villager) (Object) this;
		IModdedEntity modded = (IModdedEntity)pPlayer;
		if (this.affectedByWeary == 0)
			this.affectedByWeary = villager.getRandom().nextInt(9)+1;

		boolean flag = modded.getData().IsWeary() && this.affectedByWeary >= 5;
		if (modded.getData().IsDistrusted() && decidedOnTradingWithDistrusted && tradesWithDistrustedPlayer.length() == 0)
			flag = true;
		if (flag) {
			for(MerchantOffer merchantoffer1 : villager.getOffers()) {
				int k = -10;
				double d0 = 0.3D + 0.0625D * (double)k;
				int j = (int)Math.floor(d0 * (double)merchantoffer1.getBaseCostA().getCount());
				merchantoffer1.addToSpecialPriceDiff(-Math.min(j, 1));
			}
		}
		else if (modded.getData().IsWelcome() && !pPlayer.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
			for(MerchantOffer merchantoffer1 : villager.getOffers()) {
				int k = 10;
				double d0 = 0.3D + 0.0625D * (double)k;
				int j = (int)Math.floor(d0 * (double)merchantoffer1.getBaseCostA().getCount());
				merchantoffer1.addToSpecialPriceDiff(-Math.max(j, 1));
			}
		}
	}

	@Override
	public void blacklist(ServerPlayer player) {
		Villager villager = (Villager) (Object) this;
		if (!blacklisted.containsKey(player.getUUID())) {
			blacklisted.put(player.getUUID(), ObVille.MOD_CONFIG.blacklisted);
		}
		villager.level().broadcastEntityEvent(villager, (byte)13);
	}

	@Override
	public Map<UUID, Tuple<Law, Integer>> recentlyWitnessedCrime() {
		return recentlyWitnessedCrime;
	}

	@Override
	public Map<UUID, Integer> recentlyTakenBribe() {
		return recentlyTakenBribe;
	}

	@Override
	public void witnessCrime(Player player, Crime crime) {
		recentlyWitnessedCrime.put(player.getUUID(), new Tuple<Law, Integer>(crime.lawBroken, 0));
	}

	int invi = 0;
	@Override
	public int invisibleLineCooldown() {
		return invi;
	}

	@Override
	public void setInvisibleLineCooldown(int v) {
		invi = v;
	}

	@Override
	public Villager villager() {
		return (Villager)(Object)this;
	}

	@Override
	public Villager me() {
		return (Villager)(Object)this;
	}
}
