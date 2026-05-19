package com.stereowalker.obville.network.protocol.game;

import java.util.UUID;
import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ClientboundOverlayOverridePacket extends ClientboundUnionPacket {
	private final UUID uuid;

	public ClientboundOverlayOverridePacket(final UUID uuid) {
		super(null);
		this.uuid = uuid;
	}
	
	public ClientboundOverlayOverridePacket(RegistryFriendlyByteBuf packetBuffer) {
		super(packetBuffer);
		this.uuid = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
	}

	@Override
	public void encode(FriendlyByteBuf friendlyByteBuf) {
		RegistryFriendlyByteBuf packetBuffer = (RegistryFriendlyByteBuf) friendlyByteBuf;
		packetBuffer.writeLong(uuid.getMostSignificantBits());
		packetBuffer.writeLong(uuid.getLeastSignificantBits());
	}

	@Override
	public boolean handleOnClient(LocalPlayer player) {
		if (uuid.equals(player.getUUID())) {
			Minecraft mc = Minecraft.getInstance();
			mc.gui.setOverlayMessage(Component.translatable("obville.messages.bribe_opportunity"), false);
		}
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ResourceLocation.parse("obville:overlay_override");
	}
}
