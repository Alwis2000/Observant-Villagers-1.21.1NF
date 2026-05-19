package com.stereowalker.obville.network.protocol.game;

import com.stereowalker.unionlib.network.protocol.game.ServerboundUnionPacket;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public class ServerboundRelaxPacket extends ServerboundUnionPacket {
	private final int amount;

	public ServerboundRelaxPacket(int amount) {
		super(null);
		this.amount = amount;
	}

	public ServerboundRelaxPacket(RegistryFriendlyByteBuf packetBuffer) {
		super(packetBuffer);
		this.amount = packetBuffer.readVarInt();
	}

	@Override
	public void encode(FriendlyByteBuf friendlyByteBuf) {
		RegistryFriendlyByteBuf packetBuffer = (RegistryFriendlyByteBuf) friendlyByteBuf;
		packetBuffer.writeVarInt(this.amount);
	}

	@Override
	public boolean handleOnServer(ServerPlayer sender) {
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ResourceLocation.parse("obville:relax");
	}
}
