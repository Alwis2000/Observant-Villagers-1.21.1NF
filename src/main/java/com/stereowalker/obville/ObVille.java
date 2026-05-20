package com.stereowalker.obville;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.stereowalker.obville.client.renderer.entity.VillageChiefModel;
import com.stereowalker.obville.client.renderer.entity.VillagerChiefRenderer;
import com.stereowalker.obville.compat.GuardVillagersCompat;
import com.stereowalker.obville.config.ClientConfig;
import com.stereowalker.obville.config.ExtraLinesConfig;
import com.stereowalker.obville.config.ModConfig;
import com.stereowalker.obville.config.ReputationAmountConfig;
import com.stereowalker.obville.dat.OVModData;
import com.stereowalker.obville.dat.VillageData;
import com.stereowalker.obville.interfaces.IInvestigator;
import com.stereowalker.obville.interfaces.ILootableBlock;
import com.stereowalker.obville.interfaces.IModdedEntity;
import com.stereowalker.obville.interfaces.IVillager;
import com.stereowalker.obville.network.protocol.game.ClientboundOverlayOverridePacket;
import com.stereowalker.obville.network.protocol.game.ClientboundSoundPacket;
import com.stereowalker.obville.network.protocol.game.ClientboundNBTPacket;
import com.stereowalker.obville.network.protocol.game.ClientboundVillagerMessagePacket;
import com.stereowalker.obville.network.protocol.game.ServerboundRelaxPacket;
import com.stereowalker.obville.world.PlacedBlocks;
import com.stereowalker.obville.world.effect.Effects;
import com.stereowalker.obville.sounds.ModSounds;
import com.stereowalker.obville.world.entity.ModEntities;
import com.stereowalker.obville.world.entity.ai.memories.ModMemories;
import com.stereowalker.obville.world.entity.VillageLeader;
import com.stereowalker.unionlib.client.gui.screens.config.MinecraftModConfigsScreen;
import com.stereowalker.unionlib.config.ConfigBuilder;
import com.stereowalker.unionlib.mod.PacketHolder;
import com.stereowalker.unionlib.mod.MinecraftMod;
import com.stereowalker.unionlib.mod.ClientSegment;
import com.stereowalker.unionlib.mod.ServerSegment;
import com.stereowalker.unionlib.api.collectors.PacketCollector;
import com.stereowalker.unionlib.api.registries.RegistryCollector;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.Level;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@Mod(value = ObVille.MOD_ID)
public class ObVille extends MinecraftMod implements PacketHolder {

	public static Map<Potion,List<Fluid>> POTION_FLUID_MAP;
	public static final String MOD_ID = "obville";

	public static final ClientConfig CLIENT_CONFIG = new ClientConfig();
	public static final ExtraLinesConfig LINES_CONFIG = new ExtraLinesConfig();
	public static final ModConfig MOD_CONFIG = new ModConfig();
	public static final ReputationAmountConfig REPUTATION_CONFIG = new ReputationAmountConfig();

	private static ObVille instance;

	public static boolean hasMoreCraftingTables() {
		return ModList.get().isLoaded("mctb");
	}

	public static boolean hasFarmersDelight() {
		return ModList.get().isLoaded("farmersdelight");
	}

	public static boolean hasGuardVillagers() {
		return ModList.get().isLoaded("guardvillagers");
	}

	public static boolean hasQuark() {
		return ModList.get().isLoaded("quark");
	}

	public static boolean hasWaystones() {
		return ModList.get().isLoaded("waystones");
	}

	public static boolean hasVillagerNames() {
		return ModList.get().isLoaded("villagernames");
	}

	public ObVille(IEventBus modEventBus) 
	{
		super("obville", () -> new ClientSegment() {
			@Override
			public ResourceLocation getModIcon() {
				return ResourceLocation.fromNamespaceAndPath(MOD_ID, "pack.png");
			}

			@Override
			public Screen getConfigScreen(Minecraft mc, Screen previousScreen) {
				return new MinecraftModConfigsScreen(previousScreen, Component.translatable("gui.obville.config.title"), MOD_CONFIG, REPUTATION_CONFIG, CLIENT_CONFIG);
			}
		}, ServerSegment::new);
		net.minecraft.world.entity.EntityType.PIG.toString();
		instance = this;
		ConfigBuilder.registerConfig(CLIENT_CONFIG);
		ConfigBuilder.registerConfig(MOD_CONFIG);
		ConfigBuilder.registerConfig(REPUTATION_CONFIG);
		ConfigBuilder.registerConfig(LINES_CONFIG);
		if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
			modEventBus.addListener(this::clientRegistries);
			modEventBus.addListener(this::registerLayerDefinitions);
			modEventBus.addListener(GuiHelper::registerOverlays);
		}
		modEventBus.addListener(this::registerAttributes);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(ObVilleCommands::onRegisterCommands);

		Laws.bootstrap();
	}

	@Override
	public void registerPackets(PacketCollector collector) {
		collector.registerServerboundPacket(ResourceLocation.fromNamespaceAndPath(MOD_ID, "relax"), ServerboundRelaxPacket.class, ServerboundRelaxPacket::new);
		collector.registerClientboundPacket(ResourceLocation.fromNamespaceAndPath(MOD_ID, "overlay_override"), ClientboundOverlayOverridePacket.class, ClientboundOverlayOverridePacket::new);
		collector.registerClientboundPacket(ResourceLocation.fromNamespaceAndPath(MOD_ID, "sound"), ClientboundSoundPacket.class, ClientboundSoundPacket::new);
		collector.registerClientboundPacket(ResourceLocation.fromNamespaceAndPath(MOD_ID, "nbt"), ClientboundNBTPacket.class, ClientboundNBTPacket::new);
		collector.registerClientboundPacket(ResourceLocation.fromNamespaceAndPath(MOD_ID, "villager_message"), ClientboundVillagerMessagePacket.class, ClientboundVillagerMessagePacket::new);
	}

	public static boolean isPotentialBandit(Player player) {
		return player.getItemBySlot(EquipmentSlot.HEAD).getItem() == Items.CARVED_PUMPKIN && !EnchantmentHelper.has(player.getItemBySlot(EquipmentSlot.HEAD), net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
	}

	public static boolean upsetOnOpen(RandomizableContainerBlockEntity chest, ServerPlayer pPlayer, BlockPos pPos , BlockPos pPos2) {
		PlacedBlocks pb = PlacedBlocks.getInstance((ServerLevel) pPlayer.level());
		ILootableBlock loot = (ILootableBlock)chest;

		if (pb.didPlayerPlaceBlock(pPos) && (pPos2 == null || pb.didPlayerPlaceBlock(pPos2))) {
			return false;
		}

		if (loot != null && loot.getLoot() != null)
			System.out.println(loot.getLoot().getPath());
		if (loot != null && MOD_CONFIG.COnts().contains(loot.getLoot()) && !loot.hasPlayerOpened(pPlayer) && upsetNearby(pPlayer, pPos, true, -REPUTATION_CONFIG.opening_containers, null)) {
			loot.addPlayer(pPlayer);
			return true;
		}
		if ((!pb.didPlayerPlaceBlock(pPos) || (pPos2 != null && !pb.didPlayerPlaceBlock(pPos2))) && upsetNearby(pPlayer, pPos, true, -REPUTATION_CONFIG.opening_containers, null)) {
			pb.playerPlacedBlock(pPos);
			if (pPos2 != null) pb.playerPlacedBlock(pPos2);
			return true;
		}
		return false;
	}

	public static boolean upsetNearby(Player player, boolean angerOnlyIfCanSee, Supplier<Crime> crime) {
		return upsetNearby(player, player.blockPosition(), angerOnlyIfCanSee, 0, crime);
	}

	public static boolean isLookingAtPlayer(LivingEntity guard, Player player) {
		return guard.hasLineOfSight(player);
	}

	@SuppressWarnings("unchecked")
	public static boolean upsetNearby(Player player, BlockPos pos, boolean angerOnlyIfCanSee, int amount, Supplier<Crime> crime) {
		float distance_from_site = 20;
		List<Villager> vills = new ArrayList<Villager>();
		List<Villager> villagersInRange = player.level().getEntitiesOfClass(Villager.class, player.getBoundingBox().inflate(16.0));
		villagersInRange.stream().filter(villager -> (
				!angerOnlyIfCanSee || (((BehaviorUtils.canSee(villager, player) || player.hasEffect(MobEffects.INVISIBILITY)) && villager.hasLineOfSight(player))) && 
				!((IVillager<Villager>)villager).recentlyTakenBribe().containsKey(player.getUUID()) &&
				!villager.isDeadOrDying())).forEach(villager -> {
					Brain<Villager> brain = villager.getBrain();
					if (brain.getMemory(MemoryModuleType.HOME).isPresent() && brain.getMemory(MemoryModuleType.HOME).get().pos().closerThan(pos, distance_from_site)) {
						vills.add(villager);
						villager.setUnhappyCounter(40);
					} else if (brain.getMemory(MemoryModuleType.JOB_SITE).isPresent() && brain.getMemory(MemoryModuleType.JOB_SITE).get().pos().closerThan(pos, distance_from_site)) {
						vills.add(villager);
						villager.setUnhappyCounter(40);
					} else {
						vills.add(villager);
						villager.setUnhappyCounter(40);
					}
				});
		List<VillageLeader> leaders = player.level().getEntitiesOfClass(VillageLeader.class, player.getBoundingBox().inflate(16.0));
		leaders.stream().filter(leader -> !angerOnlyIfCanSee || (BehaviorUtils.canSee(leader, player) && isLookingAtPlayer(leader, player)) || (player.hasEffect(MobEffects.INVISIBILITY) && leader.hasLineOfSight(player))).forEach(chief -> {
			Brain<VillageLeader> brain = chief.getBrainC();
			if (brain.getMemory(MemoryModuleType.HOME).isPresent() && brain.getMemory(MemoryModuleType.HOME).get().pos().closerThan(pos, distance_from_site)) {
				vills.add(chief);
				chief.setUnhappyCounter(40);
			} else if (brain.getMemory(MemoryModuleType.JOB_SITE).isPresent() && brain.getMemory(MemoryModuleType.JOB_SITE).get().pos().closerThan(pos, distance_from_site)) {
				vills.add(chief);
				chief.setUnhappyCounter(40);
			} else {
				vills.add(chief);
				chief.setUnhappyCounter(40);
			}
		});
		List<LivingEntity> guardians = new ArrayList<LivingEntity>();
		if (hasGuardVillagers()) {
			GuardVillagersCompat.tryToAnger(player, angerOnlyIfCanSee, guardians, vills);
		}
		Crime crimeCommited = null;
		if (crime != null) crimeCommited = crime.get();
		if (!player.hasEffect(MobEffects.INVISIBILITY)) {
			vills.forEach(villager ->{
				if (player instanceof ServerPlayer sPlayer)
					((IVillager<Villager>)villager).blacklist(sPlayer);
			});

			List<LivingEntity> b = new ArrayList<LivingEntity>();
			b.addAll(vills);
			b.addAll(guardians);
			List<IronGolem> list2 = player.level().getEntitiesOfClass(IronGolem.class, player.getBoundingBox().inflate(16.0));
			list2.stream().filter(golem -> !angerOnlyIfCanSee || isLookingAtPlayer(golem, player) || BehaviorUtils.canSee(golem, player) || vills.size() > 0).forEach(golem -> {
				b.add(golem);
			});
			if (!b.isEmpty()) {

				if (ObVille.isPotentialBandit(player)) {
					OVModData modEn = ((IModdedEntity)player).getData();
					modEn.caughtBanditry();

					b.forEach(liv -> {
						if (hasGuardVillagers()) {
							GuardVillagersCompat.target(liv, player);
						}
						if (liv instanceof IronGolem golem) {
							golem.setTarget(player);
						}
					});

					return false;
				} else {
					if (guardians.size() >= 1) {
						if (hasGuardVillagers()) {
							GuardVillagersCompat.wit(player, guardians, crimeCommited);
						}
					}
					if (vills.size() == 1) {
						if (vills.get(0) instanceof IVillager && crimeCommited != null) {
							IVillager<Villager> villager = (IVillager<Villager>)vills.get(0);
							if (!villager.recentlyWitnessedCrime().containsKey(player.getUUID())) {
								villager.witnessCrime(player, crimeCommited);
								new ClientboundOverlayOverridePacket(player.getUUID()).send((ServerPlayer)player);
								Villager villagEntity = vills.get(0);
								String cleanLine = LINES_CONFIG.caught.get(villagEntity.getRandom().nextInt(LINES_CONFIG.caught.size()));
								Component cleanMessage = Component.literal(cleanLine);
								new ClientboundVillagerMessagePacket(villager.fromVillager(cleanMessage), player.getUUID(), villagEntity, cleanMessage).send((ServerPlayer)player);
							}
						}
					}

					if (crimeCommited != null && !crimeCommited.lawBroken.isPardonable()) {
						b.forEach(liv -> {
							if (hasGuardVillagers()) {
								GuardVillagersCompat.target(liv, player);
							}
						});
					}


					OVModData modEn = ((IModdedEntity)player).getData();
					modEn.incrementReputation(crime != null ? 
							crimeCommited.lawBroken.getRepHit() : -amount);
					if (crimeCommited != null) {
						modEn.commitCrime(crimeCommited);
					}
					if (player instanceof ServerPlayer serverplayer) {
						PlacedBlocks pb = PlacedBlocks.getInstance((ServerLevel) serverplayer.level());
						new ClientboundSoundPacket(false, player.getUUID()).send(serverplayer);
						if (modEn.IsExiled() && !modEn.reput().generatedBounty) {
							modEn.reput().generatedBounty = true;
							pb.villages.get(modEn.currentVillage()).generatedBounties.add(VillageData.bounty(serverplayer, modEn.currentVillage()));
						}
					}

					return true;
				}

			}
		} else {
			vills.forEach(villag -> {
				IVillager<Villager> villager = (IVillager<Villager>)villag;
				if (villager.invisibleLineCooldown() <= 0 && !(villag instanceof VillageLeader)) {
					villager.setInvisibleLineCooldown(MOD_CONFIG.invi);
					String cleanLine = LINES_CONFIG.invisible.get(villag.getRandom().nextInt(LINES_CONFIG.invisible.size()));
					Component cleanMessage = Component.literal(cleanLine);
					new ClientboundVillagerMessagePacket(villager.fromVillager(cleanMessage), player.getUUID(), villag, cleanMessage).send((ServerPlayer)player);
				}
			});
			final Crime cc = crimeCommited;
			List<LivingEntity> investigators = new ArrayList<>();
			investigators.addAll(guardians);
			investigators.addAll(leaders);
			IInvestigator investigator = (IInvestigator) investigators.get(player.getRandom().nextInt(investigators.size()));
			investigator.setInvestigatePos(pos);
			if (cc != null)
				investigator.crimeToInvestigate(cc);
		}
		return false;
	}



	public static int determineVillage(ServerLevel level, BlockPos blockpos) {
		PlacedBlocks pb = PlacedBlocks.getInstance(level);
		if (level.isCloseToVillage(blockpos, 2)) {
			if (ObVille.MOD_CONFIG.global_rep)
				return 0;
			else
				return pb.isVillageOrRegisterVillage(level, blockpos);
		}
		return -1;
	}

	public void clientRegistries(final FMLClientSetupEvent event)
	{
		EntityRenderers.register(ModEntities.VILLAGE_CHIEF.value().get(), VillagerChiefRenderer::new);
	}

	public void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
		event.registerLayerDefinition(VillageChiefModel.LAYER_LOCATION, VillageChiefModel::createBodyLayer);
	}

	@Override
	public void setupRegistries(RegistryCollector collector) {
		collector.addRegistryHolder(net.minecraft.core.registries.Registries.MOB_EFFECT, Effects.class);
		collector.addCustom(net.minecraft.core.registries.Registries.SOUND_EVENT, custom -> {
			custom.register(net.minecraft.resources.ResourceLocation.parse("obville:positive"), ModSounds.POSITIVE.value().get());
			custom.register(net.minecraft.resources.ResourceLocation.parse("obville:negative"), ModSounds.NEGATIVE.value().get());
		});
		collector.addCustom(net.minecraft.core.registries.Registries.ENTITY_TYPE, custom -> {
			custom.register(net.minecraft.resources.ResourceLocation.parse("obville:village_leader"), ModEntities.VILLAGE_CHIEF.value().get());
		});
		collector.addCustom(net.minecraft.core.registries.Registries.MEMORY_MODULE_TYPE, custom -> {
			custom.register(net.minecraft.resources.ResourceLocation.parse("obville:leader_detected_recently"), ModMemories.LEADER_DETECTED_RECENTLY.value().get());
		});
		
	}

	public static ObVille getInstance() {
		return instance;
	}

	public static boolean isImprisoned(Villager villager) {
		if (!MOD_CONFIG.prevent_trading_halls) return false;
		
		Level level = villager.level();
		BlockPos start = villager.blockPosition();
		java.util.Set<BlockPos> visited = new java.util.HashSet<>();
		java.util.Queue<BlockPos> queue = new java.util.LinkedList<>();
		
		queue.add(start);
		visited.add(start);
		
		int walkableFloorBlocks = 0;
		int threshold = MOD_CONFIG.trading_hall_space_threshold;
		
		while (!queue.isEmpty() && visited.size() < 50) {
			BlockPos current = queue.poll();
			
			if (isPassable(level, current) && isPassable(level, current.above())) {
				if (!level.getBlockState(current.below()).getCollisionShape(level, current.below()).isEmpty()) {
					walkableFloorBlocks++;
					if (walkableFloorBlocks >= threshold) {
						return false;
					}
				}
			}
			
			for (Direction dir : Direction.values()) {
				if (dir == Direction.UP || dir == Direction.DOWN) continue;
				
				for (int dy = -1; dy <= 1; dy++) {
					BlockPos neighbor = current.relative(dir).above(dy);
					if (!visited.contains(neighbor)) {
						if (canPassBetween(level, current, neighbor)) {
							visited.add(neighbor);
							queue.add(neighbor);
						}
					}
				}
			}
		}
		
		return walkableFloorBlocks < threshold;
	}

	private static boolean isPassable(Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir()) {
			return true;
		}
		if (state.is(BlockTags.WOODEN_DOORS) ||
			state.is(BlockTags.WOODEN_TRAPDOORS) ||
			state.getBlock() instanceof net.minecraft.world.level.block.FenceGateBlock) {
			return true;
		}
		return state.getCollisionShape(level, pos).isEmpty();
	}

	private static boolean canPassBetween(Level level, BlockPos from, BlockPos to) {
		int dy = to.getY() - from.getY();
		if (dy == 0) {
			return isPassable(level, to) && isPassable(level, to.above());
		} else if (dy == 1) {
			return isPassable(level, from.above().above()) && isPassable(level, to) && isPassable(level, to.above());
		} else if (dy == -1) {
			return isPassable(level, to) && isPassable(level, to.above());
		}
		return false;
	}

	private void registerAttributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
		event.put(ModEntities.VILLAGE_CHIEF.value().get(), VillageLeader.createAttributes().build());
	}

}
