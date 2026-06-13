package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, BioForgeMod.MODID);

    public static final RegistryObject<SoundEvent> NEEDLE_INSERT =
            register("needle_insert");
    public static final RegistryObject<SoundEvent> SCALPEL_CUT =
            register("scalpel_cut");
    public static final RegistryObject<SoundEvent> BONE_SAW =
            register("bone_saw");
    public static final RegistryObject<SoundEvent> CAUTERIZE =
            register("cauterize");
    public static final RegistryObject<SoundEvent> ORGAN_REMOVE =
            register("organ_remove");
    public static final RegistryObject<SoundEvent> ORGAN_IMPLANT =
            register("organ_implant");
    public static final RegistryObject<SoundEvent> LIMB_REMOVE =
            register("limb_remove");
    public static final RegistryObject<SoundEvent> LIMB_ATTACH =
            register("limb_attach");
    public static final RegistryObject<SoundEvent> CLONE_SPAWN =
            register("clone_spawn");
    public static final RegistryObject<SoundEvent> FLUID_DRIP =
            register("fluid_drip");
    public static final RegistryObject<SoundEvent> STITCH =
            register("stitch");
    public static final RegistryObject<SoundEvent> HEARTBEAT =
            register("heartbeat");
    public static final RegistryObject<SoundEvent> FLATLINE =
            register("flatline");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () ->
                SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(BioForgeMod.MODID, name)));
    }
}
