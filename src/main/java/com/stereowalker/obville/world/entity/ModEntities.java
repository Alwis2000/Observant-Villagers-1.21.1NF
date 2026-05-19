package com.stereowalker.obville.world.entity;

import com.stereowalker.obville.ObVille;
import com.stereowalker.unionlib.core.registries.Housing;
import com.stereowalker.unionlib.core.registries.RegistryHolder;
import com.stereowalker.unionlib.core.registries.RegistryObject;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

@RegistryHolder(namespace = ObVille.MOD_ID)
public class ModEntities {
	
	private static EntityType<VillageLeader> chief;
	
	@RegistryObject("village_leader")
	public static final Housing<EntityType<VillageLeader>> VILLAGE_CHIEF = Housing.create(() -> {
		if (chief == null) {
			chief = EntityType.Builder.<VillageLeader>of(VillageLeader::new, MobCategory.CREATURE).sized(0.6F, 1.95F).build("obville:village_leader");
		}
		return chief;
	});
	
}
