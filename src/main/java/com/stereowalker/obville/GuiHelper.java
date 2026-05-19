package com.stereowalker.obville;

import com.stereowalker.obville.dat.OVModData;
import com.stereowalker.obville.interfaces.IModdedEntity;
import com.stereowalker.unionlib.util.ScreenHelper;
import com.stereowalker.unionlib.util.ScreenHelper.ScreenOffset;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@OnlyIn(Dist.CLIENT)
public class GuiHelper {
	public static final ResourceLocation GUI_ICONS = ResourceLocation.fromNamespaceAndPath(ObVille.MOD_ID, "textures/gui/icons.png");

	@OnlyIn(Dist.CLIENT)
	public static void registerOverlays(RegisterGuiLayersEvent event) {
		event.registerAbove(
			VanillaGuiLayers.EXPERIENCE_BAR,
			ResourceLocation.fromNamespaceAndPath(ObVille.MOD_ID, "reputation"),
			(guiGraphics, deltaTracker) -> {
				Minecraft mc = Minecraft.getInstance();
				if (!mc.options.hideGui) {
					GuiHelper.renderTemperature(guiGraphics, ObVille.CLIENT_CONFIG.reputationPosition, mc.cameraEntity instanceof Player ? (Player) mc.cameraEntity : mc.player);
				}
			}
		);
	}

	public static int getXOffset(ScreenOffset pos, Minecraft mc) {
		if (pos.equals((Object) ScreenOffset.TOP_LEFT) || pos.equals((Object) ScreenOffset.LEFT)
				|| pos.equals((Object) ScreenOffset.BOTTOM_LEFT)) {
			return 0;
		}
		if (pos.equals((Object) ScreenOffset.TOP) || pos.equals((Object) ScreenOffset.CENTER)
				|| pos.equals((Object) ScreenOffset.BOTTOM)) {
			return mc.getWindow().getGuiScaledWidth() / 2;
		}
		if (pos.equals((Object) ScreenOffset.TOP_RIGHT) || pos.equals((Object) ScreenOffset.RIGHT)
				|| pos.equals((Object) ScreenOffset.BOTTOM_RIGHT)) {
			return mc.getWindow().getGuiScaledWidth();
		}
		return 0;
	}

	@SuppressWarnings("resource")
	@OnlyIn(Dist.CLIENT)
	public static void renderTemperature(GuiGraphics guiGraphics, ScreenOffset position, Player playerentity) {
		if (playerentity == null) return;
		Minecraft mc = Minecraft.getInstance();
		if (position == ScreenOffset.TOP && mc.gui.getBossOverlay().events.size() > 0) position = ScreenOffset.TOP_LEFT;
		
		int x = getXOffset(position, mc);
		int y = ScreenHelper.getYOffset(position, mc);
		mc.getProfiler().push("temperature");
		OVModData ent = ((IModdedEntity)playerentity).getData();

		int repu = ent.getPrevReputation();
		int col = 0;
		String rep = "";
		if (ent.IsWelcomeAt(ent.previousVillage())) {
			rep = "Welcomed";
			col = ObVille.CLIENT_CONFIG.welcome;
		}
		else if (ent.IsWearyAt(ent.previousVillage())) {
			rep = "Weary";
			col = ObVille.CLIENT_CONFIG.weary;
		}
		else if (ent.IsDistrustedAt(ent.previousVillage())) {
			rep = "Distrusting";
			col = ObVille.CLIENT_CONFIG.distrusted;
		}
		else if (ent.IsExiledAt(ent.previousVillage())) {
			rep = "Exiled";
			col = ObVille.CLIENT_CONFIG.exiled;
		}
		else {
			rep = "Neutral";
			col = ObVille.CLIENT_CONFIG.neutral;
		}
		Component rep2 = Component.literal("Reputation - ")
				.append(Component.literal(rep).setStyle(Style.EMPTY.withColor(col)));

		int repDiff = repu - ent.prevReputation.getOrDefault(ent.previousVillage(), 0); 
		if (repDiff != 0) {
			rep2 = rep2.copy().append(Component.literal(" "+(repDiff > 0 ? "+"+repDiff : repDiff)).setStyle(Style.EMPTY.withColor(repDiff > 0 ? 0x00AA00 : repDiff < 0 ? 0xAA0000 : 0xBBBBBB)));
		}
		
		int w = mc.font.width(rep2);
		guiGraphics.pose().pushPose();
		if (position == ScreenOffset.BOTTOM_RIGHT || position == ScreenOffset.RIGHT ||  position == ScreenOffset.TOP_RIGHT)
			x -= w;
		else if (position == ScreenOffset.BOTTOM || position == ScreenOffset.CENTER ||  position == ScreenOffset.TOP)
			x -= w / 2;
		
		if (position == ScreenOffset.BOTTOM_LEFT || position == ScreenOffset.BOTTOM ||  position == ScreenOffset.BOTTOM_RIGHT)
			y -= mc.font.lineHeight;
		else if (position == ScreenOffset.LEFT || position == ScreenOffset.CENTER ||  position == ScreenOffset.RIGHT)
			y -= mc.font.lineHeight / 2;
		int vTicks = ent.getVisibleTicks();
		if (vTicks != 0) {
			float time = (float)vTicks / 10.0f;
			int alpha = (int)(time * 255f);
			guiGraphics.drawString(mc.font, rep2, x, y, (alpha << 24) + 0xFFFFFF, true);
		}
		guiGraphics.pose().popPose();
		mc.getProfiler().pop();
	}
}
