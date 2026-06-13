package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.cloning.CloneEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BioForgeMod.MODID);

    public static final RegistryObject<EntityType<CloneEntity>> CLONE =
            ENTITIES.register("clone", () ->
                    EntityType.Builder.<CloneEntity>of(CloneEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(8)
                            .updateInterval(3)
                            .build("clone"));
}
