package com.stereowalker.obville.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.network.chat.Component;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderTalkBalloonsMixin implements com.cerbon.talk_balloons.util.mixin.ITalkBalloonsPlayer {

	@Unique
	private com.cerbon.talk_balloons.util.HistoricalData<com.cerbon.talk_balloons.util.BalloonData> talk_balloons$balloonMessages;

	@Unique
	private final java.util.Collection<java.util.function.Supplier<Boolean>> talk_balloons$queuedTickEvents = new java.util.concurrent.ConcurrentLinkedDeque<>();

	@Unique
	private void talk_balloons$tryInitHistoricalData() {
		if (this.talk_balloons$balloonMessages == null) {
			this.talk_balloons$balloonMessages = new com.cerbon.talk_balloons.util.HistoricalData<>(com.cerbon.talk_balloons.TalkBalloons.config.getMaxBalloons());
		}
	}

	@Override
	public void talk_balloons$createBalloonMessage(Component text, int timeToRemove) {
		this.talk_balloons$tryInitHistoricalData();
		com.cerbon.talk_balloons.util.HistoricalData<com.cerbon.talk_balloons.util.BalloonData> messages = this.talk_balloons$getBalloonMessages();
		java.util.concurrent.atomic.AtomicInteger currentTick = new java.util.concurrent.atomic.AtomicInteger(0);
		com.cerbon.talk_balloons.util.BalloonData balloonData = com.cerbon.talk_balloons.util.BalloonData.create(text, timeToRemove);
		this.talk_balloons$queuedTickEvents.add(() -> {
			if (currentTick.getAndIncrement() >= timeToRemove) {
				messages.remove(balloonData);
				return true;
			}
			return false;
		});
		messages.add(balloonData);
	}

	@Override
	public com.cerbon.talk_balloons.util.HistoricalData<com.cerbon.talk_balloons.util.BalloonData> talk_balloons$getBalloonMessages() {
		this.talk_balloons$tryInitHistoricalData();
		return this.talk_balloons$balloonMessages;
	}

	@Inject(method = "obville$tickTrader", at = @At("HEAD"))
	private void talk_balloons$tickBalloons(CallbackInfo ci) {
		WanderingTrader trader = (WanderingTrader) (Object) this;
		if (trader.level().isClientSide()) {
			java.util.HashSet<java.util.function.Supplier<Boolean>> eventsToRemove = new java.util.HashSet<>();
			for (java.util.function.Supplier<Boolean> event : this.talk_balloons$queuedTickEvents) {
				if (event.get()) {
					eventsToRemove.add(event);
				}
			}
			if (!eventsToRemove.isEmpty()) {
				this.talk_balloons$queuedTickEvents.removeAll(eventsToRemove);
			}
		}
	}
}
