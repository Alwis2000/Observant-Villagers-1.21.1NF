package com.stereowalker.obville.network.protocol.game;

import java.util.UUID;
import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class ClientboundVillagerMessagePacket extends ClientboundUnionPacket {
	private final Component message;
	private final UUID uuid;
	private final int villagerEntityId;
	private final Component cleanMessage;

	public ClientboundVillagerMessagePacket(final Component message, final UUID uuid) {
		this(message, uuid, -1, message);
	}

	public ClientboundVillagerMessagePacket(final Component message, final UUID uuid, int villagerEntityId, Component cleanMessage) {
		super(null);
		this.uuid = uuid;
		this.message = message;
		this.villagerEntityId = villagerEntityId;
		this.cleanMessage = cleanMessage;
	}

	public ClientboundVillagerMessagePacket(final Component message, final UUID uuid, Entity villager, Component cleanMessage) {
		this(message, uuid, villager != null ? villager.getId() : -1, cleanMessage);
	}
	
	public ClientboundVillagerMessagePacket(RegistryFriendlyByteBuf packetBuffer) {
		super(packetBuffer);
		this.message = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(packetBuffer);
		this.uuid = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
		this.villagerEntityId = packetBuffer.readVarInt();
		this.cleanMessage = ComponentSerialization.TRUSTED_STREAM_CODEC.decode(packetBuffer);
	}

	@Override
	public void encode(FriendlyByteBuf friendlyByteBuf) {
		RegistryFriendlyByteBuf packetBuffer = (RegistryFriendlyByteBuf) friendlyByteBuf;
		ComponentSerialization.TRUSTED_STREAM_CODEC.encode(packetBuffer, message);
		packetBuffer.writeLong(uuid.getMostSignificantBits());
		packetBuffer.writeLong(uuid.getLeastSignificantBits());
		packetBuffer.writeVarInt(villagerEntityId);
		ComponentSerialization.TRUSTED_STREAM_CODEC.encode(packetBuffer, cleanMessage);
	}

	@Override
	public boolean handleOnClient(LocalPlayer player) {
		if (uuid.equals(player.getUUID())) {
			boolean shownInBalloon = false;
			if (villagerEntityId != -1 && player.level() != null && net.neoforged.fml.ModList.get().isLoaded("talk_balloons")) {
				Entity entity = player.level().getEntity(villagerEntityId);
				if (entity != null) {
					com.stereowalker.obville.compat.TalkBalloonsCompat.createBalloon(entity, cleanMessage);
					shownInBalloon = true;
				}
			}
			if (!shownInBalloon) {
				player.sendSystemMessage(message);
			}
		}
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ResourceLocation.parse("obville:villager_message");
	}
}
