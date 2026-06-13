package com.bioforge.mod.client.fx;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.network.PlayProcedureEffectPacket.EffectType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

/**
 * Client-side effect dispatcher.
 * Each procedure type triggers a distinct combination of:
 *  - Particle bursts (blood, smoke, sparks, etc.)
 *  - Sound events
 *  - Screen flash/shake (via camera tilt — TODO: implement camera shake in next pass)
 *
 * All effects are purely cosmetic — no gameplay state changes here.
 * Called from PlayProcedureEffectPacket on the client thread.
 */
public class ProcedureFX {

    private static final Random RNG = new Random();

    public static void play(EffectType type, Vec3 pos, int entityId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Entity entity = mc.level.getEntity(entityId);

        switch (type) {
            case BLOOD_DRAW  -> playBloodDraw(mc, pos, entity);
            case INCISION    -> playIncision(mc, pos, entity);
            case BONE_SAW    -> playBoneSaw(mc, pos, entity);
            case CAUTERIZE   -> playCauterize(mc, pos, entity);
            case ORGAN_REMOVE-> playOrganRemove(mc, pos, entity);
            case ORGAN_IMPLANT -> playOrganImplant(mc, pos, entity);
            case LIMB_REMOVE -> playLimbRemove(mc, pos, entity);
            case LIMB_ATTACH -> playLimbAttach(mc, pos, entity);
            case CLONE_SPAWN -> playCloneSpawn(mc, pos, entity);
            case SEDATION    -> playSedation(mc, pos, entity);
            case TRANSFUSION -> playTransfusion(mc, pos, entity);
            case STITCH      -> playStitch(mc, pos, entity);
        }
    }

    // ─── Blood Draw ───────────────────────────────────────────────────────────

    private static void playBloodDraw(Minecraft mc, Vec3 pos, Entity entity) {
        spawnBloodParticles(mc, pos, 6, 0.15f);
        playSound(mc, pos, BioForgeSounds.NEEDLE_INSERT, 0.6f, 1.0f);
    }

    // ─── Incision ─────────────────────────────────────────────────────────────

    private static void playIncision(Minecraft mc, Vec3 pos, Entity entity) {
        spawnBloodParticles(mc, pos, 12, 0.2f);
        playSound(mc, pos, BioForgeSounds.SCALPEL_CUT, 0.8f, 1.0f);
    }

    // ─── Bone Saw ─────────────────────────────────────────────────────────────

    private static void playBoneSaw(Minecraft mc, Vec3 pos, Entity entity) {
        spawnBloodParticles(mc, pos, 20, 0.4f);
        // Bone dust particles (white)
        for (int i = 0; i < 10; i++) {
            mc.level.addParticle(ParticleTypes.POOF,
                    pos.x + RNG.nextGaussian() * 0.2,
                    pos.y + 0.8 + RNG.nextGaussian() * 0.1,
                    pos.z + RNG.nextGaussian() * 0.2,
                    RNG.nextGaussian() * 0.1, 0.05, RNG.nextGaussian() * 0.1);
        }
        playSound(mc, pos, BioForgeSounds.BONE_SAW, 1.0f, 0.9f + RNG.nextFloat() * 0.2f);
    }

    // ─── Cauterize ────────────────────────────────────────────────────────────

    private static void playCauterize(Minecraft mc, Vec3 pos, Entity entity) {
        // Smoke + fire
        for (int i = 0; i < 8; i++) {
            mc.level.addParticle(ParticleTypes.FLAME,
                    pos.x + RNG.nextGaussian() * 0.15,
                    pos.y + 0.8 + RNG.nextGaussian() * 0.1,
                    pos.z + RNG.nextGaussian() * 0.15,
                    0, 0.02, 0);
            mc.level.addParticle(ParticleTypes.SMOKE,
                    pos.x + RNG.nextGaussian() * 0.2,
                    pos.y + 1.0 + RNG.nextGaussian() * 0.1,
                    pos.z + RNG.nextGaussian() * 0.2,
                    0, 0.05, 0);
        }
        playSound(mc, pos, BioForgeSounds.CAUTERIZE, 0.9f, 1.0f);
    }

    // ─── Organ Remove ─────────────────────────────────────────────────────────

    private static void playOrganRemove(Minecraft mc, Vec3 pos, Entity entity) {
        spawnBloodParticles(mc, pos, 30, 0.5f);
        // Wet slap sound
        playSound(mc, pos, BioForgeSounds.ORGAN_REMOVE, 1.0f, 0.8f + RNG.nextFloat() * 0.3f);
    }

    // ─── Organ Implant ────────────────────────────────────────────────────────

    private static void playOrganImplant(Minecraft mc, Vec3 pos, Entity entity) {
        spawnBloodParticles(mc, pos, 15, 0.3f);
        // Heart-beat like thump
        for (int i = 0; i < 5; i++) {
            mc.level.addParticle(ParticleTypes.HEART,
                    pos.x + RNG.nextGaussian() * 0.3,
                    pos.y + 1.0 + RNG.nextGaussian() * 0.2,
                    pos.z + RNG.nextGaussian() * 0.3,
                    0, 0.05, 0);
        }
        playSound(mc, pos, BioForgeSounds.ORGAN_IMPLANT, 0.8f, 1.0f);
    }

    // ─── Limb Remove ──────────────────────────────────────────────────────────

    private static void playLimbRemove(Minecraft mc, Vec3 pos, Entity entity) {
        spawnBloodParticles(mc, pos, 40, 0.7f);
        playSound(mc, pos, BioForgeSounds.LIMB_REMOVE, 1.0f, 0.9f);
        playSound(mc, pos, BioForgeSounds.BONE_SAW, 0.7f, 0.8f);
    }

    // ─── Limb Attach ──────────────────────────────────────────────────────────

    private static void playLimbAttach(Minecraft mc, Vec3 pos, Entity entity) {
        // Sparks for cybernetic; blood for biological; creak for prosthetic
        for (int i = 0; i < 8; i++) {
            mc.level.addParticle(ParticleTypes.ELECTRIC_SPARK,
                    pos.x + RNG.nextGaussian() * 0.3,
                    pos.y + 1.0 + RNG.nextGaussian() * 0.2,
                    pos.z + RNG.nextGaussian() * 0.3,
                    RNG.nextGaussian() * 0.1, 0.05, RNG.nextGaussian() * 0.1);
        }
        spawnBloodParticles(mc, pos, 10, 0.2f);
        playSound(mc, pos, BioForgeSounds.LIMB_ATTACH, 0.9f, 1.0f);
    }

    // ─── Clone Spawn ──────────────────────────────────────────────────────────

    private static void playCloneSpawn(Minecraft mc, Vec3 pos, Entity entity) {
        // Large burst: portal + enchantment glint particles
        for (int i = 0; i < 40; i++) {
            mc.level.addParticle(ParticleTypes.PORTAL,
                    pos.x + RNG.nextGaussian() * 0.5,
                    pos.y + RNG.nextDouble() * 2.0,
                    pos.z + RNG.nextGaussian() * 0.5,
                    RNG.nextGaussian() * 0.3, 0.1, RNG.nextGaussian() * 0.3);
            mc.level.addParticle(ParticleTypes.ENCHANTED_HIT,
                    pos.x + RNG.nextGaussian() * 0.4,
                    pos.y + RNG.nextDouble() * 2.0,
                    pos.z + RNG.nextGaussian() * 0.4,
                    0, 0, 0);
        }
        playSound(mc, pos, BioForgeSounds.CLONE_SPAWN, 1.0f, 0.7f);
    }

    // ─── Sedation ─────────────────────────────────────────────────────────────

    private static void playSedation(Minecraft mc, Vec3 pos, Entity entity) {
        for (int i = 0; i < 8; i++) {
            mc.level.addParticle(ParticleTypes.MYCELIUM,
                    pos.x + RNG.nextGaussian() * 0.3,
                    pos.y + 1.5 + RNG.nextDouble() * 0.3,
                    pos.z + RNG.nextGaussian() * 0.3,
                    0, 0.01, 0);
        }
        playSound(mc, pos, BioForgeSounds.NEEDLE_INSERT, 0.5f, 0.8f);
    }

    // ─── Transfusion ──────────────────────────────────────────────────────────

    private static void playTransfusion(Minecraft mc, Vec3 pos, Entity entity) {
        spawnBloodParticles(mc, pos, 8, 0.2f);
        playSound(mc, pos, BioForgeSounds.FLUID_DRIP, 0.6f, 1.0f);
    }

    // ─── Stitch ───────────────────────────────────────────────────────────────

    private static void playStitch(Minecraft mc, Vec3 pos, Entity entity) {
        for (int i = 0; i < 5; i++) {
            mc.level.addParticle(ParticleTypes.COMPOSTER,
                    pos.x + RNG.nextGaussian() * 0.1,
                    pos.y + 0.8 + RNG.nextGaussian() * 0.1,
                    pos.z + RNG.nextGaussian() * 0.1,
                    0, 0.02, 0);
        }
        playSound(mc, pos, BioForgeSounds.STITCH, 0.7f, 1.0f);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static void spawnBloodParticles(Minecraft mc, Vec3 pos, int count, float spread) {
        // Blood: use dusty red color particles (DUST with red tint)
        for (int i = 0; i < count; i++) {
            mc.level.addParticle(
                    new net.minecraft.core.particles.DustParticleOptions(
                            new org.joml.Vector3f(0.75f, 0.05f, 0.05f), 0.8f),
                    pos.x + RNG.nextGaussian() * spread,
                    pos.y + 0.8 + RNG.nextGaussian() * spread * 0.5,
                    pos.z + RNG.nextGaussian() * spread,
                    RNG.nextGaussian() * 0.15,
                    RNG.nextDouble() * 0.2,
                    RNG.nextGaussian() * 0.15);
        }
    }

    private static void playSound(Minecraft mc, Vec3 pos, SoundEvent sound, float volume, float pitch) {
        if (mc.level != null && sound != null) {
            mc.level.playLocalSound(pos.x, pos.y, pos.z, sound,
                    SoundSource.PLAYERS, volume, pitch, false);
        }
    }
}
