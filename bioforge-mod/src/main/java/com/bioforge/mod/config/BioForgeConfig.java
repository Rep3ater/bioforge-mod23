package com.bioforge.mod.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * BioForge configuration.
 * Common config is synced server → client and governs gameplay behaviour.
 * Client config governs HUD and visual-only settings.
 */
public class BioForgeConfig {

    // ─── Common (gameplay) ────────────────────────────────────────────────────

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_SEDATION_FOR_SURGERY;
    public static final ForgeConfigSpec.BooleanValue REQUIRE_SEDATION_FOR_AMPUTATION;
    public static final ForgeConfigSpec.BooleanValue BLOOD_SYSTEM_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ORGAN_REJECTION_ENABLED;
    public static final ForgeConfigSpec.BooleanValue CLONE_INSTABILITY_ENABLED;
    public static final ForgeConfigSpec.IntValue CLONE_GROWTH_TICKS;
    public static final ForgeConfigSpec.IntValue MAX_STABLE_CLONE_GENERATION;
    public static final ForgeConfigSpec.DoubleValue BLEEDING_DAMAGE_PER_SECOND;
    public static final ForgeConfigSpec.BooleanValue PAIN_EFFECTS_ENABLED;
    public static final ForgeConfigSpec.BooleanValue ROLEPLAY_CONSENT_REQUIRED;
    public static final ForgeConfigSpec.BooleanValue SHOW_BLOOD_TYPE_ON_DEATH;
    public static final ForgeConfigSpec.BooleanValue PRESERVE_LIMBS_ON_DEATH;

    // ─── Client (visual) ─────────────────────────────────────────────────────

    public static final ForgeConfigSpec CLIENT_SPEC;
    public static final ForgeConfigSpec.BooleanValue SHOW_VERSION_ON_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_BLOOD_VOLUME_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_PAIN_LEVEL_HUD;
    public static final ForgeConfigSpec.BooleanValue SHOW_LIMB_STATUS_HUD;
    public static final ForgeConfigSpec.BooleanValue BLOOD_PARTICLES_ENABLED;
    public static final ForgeConfigSpec.BooleanValue SURGICAL_AMBIENT_SOUNDS;
    public static final ForgeConfigSpec.IntValue HUD_X_OFFSET;
    public static final ForgeConfigSpec.IntValue HUD_Y_OFFSET;

    static {
        // ─── Common ──────────────────────────────────────────────────────────
        ForgeConfigSpec.Builder common = new ForgeConfigSpec.Builder();

        common.comment("BioForge Common Configuration").push("gameplay");

        REQUIRE_SEDATION_FOR_SURGERY = common
                .comment("Require the patient to be sedated before most surgical procedures.")
                .define("requireSedationForSurgery", true);

        REQUIRE_SEDATION_FOR_AMPUTATION = common
                .comment("Require the patient to be sedated before amputation.")
                .define("requireSedationForAmputation", true);

        BLOOD_SYSTEM_ENABLED = common
                .comment("Enable the blood volume/type system (transfusions, bleeding, etc).")
                .define("bloodSystemEnabled", true);

        ORGAN_REJECTION_ENABLED = common
                .comment("Enable organ rejection mechanic for transplanted organs.")
                .define("organRejectionEnabled", true);

        CLONE_INSTABILITY_ENABLED = common
                .comment("Enable instability debuffs for higher-generation clones.")
                .define("cloneInstabilityEnabled", true);

        CLONE_GROWTH_TICKS = common
                .comment("How many ticks the Cloning Vat takes to mature a clone (default 12000 = 10 min).")
                .defineInRange("cloneGrowthTicks", 12000, 200, 144000);

        MAX_STABLE_CLONE_GENERATION = common
                .comment("Clone generations at or below this value have no instability debuffs.")
                .defineInRange("maxStableCloneGeneration", 3, 1, 20);

        BLEEDING_DAMAGE_PER_SECOND = common
                .comment("How much damage per second is dealt at critically low blood volume.")
                .defineInRange("bleedingDamagePerSecond", 2.0, 0.0, 20.0);

        PAIN_EFFECTS_ENABLED = common
                .comment("Enable pain level mechanics (nausea, weakness at high pain).")
                .define("painEffectsEnabled", true);

        ROLEPLAY_CONSENT_REQUIRED = common
                .comment("If true, the target player must /confirm before surgery begins.")
                .define("roleplayConsentRequired", false);

        SHOW_BLOOD_TYPE_ON_DEATH = common
                .comment("Show the patient's blood type in death message (if blood system on).")
                .define("showBloodTypeOnDeath", true);

        PRESERVE_LIMBS_ON_DEATH = common
                .comment("Preserve limb state (prosthetics, missing limbs) across death. True = realistic; False = respawn with full limbs.")
                .define("preserveLimbsOnDeath", true);

        common.pop();
        COMMON_SPEC = common.build();

        // ─── Client ──────────────────────────────────────────────────────────
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.comment("BioForge Client Configuration").push("hud");

        SHOW_VERSION_ON_HUD = client
                .comment("Show the BioForge version string in the top-left corner of the HUD.")
                .define("showVersionOnHud", true);

        SHOW_BLOOD_VOLUME_HUD = client
                .comment("Show blood volume bar on HUD.")
                .define("showBloodVolumeHud", true);

        SHOW_PAIN_LEVEL_HUD = client
                .comment("Show pain level indicator on HUD.")
                .define("showPainLevelHud", true);

        SHOW_LIMB_STATUS_HUD = client
                .comment("Show small limb status icons on HUD.")
                .define("showLimbStatusHud", true);

        BLOOD_PARTICLES_ENABLED = client
                .comment("Enable blood splatter particles during surgical procedures.")
                .define("bloodParticlesEnabled", true);

        SURGICAL_AMBIENT_SOUNDS = client
                .comment("Play ambient surgical sounds in the operating area.")
                .define("surgicalAmbientSounds", true);

        HUD_X_OFFSET = client
                .comment("X offset of the BioForge HUD elements from the screen edge.")
                .defineInRange("hudXOffset", 5, 0, 500);

        HUD_Y_OFFSET = client
                .comment("Y offset of the BioForge HUD elements from the screen edge.")
                .defineInRange("hudYOffset", 5, 0, 500);

        client.pop();
        CLIENT_SPEC = client.build();
    }
}
