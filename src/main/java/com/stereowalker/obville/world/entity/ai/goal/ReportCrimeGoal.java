package com.stereowalker.obville.world.entity.ai.goal;

import java.util.EnumSet;
import java.util.List;

import com.stereowalker.obville.Crime;
import com.stereowalker.obville.Law;
import com.stereowalker.obville.ObVille;
import com.stereowalker.obville.interfaces.IInvestigator;
import com.stereowalker.obville.interfaces.IVillager;
import com.stereowalker.obville.network.protocol.game.ClientboundVillagerMessagePacket;
import com.stereowalker.obville.world.entity.VillageLeader;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;

/**
 * AI Goal for regular Villagers to run to the nearest authority figure when they witness a crime.
 * The villager enters a panic state, pathfinds to a Guard/IronGolem/VillageLeader, and reports.
 * Only when they reach the authority do the actual crime consequences fire.
 */
public class ReportCrimeGoal extends Goal {
	private final Villager villager;
	private final double speedModifier;
	private LivingEntity targetAuthority;
	private int ticksRunning;
	private boolean hasSpokenPanicLine;
	private static final int PANIC_TIMEOUT = 600; // 30 seconds
	private static final double REPORT_DISTANCE_SQ = 9.0D; // 3 blocks squared
	private int calmDown;

	public ReportCrimeGoal(Villager villager, double speedModifier) {
		this.villager = villager;
		this.speedModifier = speedModifier;
		this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
	}

	@Override
	public boolean canUse() {
		if (this.calmDown > 0) {
			--this.calmDown;
			return false;
		}
		if (this.villager instanceof IVillager<?> iVillager) {
			return iVillager.isPanicking() && iVillager.getCriminal() != null;
		}
		return false;
	}

	@Override
	public boolean canContinueToUse() {
		if (this.ticksRunning >= PANIC_TIMEOUT) return false;
		if (this.villager instanceof IVillager<?> iVillager) {
			if (!iVillager.isPanicking()) return false;
			if (iVillager.getCriminal() == null || !iVillager.getCriminal().isAlive()) return false;
			// Check if our target authority is still valid
			if (this.targetAuthority != null && !this.targetAuthority.isAlive()) {
				// Try to find a new authority
				this.targetAuthority = findNearestAuthority();
				if (this.targetAuthority == null) return false;
				iVillager.setPanicTarget(this.targetAuthority);
			}
			return true;
		}
		return false;
	}

	@Override
	public void start() {
		this.ticksRunning = 0;
		this.hasSpokenPanicLine = false;
		
		if (this.villager instanceof IVillager<?> iVillager) {
			this.targetAuthority = iVillager.getPanicTarget();
			
			// If no target was pre-set, find one now
			if (this.targetAuthority == null || !this.targetAuthority.isAlive()) {
				this.targetAuthority = findNearestAuthority();
				iVillager.setPanicTarget(this.targetAuthority);
			}
		}
		
		// Show angry particles
		this.villager.setUnhappyCounter(40);
	}

	@Override
	public void tick() {
		this.ticksRunning++;
		
		if (!(this.villager instanceof IVillager<?> iVillager)) return;
		if (this.targetAuthority == null || !this.targetAuthority.isAlive()) return;
		
		// Speak a panic line (once)
		if (!this.hasSpokenPanicLine) {
			speakPanicLine(iVillager);
			this.hasSpokenPanicLine = true;
		}
		
		// Periodically re-show angry particles while panicking
		if (this.ticksRunning % 40 == 0) {
			this.villager.setUnhappyCounter(40);
		}
		
		// Check if we've reached the authority
		if (this.villager.distanceToSqr(this.targetAuthority) < REPORT_DISTANCE_SQ) {
			// Report the crime!
			reportCrime(iVillager);
			return;
		}
		
		// Keep pathfinding to authority
		this.villager.getLookControl().setLookAt(this.targetAuthority, 30.0F, 30.0F);
		this.villager.getNavigation().moveTo(this.targetAuthority, this.speedModifier);
	}

	@Override
	public void stop() {
		if (this.villager instanceof IVillager<?> iVillager) {
			iVillager.setPanicking(false);
			iVillager.setPanicTarget(null);
			iVillager.setCrimeToReport(null);
			iVillager.setCriminal(null);
		}
		this.targetAuthority = null;
		this.villager.getNavigation().stop();
		this.calmDown = reducedTickDelay(200);
	}

	/**
	 * Find the nearest authority figure. Priority: Guard > IronGolem > VillageLeader
	 */
	private LivingEntity findNearestAuthority() {
		// 1. Try Guards first (if Guard Villagers mod is loaded)
		if (ObVille.hasGuardVillagers()) {
			try {
				List<? extends LivingEntity> guards = com.stereowalker.obville.compat.GuardVillagersCompat.findNearbyGuards(this.villager);
				if (guards != null && !guards.isEmpty()) {
					LivingEntity nearest = null;
					double nearestDist = Double.MAX_VALUE;
					for (LivingEntity guard : guards) {
						double dist = this.villager.distanceToSqr(guard);
						if (dist < nearestDist) {
							nearestDist = dist;
							nearest = guard;
						}
					}
					if (nearest != null) return nearest;
				}
			} catch (Exception e) {
				// Guard Villagers compat failed, continue to fallbacks
			}
		}
		
		// 2. Try Iron Golems
		List<IronGolem> golems = this.villager.level().getEntitiesOfClass(
			IronGolem.class, this.villager.getBoundingBox().inflate(32.0));
		if (!golems.isEmpty()) {
			IronGolem nearest = null;
			double nearestDist = Double.MAX_VALUE;
			for (IronGolem golem : golems) {
				double dist = this.villager.distanceToSqr(golem);
				if (dist < nearestDist) {
					nearestDist = dist;
					nearest = golem;
				}
			}
			if (nearest != null) return nearest;
		}
		
		// 3. Always fall back to VillageLeader
		List<VillageLeader> leaders = this.villager.level().getEntitiesOfClass(
			VillageLeader.class, this.villager.getBoundingBox().inflate(64.0));
		if (!leaders.isEmpty()) {
			VillageLeader nearest = null;
			double nearestDist = Double.MAX_VALUE;
			for (VillageLeader leader : leaders) {
				double dist = this.villager.distanceToSqr(leader);
				if (dist < nearestDist) {
					nearestDist = dist;
					nearest = leader;
				}
			}
			return nearest;
		}
		
		return null;
	}

	/**
	 * Speak a panic line based on personality.
	 * Extroverts yell loudly, introverts mutter quietly, ambiverts are 50/50.
	 */
	private void speakPanicLine(IVillager<?> iVillager) {
		Player criminal = iVillager.getCriminal();
		if (criminal == null || !(criminal instanceof ServerPlayer serverPlayer)) return;
		
		Crime crime = iVillager.getCrimeToReport();
		Law.Severity severity = (crime != null && crime.lawBroken != null) 
			? crime.lawBroken.getSeverity() : Law.Severity.LOW;
		
		int personalityHash = Math.abs(this.villager.getUUID().hashCode()) % 10;
		boolean isExtrovert = personalityHash < 4;  // 40%
		boolean isAmbivert = personalityHash >= 7;   // 30%
		// Introvert = 4-6 (30%) — they stay silent
		
		boolean shouldSpeak = false;
		if (severity == Law.Severity.HIGH) {
			// Everyone screams during murder
			shouldSpeak = true;
		} else if (isExtrovert) {
			shouldSpeak = true;
		} else if (isAmbivert) {
			shouldSpeak = this.villager.getRandom().nextBoolean();
		}
		// Introverts: silent for LOW/MEDIUM
		
		if (shouldSpeak) {
			List<String> lines;
			if (severity == Law.Severity.HIGH) {
				lines = ObVille.LINES_CONFIG.panic_murder;
			} else if (isExtrovert || (isAmbivert && this.villager.getRandom().nextBoolean())) {
				lines = ObVille.LINES_CONFIG.panic_loud;
			} else {
				lines = ObVille.LINES_CONFIG.panic_quiet;
			}
			
			if (lines != null && !lines.isEmpty()) {
				String line = lines.get(this.villager.getRandom().nextInt(lines.size()));
				Component cleanMessage = Component.literal(line);
				Component message = iVillager.fromVillager(cleanMessage);
				new ClientboundVillagerMessagePacket(message, criminal.getUUID(), this.villager, cleanMessage)
					.send(serverPlayer);
			}
		}
	}

	/**
	 * The villager has reached the authority — report the crime.
	 */
	private void reportCrime(IVillager<?> iVillager) {
		Player criminal = iVillager.getCriminal();
		Crime crime = iVillager.getCrimeToReport();
		
		if (this.targetAuthority instanceof Mob mob && this.targetAuthority instanceof IInvestigator investigator) {
			investigator.onReportReceived(mob, this.villager, criminal, crime);
		} else if (this.targetAuthority instanceof IronGolem golem && criminal != null) {
			// Iron Golems just get angry at the player
			golem.setTarget(criminal);
		}
		
		// Clear panic state — stop() will handle the rest
		iVillager.setPanicking(false);
	}
}
