package com.bioforge.mod.cloning;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import com.bioforge.mod.network.SyncBioDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

/**
 * Cloning system for BioForge.
 *
 * Pipeline:
 *  1. Extract DNA from a living player/entity → DNA Sample item (with NBT)
 *  2. Place sample in Cloning Vat → starts growth timer (configurable, default 10 min)
 *  3. Clone spawns as a server-side entity with:
 *      - Same skin as donor (name tag based or stored texture hash)
 *      - Same blood type
 *      - Same limb state (including prosthetics)
 *      - Clone generation = donor.generation + 1
 *      - Optional: partial memory transfer if Memory Crystal was inserted
 *  4. Clone degradation: higher clone generation = more instability debuffs
 *
 * Players can be cloned from their own DNA; ethical roleplay is up to the server.
 */
public class CloningSystem {

    public static final int DEFAULT_GROWTH_TICKS = 12000; // 10 minutes
    public static final int MAX_STABLE_GENERATION = 5;    // gen 6+ = severe instability

    /**
     * Creates a DNA sample item from the target player.
     * Stores full BioData snapshot + skin info in item NBT.
     */
    public static ItemStack captureDna(Player target, Player sampler) {
        PlayerBioData bio = BioForgeCapabilities.get(target);

        // Generate or retrieve hash
        String hash = bio.getDnaHash();
        if (hash.isEmpty()) {
            hash = generateHash(target);
            bio.setDnaHash(hash);
        }

        ItemStack sample = new ItemStack(com.bioforge.mod.registry.ModItems.DNA_SAMPLE.get());
        CompoundTag tag = sample.getOrCreateTag();

        tag.putString("source_name", target.getName().getString());
        tag.putString("source_uuid", target.getUUID().toString());
        tag.putString("dna_hash", hash);
        tag.putString("blood_type", bio.getBloodType().name());
        tag.putInt("clone_generation", bio.getCloneGeneration());
        tag.putBoolean("has_memory", false);

        // Store limb configuration snapshot
        CompoundTag limbSnapshot = new CompoundTag();
        bio.getAllLimbs().forEach((slot, state) -> limbSnapshot.put(slot, state.toNBT()));
        tag.put("limb_snapshot", limbSnapshot);

        bio.addMedicalHistoryEntry("[T] DNA captured by " + sampler.getName().getString());

        sample.setHoverName(Component.literal("DNA Sample [" + target.getName().getString() + "]")
                .withStyle(ChatFormatting.AQUA));

        return sample;
    }

    /**
     * Attempts to spawn a clone entity from a DNA sample stack.
     * Called when Cloning Vat finishes its growth cycle.
     *
     * @param dnaSample  the DNA Sample item with full NBT
     * @param level      the server level to spawn in
     * @param spawnPos   where to place the clone
     * @param operator   who initiated the cloning (for logging)
     */
    public static void spawnClone(ItemStack dnaSample, ServerLevel level, Vec3 spawnPos, Player operator) {
        CompoundTag tag = dnaSample.getTag();
        if (tag == null) {
            operator.sendSystemMessage(Component.literal("[BioForge] DNA sample is corrupted — no data.").withStyle(ChatFormatting.RED));
            return;
        }

        String sourceName = tag.getString("source_name");
        String dnaHash    = tag.getString("dna_hash");
        int generation    = tag.getInt("clone_generation") + 1;
        boolean hasMemory = tag.getBoolean("has_memory");

        // Look up the original player if online
        ServerPlayer original = level.getServer().getPlayerList().getPlayerByName(sourceName);

        // Spawn a clone entity (represented as a special CloneEntity or a named player skeleton)
        CloneEntity clone = CloneEntity.create(level, dnaHash, sourceName, generation);
        clone.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        // Apply limb configuration from snapshot
        if (tag.contains("limb_snapshot")) {
            clone.applyLimbSnapshot(tag.getCompound("limb_snapshot"));
        }

        // Apply generation-based instability debuffs
        applyInstability(clone, generation);

        level.addFreshEntity(clone);

        // Broadcast FX to all nearby
        BioForgeNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        spawnPos.x, spawnPos.y, spawnPos.z, 32, level.dimension())),
                new PlayProcedureEffectPacket(
                        PlayProcedureEffectPacket.EffectType.CLONE_SPAWN,
                        spawnPos, clone.getId())
        );

        // If original player is online, notify them and record history
        if (original != null) {
            PlayerBioData originalBio = BioForgeCapabilities.get(original);
            originalBio.incrementCloneGeneration();
            originalBio.addMedicalHistoryEntry("[T] Clone (gen " + generation + ") spawned by " + operator.getName().getString());
            BioForgeNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> original),
                    new SyncBioDataPacket(originalBio)
            );
            original.sendSystemMessage(
                    Component.literal("[BioForge] A clone of you (gen " + generation + ") has been created.")
                            .withStyle(ChatFormatting.DARK_AQUA));
        }

        operator.sendSystemMessage(
                Component.literal("[BioForge] Clone of " + sourceName + " (gen " + generation + ") spawned.")
                        .withStyle(ChatFormatting.GREEN));
    }

    /**
     * Inserts a Memory Crystal into a DNA sample, enabling partial memory transfer.
     * Memory crystals must be charged by right-clicking on the original player.
     */
    public static boolean insertMemoryCrystal(ItemStack dnaSample, ItemStack memoryCrystal) {
        if (!memoryCrystal.is(com.bioforge.mod.registry.ModItems.MEMORY_CRYSTAL.get())) return false;
        CompoundTag tag = dnaSample.getOrCreateTag();

        String crystalOwner = memoryCrystal.getOrCreateTag().getString("owner_uuid");
        String sampleOwner  = tag.getString("source_uuid");

        if (!crystalOwner.equals(sampleOwner)) return false; // crystal must match the DNA owner

        tag.putBoolean("has_memory", true);
        return true;
    }

    /**
     * Applies instability debuffs that worsen with each clone generation.
     * Gen 1-3: mild; Gen 4-5: moderate; Gen 6+: severe
     */
    private static void applyInstability(CloneEntity clone, int generation) {
        if (generation <= 3) {
            clone.setInstabilityLevel(CloneEntity.Instability.MILD);
        } else if (generation <= 5) {
            clone.setInstabilityLevel(CloneEntity.Instability.MODERATE);
        } else {
            clone.setInstabilityLevel(CloneEntity.Instability.SEVERE);
        }
    }

    private static String generateHash(Player player) {
        return String.format("%016X%016X",
                player.getUUID().getMostSignificantBits(),
                player.getUUID().getLeastSignificantBits());
    }
}
