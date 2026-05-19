package com.stereowalker.obville.mixins.client;

import com.cerbon.talk_balloons.client.BalloonRenderer;
import com.cerbon.talk_balloons.util.mixin.ITalkBalloonsPlayer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

	@Inject(method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
	private void talk_balloons$renderBalloons(LivingEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
		if (entity instanceof Player) {
			return; // Talk Balloons already renders players
		}
		if (entity instanceof ITalkBalloonsPlayer talkEntity) {
			if (talkEntity.talk_balloons$getBalloonMessages() != null && !talkEntity.talk_balloons$getBalloonMessages().isEmpty()) {
				Font font = ((EntityRenderer<?>) (Object) this).getFont();
				org.joml.Quaternionf cameraOrientation = Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation();
				BalloonRenderer.renderBalloons(
						poseStack,
						buffer,
						BalloonRenderer.toEulerXyzDegrees(cameraOrientation),
						font,
						talkEntity.talk_balloons$getBalloonMessages(),
						entity.getBbHeight(),
						com.cerbon.talk_balloons.client.TalkBalloonsClient.syncedConfigs.getPlayerConfig(entity.getUUID())
				);
			}
		}
	}
}
