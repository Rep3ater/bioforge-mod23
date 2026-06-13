package com.bioforge.mod.bodyexit;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBodyEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, BioForgeMod.MODID);

    public static final RegistryObject<EntityType<BodyShellEntity>> BODY_SHELL =
            ENTITIES.register("body_shell", () ->
                    EntityType.Builder.<BodyShellEntity>of(BodyShellEntity::new, MobCategory.MISC)
                            .sized(0.6f, 1.8f)
                            .clientTrackingRange(10)
                            .updateInterval(3)
                            .build("body_shell"));
}
