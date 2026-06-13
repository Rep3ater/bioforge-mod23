package com.bioforge.mod.cloning;

import com.bioforge.mod.core.LimbState;
import com.bioforge.mod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

/**
 * CloneEntity — a biological copy of a player spawned by the Cloning Vat.
 *
 * Uses the standard (EntityType, Level) constructor only.
 * DNA/name/generation are set via NBT helpers after construction.
 * This avoids the circular-reference crash from calling CLONE.get() inside
 * a constructor that runs during entity type registration.
 */
public class CloneEntity extends PathfinderMob {

    public enum Instability { STABLE, MILD, MODERATE, SEVERE }

    private static final EntityDataAccessor<String> DNA_HASH =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> DONOR_NAME =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> CLONE_GENERATION =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> INSTABILITY_ORDINAL =
            SynchedEntityData.defineId(CloneEntity.class, EntityDataSerializers.INT);

    private final Map<String, LimbState> limbStates = new HashMap<>();
    private int instabilityTick = 0;

    /** Standard constructor — required by EntityType.Builder */
    public CloneEntity(EntityType<? extends CloneEntity> type, Level level) {
        super(type, level);
    }

    /**
     * Factory method used by CloningSystem.
     * Avoids touching ModEntities.CLONE inside the constructor body.
     */
    public static CloneEntity create(Level level, String dnaHash, String donorName, int generation) {
        CloneEntity entity = ModEntities.CLONE.get().create(level);
        if (entity == null) throw new IllegalStateException("[BioForge] Failed to create CloneEntity — type not registered?");
        entity.entityData.set(DNA_HASH, dnaHash);
        entity.entityData.set(DONOR_NAME, donorName);
        entity.entityData.set(CLONE_GENERATION, generation);
        entity.entityData.set(INSTABILITY_ORDINAL, Instability.MILD.ordinal());
        return entity;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(DNA_HASH, "");
        entityData.define(DONOR_NAME, "Clone");
        entityData.define(CLONE_GENERATION, 1);
        entityData.define(INSTABILITY_ORDINAL, Instability.MILD.ordinal());
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(4, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8.0f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) tickInstability();
    }

    private void tickInstability() {
        instabilityTick++;
        Instability inst = getInstabilityLevel();
        int interval = switch (inst) {
            case STABLE -> 0;
            case MILD -> 200;
            case MODERATE -> 100;
            case SEVERE -> 40;
        };
        if (interval > 0 && instabilityTick % interval == 0) {
            switch (inst) {
                case MILD ->
                    addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                case MODERATE -> {
                    addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 1));
                    addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
                }
                case SEVERE -> {
                    hurt(damageSources().generic(), 1.0f);
                    addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 2));
                    addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
                }
                default -> {}
            }
        }
    }

    public void applyLimbSnapshot(CompoundTag limbSnapshot) {
        limbSnapshot.getAllKeys().forEach(slot ->
                limbStates.put(slot, LimbState.fromNBT(limbSnapshot.getCompound(slot))));
    }

    public Map<String, LimbState> getLimbStates() { return limbStates; }
    public String getDnaHash()       { return entityData.get(DNA_HASH); }
    public String getDonorName()     { return entityData.get(DONOR_NAME); }
    public int getCloneGeneration()  { return entityData.get(CLONE_GENERATION); }

    public Instability getInstabilityLevel() {
        int ord = entityData.get(INSTABILITY_ORDINAL);
        Instability[] vals = Instability.values();
        return (ord >= 0 && ord < vals.length) ? vals[ord] : Instability.MILD;
    }

    public void setInstabilityLevel(Instability lvl) {
        entityData.set(INSTABILITY_ORDINAL, lvl.ordinal());
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("dna_hash", getDnaHash());
        tag.putString("donor_name", getDonorName());
        tag.putInt("clone_generation", getCloneGeneration());
        tag.putInt("instability", entityData.get(INSTABILITY_ORDINAL));
        CompoundTag limbs = new CompoundTag();
        limbStates.forEach((slot, state) -> limbs.put(slot, state.toNBT()));
        tag.put("limbs", limbs);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(DNA_HASH, tag.getString("dna_hash"));
        entityData.set(DONOR_NAME, tag.getString("donor_name"));
        entityData.set(CLONE_GENERATION, tag.getInt("clone_generation"));
        entityData.set(INSTABILITY_ORDINAL, tag.getInt("instability"));
        if (tag.contains("limbs")) {
            CompoundTag limbs = tag.getCompound("limbs");
            limbs.getAllKeys().forEach(slot ->
                    limbStates.put(slot, LimbState.fromNBT(limbs.getCompound(slot))));
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.PLAYER_DEATH;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource src) {
        return net.minecraft.sounds.SoundEvents.PLAYER_HURT;
    }
}
