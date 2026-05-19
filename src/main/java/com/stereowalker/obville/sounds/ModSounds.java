package com.stereowalker.obville.sounds;

import com.stereowalker.obville.ObVille;
import com.stereowalker.unionlib.core.registries.Housing;
import com.stereowalker.unionlib.core.registries.RegistryHolder;
import com.stereowalker.unionlib.core.registries.RegistryObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

@RegistryHolder(namespace = ObVille.MOD_ID)
public class ModSounds {
	
	private static SoundEvent positive;
	private static SoundEvent negative;

	@RegistryObject("positive")
	public static final Housing<SoundEvent> POSITIVE = Housing.create(() -> {
		if (positive == null) {
			positive = SoundEvent.createVariableRangeEvent(ResourceLocation.parse("obville:positive"));
		}
		return positive;
	});
	
	@RegistryObject("negative")
	public static final Housing<SoundEvent> NEGATIVE = Housing.create(() -> {
		if (negative == null) {
			negative = SoundEvent.createVariableRangeEvent(ResourceLocation.parse("obville:negative"));
		}
		return negative;
	});
	
}