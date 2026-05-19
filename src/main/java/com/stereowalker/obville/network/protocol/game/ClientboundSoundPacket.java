package com.stereowalker.obville.network.protocol.game;

import java.util.UUID;
import com.stereowalker.obville.sounds.ModSounds;
import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class ClientboundSoundPacket extends ClientboundUnionPacket {
	private final boolean positive;
	private final UUID uuid;

	public ClientboundSoundPacket(final boolean positive, final UUID uuid) {
		super(null);
		this.uuid = uuid;
		this.positive = positive;
	}

	public ClientboundSoundPacket(RegistryFriendlyByteBuf packetBuffer) {
		super(packetBuffer);
		this.positive = packetBuffer.readBoolean();
		this.uuid = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
	}

	@Override
	public void encode(FriendlyByteBuf friendlyByteBuf) {
		RegistryFriendlyByteBuf packetBuffer = (RegistryFriendlyByteBuf) friendlyByteBuf;
		packetBuffer.writeBoolean(this.positive);
		packetBuffer.writeLong(this.uuid.getMostSignificantBits());
		packetBuffer.writeLong(this.uuid.getLeastSignificantBits());
	}

	@Override
	public boolean handleOnClient(LocalPlayer player) {
		if (uuid.equals(player.getUUID())) {
			player.level().playLocalSound(player.getX(), player.getY(), player.getZ(), positive ? ModSounds.POSITIVE.value().get() : ModSounds.NEGATIVE.value().get(), SoundSource.PLAYERS, 1, 1, false);
		}
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ResourceLocation.parse("obville:sound");
	}
}
