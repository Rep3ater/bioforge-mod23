package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, BioForgeMod.MODID);

    public static final RegistryObject<SimpleParticleType> BLOOD_SPLATTER =
            PARTICLES.register("blood_splatter", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> SURGICAL_SPARK =
            PARTICLES.register("surgical_spark", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> CLONE_AURA =
            PARTICLES.register("clone_aura", () -> new SimpleParticleType(false));
}
