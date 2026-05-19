package com.stereowalker.obville.network.protocol.game;

import java.util.UUID;
import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;

public class ClientboundVillagerMessagePacket extends ClientboundUnionPacket {
	private final Component message;
	private final UUID uuid;

	public ClientboundVillagerMessagePacket(final Component message, final UUID uuid) {
		super(null);
		this.uuid = uuid;
		this.message = message;
	}
	
	public ClientboundVillagerMessagePacket(RegistryFriendlyByteBuf packetBuffer) {
		super(packetBuffer);
		this.message = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(packetBuffer);
		this.uuid = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
	}

	@Override
	public void encode(FriendlyByteBuf friendlyByteBuf) {
		RegistryFriendlyByteBuf packetBuffer = (RegistryFriendlyByteBuf) friendlyByteBuf;
		ComponentSerialization.TRUSTED_STREAM_CODEC.encode(packetBuffer, message);
		packetBuffer.writeLong(uuid.getMostSignificantBits());
		packetBuffer.writeLong(uuid.getLeastSignificantBits());
	}

	@Override
	public boolean handleOnClient(LocalPlayer player) {
		if (uuid.equals(player.getUUID())) {
			player.sendSystemMessage(message);
		}
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ResourceLocation.parse("obville:villager_message");
	}
}
