package com.bioforge.mod.procedures;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.BloodType;
import com.bioforge.mod.core.LimbState;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import com.bioforge.mod.network.SyncBioDataPacket;
import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

/**
 * Central registry for all BioForge surgical procedures.
 *
 * Each procedure is a named operation that:
 *  1. Validates preconditions (sedation, tool presence, blood volume, etc.)
 *  2. Applies server-side data changes via PlayerBioData
 *  3. Broadcasts visual FX packets to nearby players
 *  4. Syncs updated BioData back to the affected player
 *  5. Appends to the patient's medical history
 *
 * To add a custom procedure: call register() with your procedure ID and handler.
 */
public class ProcedureRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    @FunctionalInterface
    public interface ProcedureHandler {
        ProcedureResult apply(Player surgeon, Player patient, CompoundTag data, Level level);
    }

    private static final Map<String, ProcedureHandler> PROCEDURES = new HashMap<>();

    static {
        registerDefaults();
    }

    // ─── Registration ─────────────────────────────────────────────────────────

    public static void register(String id, ProcedureHandler handler) {
        PROCEDURES.put(id, handler);
    }

    private static void registerDefaults() {
        register("blood_draw",           ProcedureRegistry::bloodDraw);
        register("blood_transfusion",    ProcedureRegistry::bloodTransfusion);
        register("dna_sample",           ProcedureRegistry::dnaSample);
        register("limb_amputate",        ProcedureRegistry::limbAmputate);
        register("limb_attach_prosthetic", ProcedureRegistry::limbAttachProsthetic);
        register("limb_attach_biological", ProcedureRegistry::limbAttachBiological);
        register("limb_attach_cybernetic", ProcedureRegistry::limbAttachCybernetic);
        register("organ_remove",         ProcedureRegistry::organRemove);
        register("organ_transplant",     ProcedureRegistry::organTransplant);
        register("administer_sedation",  ProcedureRegistry::administerSedation);
        register("administer_morphine",  ProcedureRegistry::administerMorphine);
        register("cauterize_wound",      ProcedureRegistry::cauterizeWound);
        register("stitch_wound",         ProcedureRegistry::stitchWound);
        register("assign_patient_id",    ProcedureRegistry::assignPatientId);
    }

    // ─── Execution Entry Point ────────────────────────────────────────────────

    /**
     * Called from the network packet handler when a procedure request arrives.
     * Resolves the target entity, runs the procedure, and sends results.
     */
    public static void execute(String procedureId, Player surgeon, int targetEntityId, CompoundTag extraData) {
        Level level = surgeon.level();
        Entity target = level.getEntity(targetEntityId);

        if (!(target instanceof Player patient)) {
            surgeon.sendSystemMessage(Component.literal("[BioForge] Target is not a player.").withStyle(ChatFormatting.RED));
            return;
        }

        ProcedureHandler handler = PROCEDURES.get(procedureId);
        if (handler == null) {
            LOGGER.warn("[BioForge] Unknown procedure: {}", procedureId);
            return;
        }

        ProcedureResult result = handler.apply(surgeon, patient, extraData, level);
        handleResult(result, surgeon, patient);
    }

    private static void handleResult(ProcedureResult result, Player surgeon, Player patient) {
        if (result.success()) {
            // Sync bio data to patient's client
            if (patient instanceof ServerPlayer sp) {
                PlayerBioData data = BioForgeCapabilities.get(patient);
                BioForgeNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> sp),
                        new SyncBioDataPacket(data)
                );
            }
            surgeon.sendSystemMessage(Component.literal("[BioForge] " + result.message()).withStyle(ChatFormatting.GREEN));
        } else {
            surgeon.sendSystemMessage(Component.literal("[BioForge] " + result.message()).withStyle(ChatFormatting.RED));
        }

        // Broadcast FX to nearby players
        if (result.fxType() != null) {
            BioForgeNetwork.CHANNEL.send(
                    PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                            patient.getX(), patient.getY(), patient.getZ(), 32,
                            patient.level().dimension())),
                    new PlayProcedureEffectPacket(result.fxType(), patient.position(), patient.getId())
            );
        }
    }

    // ─── Procedure Implementations ────────────────────────────────────────────

    private static ProcedureResult bloodDraw(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);

        if (bio.getBloodVolume() < 0.5f) {
            return ProcedureResult.fail("Patient has insufficient blood volume.");
        }

        // Draw 0.45L (about 1 unit), mild debuff
        bio.removeBlood(0.45f);
        bio.addPain(0.5f);
        if (!bio.hasCondition("sedated")) {
            patient.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0));
        }

        bio.addMedicalHistoryEntry(timestamp() + " Blood draw performed by " + surgeon.getName().getString()
                + ". Volume drawn: 0.45L. Type: " + bio.getBloodType().getDisplay());

        return ProcedureResult.success("Blood draw complete. Sample collected.",
                PlayProcedureEffectPacket.EffectType.BLOOD_DRAW);
    }

    private static ProcedureResult bloodTransfusion(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);

        String donorTypeStr = data.getString("donor_blood_type");
        BloodType donorType = BloodType.fromString(donorTypeStr);
        float amount = data.contains("amount") ? data.getFloat("amount") : 0.45f;

        if (!bio.getBloodType().canReceiveFrom(donorType)) {
            // Incompatible transfusion — severe reaction
            patient.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1));
            patient.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 600, 2));
            bio.addCondition("transfusion_reaction");
            bio.addMedicalHistoryEntry(timestamp() + " INCOMPATIBLE TRANSFUSION. Donor: " + donorType.getDisplay()
                    + " Recipient: " + bio.getBloodType().getDisplay() + " — REACTION TRIGGERED.");
            return ProcedureResult.success("TRANSFUSION REACTION! Blood types incompatible.",
                    PlayProcedureEffectPacket.EffectType.TRANSFUSION);
        }

        bio.addBlood(amount);
        if (bio.getBloodVolume() > 3.5f) {
            patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        }
        bio.addMedicalHistoryEntry(timestamp() + " Transfusion: " + amount + "L of " + donorType.getDisplay()
                + " administered by " + surgeon.getName().getString());

        return ProcedureResult.success("Transfusion complete. " + amount + "L administered.",
                PlayProcedureEffectPacket.EffectType.TRANSFUSION);
    }

    private static ProcedureResult dnaSample(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);

        // Generate deterministic DNA hash from player UUID
        String hash = generateDnaHash(patient.getUUID());
        bio.setDnaHash(hash);

        bio.addMedicalHistoryEntry(timestamp() + " DNA sample collected by " + surgeon.getName().getString()
                + ". Hash: " + hash.substring(0, 8) + "...");

        return ProcedureResult.success("DNA sample collected. Hash: " + hash.substring(0, 8) + "...",
                PlayProcedureEffectPacket.EffectType.BLOOD_DRAW);
    }

    private static ProcedureResult limbAmputate(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        String slot = data.getString("slot"); // e.g. "left_arm"

        if (!isValidLimbSlot(slot)) return ProcedureResult.fail("Invalid limb slot: " + slot);
        if (!bio.hasLimb(slot)) return ProcedureResult.fail("Slot " + slot + " has no limb to remove.");
        if (!bio.isSedated() && !data.getBoolean("force")) {
            return ProcedureResult.fail("Patient must be sedated before amputation (or set force=true).");
        }

        LimbState removed = bio.getLimb(slot);
        bio.setLimb(slot, LimbState.absent());
        bio.removeBlood(0.8f);
        bio.addPain(8f);
        bio.addCondition("bleeding_" + slot);
        patient.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 2));
        patient.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 1));

        bio.addMedicalHistoryEntry(timestamp() + " Amputation of " + slot + " performed by "
                + surgeon.getName().getString() + ". Previous state: " + removed.getType().name());

        return ProcedureResult.success("Amputation of " + slot + " complete.",
                PlayProcedureEffectPacket.EffectType.LIMB_REMOVE);
    }

    private static ProcedureResult limbAttachProsthetic(Player surgeon, Player patient, CompoundTag data, Level level) {
        return attachLimb(surgeon, patient, data, "prosthetic", PlayProcedureEffectPacket.EffectType.LIMB_ATTACH);
    }

    private static ProcedureResult limbAttachBiological(Player surgeon, Player patient, CompoundTag data, Level level) {
        return attachLimb(surgeon, patient, data, "biological", PlayProcedureEffectPacket.EffectType.LIMB_ATTACH);
    }

    private static ProcedureResult limbAttachCybernetic(Player surgeon, Player patient, CompoundTag data, Level level) {
        return attachLimb(surgeon, patient, data, "cybernetic", PlayProcedureEffectPacket.EffectType.LIMB_ATTACH);
    }

    private static ProcedureResult attachLimb(Player surgeon, Player patient, CompoundTag data, String limbTypeStr, PlayProcedureEffectPacket.EffectType fx) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        String slot = data.getString("slot");
        String modelOverride = data.getString("model_override"); // "" = default

        if (!isValidLimbSlot(slot)) return ProcedureResult.fail("Invalid limb slot: " + slot);
        if (bio.hasLimb(slot)) return ProcedureResult.fail("Slot " + slot + " already occupied. Amputate first.");

        LimbState newLimb = switch (limbTypeStr) {
            case "prosthetic" -> LimbState.prosthetic(modelOverride);
            case "cybernetic" -> LimbState.cybernetic(modelOverride);
            default -> {
                String donorDna = data.getString("donor_dna");
                yield LimbState.transplanted(donorDna, modelOverride);
            }
        };

        bio.setLimb(slot, newLimb);
        bio.addPain(3f);
        bio.addMedicalHistoryEntry(timestamp() + " " + limbTypeStr + " limb attached to " + slot
                + " by " + surgeon.getName().getString()
                + (modelOverride.isEmpty() ? "" : " [custom model: " + modelOverride + "]"));

        return ProcedureResult.success(limbTypeStr + " limb attached to " + slot + ".", fx);
    }

    private static ProcedureResult organRemove(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        String slot = data.getString("slot"); // e.g. "heart", "liver"

        if (!bio.isSedated()) return ProcedureResult.fail("Patient must be sedated for organ removal.");

        bio.removeBlood(0.5f);
        bio.addPain(6f);
        bio.addCondition("open_cavity_" + slot);
        bio.addMedicalHistoryEntry(timestamp() + " Organ removed from slot: " + slot
                + " by " + surgeon.getName().getString());

        return ProcedureResult.success("Organ removed from " + slot + ".",
                PlayProcedureEffectPacket.EffectType.ORGAN_REMOVE);
    }

    private static ProcedureResult organTransplant(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        String slot = data.getString("slot");
        String donorDna = data.getString("donor_dna");

        if (!bio.isSedated()) return ProcedureResult.fail("Patient must be sedated for organ transplant.");

        bio.transplantOrgan(slot, donorDna);
        bio.removeCondition("open_cavity_" + slot);
        bio.removeBlood(0.3f);
        bio.addPain(4f);
        bio.addCondition("transplant_recovery");

        bio.addMedicalHistoryEntry(timestamp() + " Organ transplanted into slot: " + slot
                + ". Donor DNA: " + (donorDna.isEmpty() ? "synthetic" : donorDna.substring(0, 8) + "...")
                + ". Surgeon: " + surgeon.getName().getString());

        return ProcedureResult.success("Organ transplanted into " + slot + ". Monitor for rejection.",
                PlayProcedureEffectPacket.EffectType.ORGAN_IMPLANT);
    }

    private static ProcedureResult administerSedation(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        int duration = data.contains("duration_ticks") ? data.getInt("duration_ticks") : 2400; // 2 min default

        bio.setSedated(true, duration);
        bio.addCondition("sedated");
        patient.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
        patient.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 3));
        patient.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 10));

        bio.addMedicalHistoryEntry(timestamp() + " Anesthetic administered by "
                + surgeon.getName().getString() + ". Duration: " + (duration / 20) + "s");

        return ProcedureResult.success("Anesthetic administered. Patient sedated for " + (duration / 20) + "s.",
                PlayProcedureEffectPacket.EffectType.SEDATION);
    }

    private static ProcedureResult administerMorphine(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        bio.reducePain(5f);
        bio.addCondition("on_morphine");
        patient.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
        patient.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
        bio.addMedicalHistoryEntry(timestamp() + " Morphine administered by " + surgeon.getName().getString());
        return ProcedureResult.success("Morphine administered. Pain reduced.", null);
    }

    private static ProcedureResult cauterizeWound(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        String slot = data.getString("slot");
        bio.removeCondition("bleeding_" + slot);
        bio.addPain(3f);
        patient.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0));
        bio.addMedicalHistoryEntry(timestamp() + " Wound cauterized at " + slot + " by " + surgeon.getName().getString());
        return ProcedureResult.success("Wound cauterized at " + slot + ".",
                PlayProcedureEffectPacket.EffectType.CAUTERIZE);
    }

    private static ProcedureResult stitchWound(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        String slot = data.getString("slot");
        bio.removeCondition("bleeding_" + slot);
        bio.removeCondition("open_cavity_" + slot);
        bio.addPain(1.5f);
        patient.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 400, 0));
        bio.addMedicalHistoryEntry(timestamp() + " Sutures applied at " + slot + " by " + surgeon.getName().getString());
        return ProcedureResult.success("Wound stitched at " + slot + ".",
                PlayProcedureEffectPacket.EffectType.STITCH);
    }

    private static ProcedureResult assignPatientId(Player surgeon, Player patient, CompoundTag data, Level level) {
        PlayerBioData bio = BioForgeCapabilities.get(patient);
        String id = data.getString("patient_id");
        if (id.isEmpty()) {
            id = "PAT-" + Integer.toHexString(patient.getUUID().hashCode()).toUpperCase().substring(0, 6);
        }
        bio.setPatientId(id);
        bio.addMedicalHistoryEntry(timestamp() + " Patient ID assigned: " + id + " by " + surgeon.getName().getString());
        return ProcedureResult.success("Patient ID set to: " + id, null);
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private static boolean isValidLimbSlot(String slot) {
        return slot != null && (slot.equals("left_arm") || slot.equals("right_arm")
                || slot.equals("left_leg") || slot.equals("right_leg"));
    }

    private static String generateDnaHash(UUID uuid) {
        // Deterministic hex hash from UUID — stable per player
        long h1 = uuid.getMostSignificantBits();
        long h2 = uuid.getLeastSignificantBits();
        return String.format("%016X%016X", h1, h2);
    }

    private static String timestamp() {
        // Uses in-game time; real timestamps would need server time
        return "[T]";
    }
}
