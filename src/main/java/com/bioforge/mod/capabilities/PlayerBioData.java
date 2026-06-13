package com.bioforge.mod.capabilities;

import com.bioforge.mod.core.LimbState;
import com.bioforge.mod.core.BloodType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.*;

/**
 * Stores ALL biological data for a player or NPC.
 *
 * ─── Data sections ───────────────────────────────────────────────────────────
 *  Limbs:        which limbs are present, their type (bio/prosthetic/cybernetic)
 *  Blood:        blood type, current blood volume, last donors
 *  Organs:       which organs are transplanted, rejection risk
 *  DNA:          player's unique DNA hash, clone generation count
 *  Medical:      conditions, ongoing effects (sedated, bleeding, etc.)
 *  Roleplay:     patient ID, medical history log entries
 * ─────────────────────────────────────────────────────────────────────────────
 */
public class PlayerBioData {

    // Limb states — keyed by LimbSlot name
    private final Map<String, LimbState> limbs = new LinkedHashMap<>();

    // Blood system
    private BloodType bloodType = BloodType.UNKNOWN;
    private float bloodVolume = 5.0f;      // litres; 5.0 = full health
    private float maxBloodVolume = 5.0f;

    // DNA / Cloning
    private String dnaHash = "";
    private int cloneGeneration = 0;       // 0 = original, 1 = first clone, etc.
    private boolean hasDnaOnFile = false;

    // Organs (transplanted; null = native/removed)
    private final Map<String, String> organOwners = new LinkedHashMap<>(); // slot -> donor dna hash
    private final Map<String, Float> organRejection = new LinkedHashMap<>(); // slot -> 0..1

    // Medical conditions (active effect IDs)
    private final Set<String> activeConditions = new LinkedHashSet<>();

    // Sedation state
    private boolean sedated = false;
    private int sedationTicksRemaining = 0;

    // Pain level 0..10
    private float painLevel = 0f;

    // Roleplay data
    private String patientId = "";
    private final List<String> medicalHistory = new ArrayList<>();

    // ─── Limbs ───────────────────────────────────────────────────────────────

    public void initDefaultLimbs() {
        for (String slot : new String[]{"left_arm", "right_arm", "left_leg", "right_leg"}) {
            limbs.put(slot, LimbState.naturalPresent());
        }
    }

    public LimbState getLimb(String slot) {
        return limbs.getOrDefault(slot, LimbState.naturalPresent());
    }

    public void setLimb(String slot, LimbState state) {
        limbs.put(slot, state);
    }

    public boolean hasLimb(String slot) {
        LimbState s = limbs.get(slot);
        return s != null && s.isPresent();
    }

    public Map<String, LimbState> getAllLimbs() {
        return Collections.unmodifiableMap(limbs);
    }

    // ─── Blood ───────────────────────────────────────────────────────────────

    public BloodType getBloodType() { return bloodType; }
    public void setBloodType(BloodType bt) { this.bloodType = bt; }

    public float getBloodVolume() { return bloodVolume; }
    public float getMaxBloodVolume() { return maxBloodVolume; }

    public void addBlood(float amount) {
        bloodVolume = Math.min(maxBloodVolume, bloodVolume + amount);
    }

    public void removeBlood(float amount) {
        bloodVolume = Math.max(0f, bloodVolume - amount);
    }

    public float getBloodPercent() {
        return maxBloodVolume > 0 ? bloodVolume / maxBloodVolume : 0;
    }

    // ─── DNA ─────────────────────────────────────────────────────────────────

    public String getDnaHash() { return dnaHash; }
    public void setDnaHash(String hash) { this.dnaHash = hash; hasDnaOnFile = !hash.isEmpty(); }
    public boolean hasDnaOnFile() { return hasDnaOnFile; }
    public int getCloneGeneration() { return cloneGeneration; }
    public void incrementCloneGeneration() { cloneGeneration++; }

    // ─── Organs ──────────────────────────────────────────────────────────────

    public void transplantOrgan(String slot, String donorDna) {
        organOwners.put(slot, donorDna);
        // Rejection chance higher for mismatched blood types
        organRejection.put(slot, donorDna.isEmpty() ? 0f : 0.15f);
    }

    public float getOrganRejection(String slot) {
        return organRejection.getOrDefault(slot, 0f);
    }

    public void tickOrganRejection() {
        for (Map.Entry<String, Float> e : organRejection.entrySet()) {
            // Slowly increase rejection unless on immunosuppressants
            if (!activeConditions.contains("immunosuppressed")) {
                float val = e.getValue() + 0.0001f;
                organRejection.put(e.getKey(), Math.min(1f, val));
            }
        }
    }

    // ─── Conditions ──────────────────────────────────────────────────────────

    public void addCondition(String condition) { activeConditions.add(condition); }
    public void removeCondition(String condition) { activeConditions.remove(condition); }
    public boolean hasCondition(String condition) { return activeConditions.contains(condition); }
    public Set<String> getActiveConditions() { return Collections.unmodifiableSet(activeConditions); }

    // ─── Sedation ────────────────────────────────────────────────────────────

    public boolean isSedated() { return sedated; }
    public void setSedated(boolean s, int ticks) { sedated = s; sedationTicksRemaining = ticks; }
    public void tickSedation() {
        if (sedated) {
            sedationTicksRemaining--;
            if (sedationTicksRemaining <= 0) sedated = false;
        }
    }
    public int getSedationTicksRemaining() { return sedationTicksRemaining; }

    // ─── Pain ────────────────────────────────────────────────────────────────

    public float getPainLevel() { return painLevel; }
    public void addPain(float amount) { painLevel = Math.min(10f, painLevel + amount); }
    public void reducePain(float amount) { painLevel = Math.max(0f, painLevel - amount); }

    // ─── Roleplay ────────────────────────────────────────────────────────────

    public String getPatientId() { return patientId; }
    public void setPatientId(String id) { this.patientId = id; }

    public List<String> getMedicalHistory() { return Collections.unmodifiableList(medicalHistory); }
    public void addMedicalHistoryEntry(String entry) {
        medicalHistory.add(entry);
        if (medicalHistory.size() > 100) medicalHistory.remove(0);
    }

    // ─── NBT Serialization ───────────────────────────────────────────────────

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();

        // Limbs
        CompoundTag limbTag = new CompoundTag();
        limbs.forEach((slot, state) -> limbTag.put(slot, state.toNBT()));
        tag.put("limbs", limbTag);

        // Blood
        tag.putString("blood_type", bloodType.name());
        tag.putFloat("blood_volume", bloodVolume);
        tag.putFloat("max_blood_volume", maxBloodVolume);

        // DNA
        tag.putString("dna_hash", dnaHash);
        tag.putInt("clone_gen", cloneGeneration);
        tag.putBoolean("dna_on_file", hasDnaOnFile);

        // Organs
        CompoundTag orgOwners = new CompoundTag();
        organOwners.forEach(orgOwners::putString);
        tag.put("organ_owners", orgOwners);

        CompoundTag orgRej = new CompoundTag();
        organRejection.forEach(orgRej::putFloat);
        tag.put("organ_rejection", orgRej);

        // Conditions
        ListTag condList = new ListTag();
        activeConditions.forEach(c -> condList.add(StringTag.valueOf(c)));
        tag.put("conditions", condList);

        // Sedation
        tag.putBoolean("sedated", sedated);
        tag.putInt("sedation_ticks", sedationTicksRemaining);

        // Pain
        tag.putFloat("pain", painLevel);

        // Roleplay
        tag.putString("patient_id", patientId);
        ListTag histList = new ListTag();
        medicalHistory.forEach(e -> histList.add(StringTag.valueOf(e)));
        tag.put("medical_history", histList);

        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        // Limbs
        if (tag.contains("limbs")) {
            CompoundTag limbTag = tag.getCompound("limbs");
            for (String slot : limbTag.getAllKeys()) {
                limbs.put(slot, LimbState.fromNBT(limbTag.getCompound(slot)));
            }
        }

        // Blood
        if (tag.contains("blood_type")) bloodType = BloodType.fromString(tag.getString("blood_type"));
        if (tag.contains("blood_volume")) bloodVolume = tag.getFloat("blood_volume");
        if (tag.contains("max_blood_volume")) maxBloodVolume = tag.getFloat("max_blood_volume");

        // DNA
        if (tag.contains("dna_hash")) { dnaHash = tag.getString("dna_hash"); }
        if (tag.contains("clone_gen")) cloneGeneration = tag.getInt("clone_gen");
        if (tag.contains("dna_on_file")) hasDnaOnFile = tag.getBoolean("dna_on_file");

        // Organs
        if (tag.contains("organ_owners")) {
            CompoundTag oo = tag.getCompound("organ_owners");
            oo.getAllKeys().forEach(k -> organOwners.put(k, oo.getString(k)));
        }
        if (tag.contains("organ_rejection")) {
            CompoundTag or = tag.getCompound("organ_rejection");
            or.getAllKeys().forEach(k -> organRejection.put(k, or.getFloat(k)));
        }

        // Conditions
        if (tag.contains("conditions")) {
            ListTag condList = tag.getList("conditions", Tag.TAG_STRING);
            condList.forEach(t -> activeConditions.add(t.getAsString()));
        }

        // Sedation
        if (tag.contains("sedated")) sedated = tag.getBoolean("sedated");
        if (tag.contains("sedation_ticks")) sedationTicksRemaining = tag.getInt("sedation_ticks");

        // Pain
        if (tag.contains("pain")) painLevel = tag.getFloat("pain");

        // Roleplay
        if (tag.contains("patient_id")) patientId = tag.getString("patient_id");
        if (tag.contains("medical_history")) {
            ListTag histList = tag.getList("medical_history", Tag.TAG_STRING);
            histList.forEach(t -> medicalHistory.add(t.getAsString()));
        }
    }
}
