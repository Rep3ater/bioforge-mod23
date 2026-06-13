package com.bioforge.mod.core;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.SyncBioDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Server-side event handler for BioForge gameplay systems.
 *
 * Handles:
 *  - Per-tick updates: sedation countdown, bleeding damage, pain decay, organ rejection
 *  - Player login: sync bio data to client, init defaults
 *  - Player respawn: preserve/clear bio data per config
 *  - Attribute modifiers: apply limb-based movement/attack penalties
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID)
public class BioForgeEvents {

    private static final int TICK_INTERVAL = 20; // process every second

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;
        if (player.level().isClientSide) return;
        if (player.tickCount % TICK_INTERVAL != 0) return;

        PlayerBioData bio = BioForgeCapabilities.get(player);

        // ── Sedation countdown ───────────────────────────────────────────────
        if (bio.isSedated()) {
            bio.tickSedation();
            if (!bio.isSedated()) {
                bio.removeCondition("sedated");
            }
        }

        // ── Bleeding damage ──────────────────────────────────────────────────
        boolean anyBleeding = bio.getActiveConditions().stream()
                .anyMatch(c -> c.startsWith("bleeding_"));
        if (anyBleeding) {
            bio.removeBlood(0.05f); // slow bleed per second
            if (bio.getBloodVolume() < 1.5f) {
                // Hemorrhagic shock
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.WEAKNESS, 25, 2));
            }
            if (bio.getBloodVolume() <= 0.2f) {
                // Critical — deal real damage
                player.hurt(player.damageSources().generic(), 2.0f);
            }
        }

        // ── Slow blood regeneration (no bleeding, high volume) ───────────────
        if (!anyBleeding && bio.getBloodVolume() < bio.getMaxBloodVolume()) {
            bio.addBlood(0.01f);
        }

        // ── Pain decay ───────────────────────────────────────────────────────
        if (bio.getPainLevel() > 0) {
            bio.reducePain(0.1f);
            // High pain causes screen wobble (nausea on server)
            if (bio.getPainLevel() > 7f && !bio.hasCondition("on_morphine")) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.CONFUSION, 25, 0, false, false));
            }
        }

        // ── Organ rejection tick ─────────────────────────────────────────────
        bio.tickOrganRejection();
        for (String slot : new String[]{"heart", "liver", "kidney", "lung"}) {
            float rejection = bio.getOrganRejection(slot);
            if (rejection > 0.7f) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.POISON, 25, 0, false, false));
            }
            if (rejection >= 1.0f) {
                player.hurt(player.damageSources().generic(), 3.0f);
                bio.addMedicalHistoryEntry("[T] ORGAN REJECTION: " + slot + " fully rejected.");
                bio.transplantOrgan(slot, ""); // organ dies
            }
        }

        // ── Morphine decay ───────────────────────────────────────────────────
        // Simple approach: remove condition if pain is very low
        if (bio.hasCondition("on_morphine") && bio.getPainLevel() < 0.5f) {
            bio.removeCondition("on_morphine");
        }

        // ── Sync to client every 5 seconds ──────────────────────────────────
        if (player.tickCount % 100 == 0 && player instanceof ServerPlayer sp) {
            BioForgeNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SyncBioDataPacket(bio)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            PlayerBioData bio = BioForgeCapabilities.get(player);
            // Ensure limbs initialized
            if (bio.getAllLimbs().isEmpty()) {
                bio.initDefaultLimbs();
            }
            // Sync immediately on login
            BioForgeNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SyncBioDataPacket(bio)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            PlayerBioData bio = BioForgeCapabilities.get(player);
            // On respawn: clear transient conditions, preserve structural state
            bio.removeCondition("sedated");
            bio.setSedated(false, 0);
            // Blood resets to 60% on respawn (slight penalty)
            bio.addBlood(bio.getMaxBloodVolume() * 0.6f - bio.getBloodVolume());
            bio.reducePain(bio.getPainLevel());
            // Limbs persist (death doesn't magically regenerate them)
            BioForgeNetwork.CHANNEL.send(
                    PacketDistributor.PLAYER.with(() -> sp),
                    new SyncBioDataPacket(bio)
            );
        }
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            PlayerBioData bio = BioForgeCapabilities.get(player);
            bio.addMedicalHistoryEntry("[T] Patient died. Cause: " + event.getSource().getMsgId());
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // Preserve capability data across death/respawn (same player UUID)
        Player original = event.getOriginal();
        Player newPlayer = event.getEntity();

        original.getCapability(BioForgeCapabilities.PLAYER_BIO_DATA).ifPresent(oldData -> {
            newPlayer.getCapability(BioForgeCapabilities.PLAYER_BIO_DATA).ifPresent(newData -> {
                newData.deserializeNBT(oldData.serializeNBT());
            });
        });
    }
}
