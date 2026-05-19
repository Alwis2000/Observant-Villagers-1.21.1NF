package com.stereowalker.obville.network.protocol.game;

import java.util.UUID;
import com.stereowalker.obville.interfaces.IModdedEntity;
import com.stereowalker.unionlib.network.protocol.game.ClientboundUnionPacket;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class ClientboundNBTPacket extends ClientboundUnionPacket {
	private CompoundTag stats;
	private UUID uuid;
	
	public ClientboundNBTPacket(final CompoundTag statsIn, final UUID uuid) {
		super(null);
		this.stats = statsIn;
		this.uuid = uuid;
	}
	
	public ClientboundNBTPacket(final Player player){
		super(null);
		this.stats = new CompoundTag();
		this.uuid = player.getUUID();
		((IModdedEntity)player).getData().write(stats, player.level().registryAccess());
	}
	
	public ClientboundNBTPacket(RegistryFriendlyByteBuf packetBuffer) {
		super(packetBuffer);
		this.stats = packetBuffer.readNbt();
		this.uuid = new UUID(packetBuffer.readLong(), packetBuffer.readLong());
	}

	@Override
	public void encode(final FriendlyByteBuf friendlyByteBuf) {
		RegistryFriendlyByteBuf packetBuffer = (RegistryFriendlyByteBuf) friendlyByteBuf;
		packetBuffer.writeNbt(this.stats);
		packetBuffer.writeLong(this.uuid.getMostSignificantBits());
		packetBuffer.writeLong(this.uuid.getLeastSignificantBits());
	}
	
	@Override
	public boolean handleOnClient(LocalPlayer player) {
		((IModdedEntity)player).getData().read(stats, player.level().registryAccess());
		return true;
	}

	@Override
	public ResourceLocation id() {
		return ResourceLocation.parse("obville:nbt");
	}
}
