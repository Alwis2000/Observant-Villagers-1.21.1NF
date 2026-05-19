package com.stereowalker.obville.world.entity.ai.memories;

import java.util.Optional;
import com.mojang.serialization.Codec;
import com.stereowalker.obville.ObVille;
import com.stereowalker.unionlib.core.registries.Housing;
import com.stereowalker.unionlib.core.registries.RegistryHolder;
import com.stereowalker.unionlib.core.registries.RegistryObject;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

@RegistryHolder(namespace = ObVille.MOD_ID)
public class ModMemories {
	
	private static MemoryModuleType<Boolean> leaderDetectedRecently;

	@RegistryObject("leader_detected_recently")
	public static final Housing<MemoryModuleType<Boolean>> LEADER_DETECTED_RECENTLY = Housing.create(() -> {
		if (leaderDetectedRecently == null) {
			leaderDetectedRecently = new MemoryModuleType<>(Optional.of(Codec.BOOL));
		}
		return leaderDetectedRecently;
	});
	
}
