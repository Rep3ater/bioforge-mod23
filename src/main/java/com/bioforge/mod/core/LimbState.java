package com.bioforge.mod.core;

import net.minecraft.nbt.CompoundTag;

/**
 * Represents the biological state of one limb slot (arm/leg).
 *
 * Types:
 *  NONE        - limb is absent (amputated, never grown)
 *  BIOLOGICAL  - natural flesh limb (native or transplanted)
 *  PROSTHETIC  - mechanical/wooden prosthetic
 *  CYBERNETIC  - enhanced cybernetic implant
 *
 * Each LimbState carries:
 *  - present: whether the slot has anything attached
 *  - type: what kind of limb
 *  - donorDna: if transplanted, whose DNA
 *  - healthPercent: 0..1, damaged limbs reduce effectiveness
 *  - modelOverride: optional resource location string for custom model
 */
public class LimbState {

    public enum LimbType { NONE, BIOLOGICAL, PROSTHETIC, CYBERNETIC }

    private boolean present;
    private LimbType type;
    private String donorDna;
    private float healthPercent;   // 0.0 .. 1.0
    private String modelOverride;  // "" = use default, else custom model RL

    // Stat modifiers applied by this limb
    private float speedModifier;       // negative = slower
    private float miningSpeedModifier;
    private float attackModifier;
    private float jumpModifier;

    private LimbState() {}

    public static LimbState naturalPresent() {
        LimbState s = new LimbState();
        s.present = true;
        s.type = LimbType.BIOLOGICAL;
        s.donorDna = "";
        s.healthPercent = 1.0f;
        s.modelOverride = "";
        s.speedModifier = 0f;
        s.miningSpeedModifier = 0f;
        s.attackModifier = 0f;
        s.jumpModifier = 0f;
        return s;
    }

    public static LimbState absent() {
        LimbState s = new LimbState();
        s.present = false;
        s.type = LimbType.NONE;
        s.donorDna = "";
        s.healthPercent = 0f;
        s.modelOverride = "";
        // Significant penalties for missing limbs
        s.speedModifier = -0.3f;
        s.miningSpeedModifier = -0.5f;
        s.attackModifier = -0.5f;
        s.jumpModifier = -0.3f;
        return s;
    }

    public static LimbState prosthetic(String modelOverride) {
        LimbState s = new LimbState();
        s.present = true;
        s.type = LimbType.PROSTHETIC;
        s.donorDna = "";
        s.healthPercent = 1.0f;
        s.modelOverride = modelOverride;
        s.speedModifier = -0.1f;
        s.miningSpeedModifier = 0f;
        s.attackModifier = -0.1f;
        s.jumpModifier = -0.1f;
        return s;
    }

    public static LimbState cybernetic(String modelOverride) {
        LimbState s = new LimbState();
        s.present = true;
        s.type = LimbType.CYBERNETIC;
        s.donorDna = "";
        s.healthPercent = 1.0f;
        s.modelOverride = modelOverride;
        // Slight bonuses
        s.speedModifier = 0.1f;
        s.miningSpeedModifier = 0.2f;
        s.attackModifier = 0.15f;
        s.jumpModifier = 0.1f;
        return s;
    }

    public static LimbState transplanted(String donorDna, String modelOverride) {
        LimbState s = naturalPresent();
        s.donorDna = donorDna;
        s.modelOverride = modelOverride;
        return s;
    }

    // ─── Getters / Setters ───────────────────────────────────────────────────

    public boolean isPresent() { return present; }
    public LimbType getType() { return type; }
    public String getDonorDna() { return donorDna; }
    public float getHealthPercent() { return healthPercent; }
    public String getModelOverride() { return modelOverride; }
    public float getSpeedModifier() { return speedModifier; }
    public float getMiningSpeedModifier() { return miningSpeedModifier; }
    public float getAttackModifier() { return attackModifier; }
    public float getJumpModifier() { return jumpModifier; }

    public void setHealthPercent(float hp) { this.healthPercent = Math.max(0, Math.min(1, hp)); }
    public void setModelOverride(String override) { this.modelOverride = override; }

    public boolean isTransplanted() {
        return present && type == LimbType.BIOLOGICAL && donorDna != null && !donorDna.isEmpty();
    }

    // ─── NBT ─────────────────────────────────────────────────────────────────

    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("present", present);
        tag.putString("type", type.name());
        tag.putString("donor_dna", donorDna != null ? donorDna : "");
        tag.putFloat("health", healthPercent);
        tag.putString("model_override", modelOverride != null ? modelOverride : "");
        tag.putFloat("speed_mod", speedModifier);
        tag.putFloat("mining_mod", miningSpeedModifier);
        tag.putFloat("attack_mod", attackModifier);
        tag.putFloat("jump_mod", jumpModifier);
        return tag;
    }

    public static LimbState fromNBT(CompoundTag tag) {
        LimbState s = new LimbState();
        s.present = tag.getBoolean("present");
        try { s.type = LimbType.valueOf(tag.getString("type")); }
        catch (Exception e) { s.type = LimbType.BIOLOGICAL; }
        s.donorDna = tag.getString("donor_dna");
        s.healthPercent = tag.getFloat("health");
        s.modelOverride = tag.getString("model_override");
        s.speedModifier = tag.getFloat("speed_mod");
        s.miningSpeedModifier = tag.getFloat("mining_mod");
        s.attackModifier = tag.getFloat("attack_mod");
        s.jumpModifier = tag.getFloat("jump_mod");
        return s;
    }

    @Override
    public String toString() {
        return String.format("LimbState{present=%b, type=%s, health=%.0f%%}", present, type, healthPercent * 100);
    }
}
