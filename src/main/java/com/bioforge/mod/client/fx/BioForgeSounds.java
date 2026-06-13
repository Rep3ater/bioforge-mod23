package com.bioforge.mod.client.fx;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Sound events for BioForge procedures.
 *
 * All sounds are defined in assets/bioforge/sounds.json.
 * Placeholder audio files ship with the mod; they can be replaced by
 * resource packs for custom sounds.
 *
 * Sounds registered here are referenced by ProcedureFX directly.
 */
public class BioForgeSounds {

    // These are direct SoundEvent references used by ProcedureFX.
    // They are populated by the registry object getters once registered.

    public static SoundEvent NEEDLE_INSERT   = null;
    public static SoundEvent SCALPEL_CUT     = null;
    public static SoundEvent BONE_SAW        = null;
    public static SoundEvent CAUTERIZE       = null;
    public static SoundEvent ORGAN_REMOVE    = null;
    public static SoundEvent ORGAN_IMPLANT   = null;
    public static SoundEvent LIMB_REMOVE     = null;
    public static SoundEvent LIMB_ATTACH     = null;
    public static SoundEvent CLONE_SPAWN     = null;
    public static SoundEvent FLUID_DRIP      = null;
    public static SoundEvent STITCH          = null;
    public static SoundEvent HEARTBEAT       = null;
    public static SoundEvent FLATLINE        = null;

    /**
     * Called during client setup to populate the static SoundEvent references
     * from the ModSounds registry (which runs before this point).
     */
    public static void init() {
        NEEDLE_INSERT = event("needle_insert");
        SCALPEL_CUT   = event("scalpel_cut");
        BONE_SAW      = event("bone_saw");
        CAUTERIZE     = event("cauterize");
        ORGAN_REMOVE  = event("organ_remove");
        ORGAN_IMPLANT = event("organ_implant");
        LIMB_REMOVE   = event("limb_remove");
        LIMB_ATTACH   = event("limb_attach");
        CLONE_SPAWN   = event("clone_spawn");
        FLUID_DRIP    = event("fluid_drip");
        STITCH        = event("stitch");
        HEARTBEAT     = event("heartbeat");
        FLATLINE      = event("flatline");
    }

    private static SoundEvent event(String name) {
        // Creates an unregistered SoundEvent for direct use
        return SoundEvent.createVariableRangeEvent(new ResourceLocation(BioForgeMod.MODID, name));
    }
}
