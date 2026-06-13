package com.bioforge.mod.bodyexit;

import com.bioforge.mod.registry.ModEntities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * BodyShellEntity — the inert physical body left behind when a player
 * uses astral projection / body-leave.
 *
 * Features:
 *  - Stores a copy of the owner player's UUID so they can return.
 *  - On death it drops all saved items (the player's inventory at exit time).
 *  - Renders using the Steve / Alex biped model with the player's skin texture.
 *  - The body is invisible to spectator-mode projection (client-side).
 *  - Sneak-rightclick by the owning player returns them to body.
 */
public class BodyShellEntity extends net.minecraft.world.entity.PathfinderMob {

    // ── Synced data ──────────────────────────────────────────────────────────
    private static final EntityDataAccessor<String> OWNER_UUID_STR =
            SynchedEntityData.defineId(BodyShellEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_TEXTURE_URL =
            SynchedEntityData.defineId(BodyShellEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM_MODEL =
            SynchedEntityData.defineId(BodyShellEntity.class, EntityDataSerializers.BOOLEAN);

    /** Saved inventory for drop-on-death. Set by BodyExitSystem at exit time. */
    private final List<ItemStack> savedInventory = new ArrayList<>();

    /** The player UUID this body belongs to. */
    private UUID ownerUuid = null;

    // ─────────────────────────────────────────────────────────────────────────

    public BodyShellEntity(EntityType<? extends BodyShellEntity> type, Level level) {
        super(type, level);
        this.setInvulnerable(false);
        this.noPhysics = false;
    }

    // ── EntityData ───────────────────────────────────────────────────────────

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(OWNER_UUID_STR, "");
        entityData.define(SKIN_TEXTURE_URL, "");
        entityData.define(SLIM_MODEL, false);
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder createAttributes() {
        return net.minecraft.world.entity.PathfinderMob.createMobAttributes()
                .add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH, 20.0)
                .add(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public UUID getOwnerUuid() { return ownerUuid; }

    public void setOwnerUuid(UUID uuid) {
        this.ownerUuid = uuid;
        entityData.set(OWNER_UUID_STR, uuid != null ? uuid.toString() : "");
    }

    public String getSkinTextureUrl() { return entityData.get(SKIN_TEXTURE_URL); }
    public void setSkinTextureUrl(String url) { entityData.set(SKIN_TEXTURE_URL, url); }

    public boolean isSlimModel() { return entityData.get(SLIM_MODEL); }
    public void setSlimModel(boolean slim) { entityData.set(SLIM_MODEL, slim); }

    /** Called by BodyExitSystem — stores a snapshot of the player's inventory. */
    public void setInventorySnapshot(List<ItemStack> items) {
        savedInventory.clear();
        for (ItemStack s : items) savedInventory.add(s.copy());
    }

    /** Returns the saved inventory for restoring on body re-entry. */
    public List<ItemStack> getSavedInventory() {
        return savedInventory;
    }

    // ── Behaviour — the body is inert and doesn't move ───────────────────────

    @Override
    public boolean isNoGravity() { return false; }

    @Override
    public boolean isPushable() { return false; }

    @Override
    public boolean isPickable() { return true; }

    /** No ambient AI — the body just lies there. */
    @Override
    public Iterable<ItemStack> getArmorSlots() { return List.of(); }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) { return ItemStack.EMPTY; }

    @Override
    public void setItemSlot(EquipmentSlot slot, ItemStack stack) {}

    @Override
    public HumanoidArm getMainArm() { return HumanoidArm.RIGHT; }

    // ── Interactions ─────────────────────────────────────────────────────────

    @Override
    protected net.minecraft.world.InteractionResult mobInteract(Player player, net.minecraft.world.InteractionHand hand) {
        if (!level().isClientSide && player instanceof ServerPlayer sp) {
            // Only the owning player can re-enter their body
            if (ownerUuid != null && ownerUuid.equals(player.getUUID())) {
                BodyExitSystem.returnToBody(sp, this);
                return net.minecraft.world.InteractionResult.SUCCESS;
            } else {
                player.sendSystemMessage(Component.literal("§c[BioForge] This body is occupied."));
            }
        }
        return net.minecraft.world.InteractionResult.PASS;
    }

    // ── Death — drop saved inventory ─────────────────────────────────────────

    @Override
    public void die(DamageSource cause) {
        super.die(cause);
        if (!level().isClientSide) {
            // Drop all saved inventory items
            for (ItemStack stack : savedInventory) {
                if (!stack.isEmpty()) {
                    spawnAtLocation(stack);
                }
            }
            savedInventory.clear();

            // Notify server — the owner's body was killed; they die too
            if (ownerUuid != null && level() instanceof net.minecraft.server.level.ServerLevel sl) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUuid);
                if (owner != null && BodyExitSystem.isProjecting(owner)) {
                    // Force the player back in a dead state
                    BodyExitSystem.killProjectingPlayer(owner, cause);
                }
            }
        }
    }

    // ── Custom display name ───────────────────────────────────────────────────

    @Override
    public Component getDisplayName() {
        if (ownerUuid != null && !entityData.get(OWNER_UUID_STR).isEmpty()) {
            // Try to get player name
            if (level() instanceof net.minecraft.server.level.ServerLevel sl) {
                ServerPlayer owner = sl.getServer().getPlayerList().getPlayer(ownerUuid);
                if (owner != null) {
                    return Component.literal("§7" + owner.getName().getString() + "'s Body");
                }
            }
        }
        return Component.literal("§7Abandoned Body");
    }

    @Override
    public boolean shouldShowName() { return true; }

    // ── NBT ──────────────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (ownerUuid != null) tag.putUUID("owner_uuid", ownerUuid);
        tag.putString("skin_url", getSkinTextureUrl());
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
        if (tag.contains("skin_url")) entityData.set(SKIN_TEXTURE_URL, tag.getString("skin_url"));
        if (tag.contains("slim_model")) entityData.set(SLIM_MODEL, tag.getBoolean("slim_model"));

        if (tag.contains("saved_inventory", Tag.TAG_LIST)) {
            ListTag invTag = tag.getList("saved_inventory", Tag.TAG_COMPOUND);
            savedInventory.clear();
            for (int i = 0; i < invTag.size(); i++) {
                savedInventory.add(ItemStack.of(invTag.getCompound(i)));
            }
        }
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getDeathSound() {
        return net.minecraft.sounds.SoundEvents.PLAYER_DEATH;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource src) {
        return net.minecraft.sounds.SoundEvents.PLAYER_HURT;
    }

    @Override
    protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
}
