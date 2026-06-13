package com.bioforge.mod.bodyexit;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BodyShellEntity — the inert physical body left behind during astral projection.
 *
 * - Stores the owning player's UUID for re-entry validation.
 * - Holds a full inventory snapshot that drops on death.
 * - Kills the projecting player if the shell is destroyed.
 * - Renders with the player's actual Minecraft skin via BodyShellRenderer.
 * - Extends PathfinderMob so mobInteract / getAmbientSound are valid.
 */
public class BodyShellEntity extends PathfinderMob {

    // ── Synced data ──────────────────────────────────────────────────────────
    private static final EntityDataAccessor<String> OWNER_UUID_STR =
            SynchedEntityData.defineId(BodyShellEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM_MODEL =
            SynchedEntityData.defineId(BodyShellEntity.class, EntityDataSerializers.BOOLEAN);

    private final List<ItemStack> savedInventory = new ArrayList<>();
    @Nullable private UUID ownerUuid = null;

    // ─────────────────────────────────────────────────────────────────────────

    public BodyShellEntity(EntityType<? extends BodyShellEntity> type, Level level) {
        super(type, level);
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)       // completely immobile
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0); // can't be knocked around
    }

    // ── Synced data registration ──────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_UUID_STR, "");
        entityData.define(SLIM_MODEL, false);
    }

    // ── No AI — the body is inert ─────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        // No goals — body does not move or act
    }

    @Override
    @Nullable
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        MobSpawnType reason, @Nullable SpawnGroupData data,
                                        @Nullable CompoundTag tag) {
        // Skip standard mob initialization (loot table, equipment, etc.)
        return data;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    @Nullable
    public UUID getOwnerUuid() { return ownerUuid; }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
        entityData.set(OWNER_UUID_STR, uuid != null ? uuid.toString() : "");
    }

    public boolean isSlimModel() { return entityData.get(SLIM_MODEL); }
    public void setSlimModel(boolean slim) { entityData.set(SLIM_MODEL, slim); }

    public void setInventorySnapshot(List<ItemStack> items) {
        savedInventory.clear();
        for (ItemStack s : items) {
            if (!s.isEmpty()) savedInventory.add(s.copy());
        }
    }

    public List<ItemStack> getSavedInventory() { return savedInventory; }

    // ── Physics / movement ────────────────────────────────────────────────────

    @Override public boolean isNoGravity() { return false; }
    @Override public boolean isPushable()  { return false; }
    @Override public boolean isPickable()  { return true; }

    // ── Equipment stubs (required by LivingEntity / Mob) ─────────────────────

    @Override public Iterable<ItemStack> getArmorSlots() { return List.of(); }
    @Override public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }
    @Override public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}
    @Override public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    // ── Interaction: right-clicking the body re-enters it ────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            if (ownerUuid != null && ownerUuid.equals(sp.getUUID())) {
                BodyExitSystem.returnToBody(sp, this);
                return InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("§c[BioForge] This body belongs to someone else."));
            }
        }
        return InteractionResult.PASS;
    }

    // ── Death: drop items, kill the projecting player ─────────────────────────

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level().isClientSide) {
            for (ItemStack stack : savedInventory) {
                spawnAtLocation(stack.copy());
            }
            savedInventory.clear();

            if (ownerUuid != null && level() instanceof ServerLevel sl) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUuid);
                if (owner != null && BodyExitSystem.isProjecting(owner)) {
                    BodyExitSystem.killProjectingPlayer(owner, cause);
                }
            }
        }
    }

    // ── Display name ──────────────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        if (ownerUuid != null && level() instanceof ServerLevel sl) {
            ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUuid);
            if (owner != null) {
                return Component.literal("§7" + owner.getName().getString() + "'s Body");
            }
        }
        return Component.literal("§7Abandoned Body");
    }

    @Override public boolean shouldShowName() { return true; }

    // ── Sounds ────────────────────────────────────────────────────────────────

    @Override @Nullable protected SoundEvent getAmbientSound() { return null; }
    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }
    @Override protected SoundEvent getHurtSound(DamageSource src) { return SoundEvents.PLAYER_HURT; }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putUUID("owner_uuid", ownerUuid);
        tag.putBoolean("slim_model", isSlimModel());

        ListTag invTag = new ListTag();
        for (ItemStack s : savedInventory) {
            CompoundTag it = new CompoundTag();
            s.save(it);
            invTag.add(it);
        }
        tag.put("saved_inventory", invTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.hasUUID("owner_uuid")) {
            ownerUuid = tag.getUUID("owner_uuid");
            entityData.set(OWNER_UUID_STR, ownerUuid.toString());
        }
        if (tag.contains("slim_model")) {
            entityData.set(SLIM_MODEL, tag.getBoolean("slim_model"));
        }
        if (tag.contains("saved_inventory", Tag.TAG_LIST)) {
            ListTag invTag = tag.getList("saved_inventory", Tag.TAG_COMPOUND);
            savedInventory.clear();
            for (int i = 0; i < invTag.size(); i++) {
                ItemStack s = ItemStack.of(invTag.getCompound(i));
                if (!s.isEmpty()) savedInventory.add(s);
            }
        }
    }
}
