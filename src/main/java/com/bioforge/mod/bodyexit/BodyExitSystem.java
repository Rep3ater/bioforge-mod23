package com.bioforge.mod.bodyexit;

import com.bioforge.mod.network.BioForgeNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.*;

/**
 * BodyExitSystem — server-side coordinator for the body-leave (astral projection) system.
 *
 * Flow:
 *  1. Player uses SpiritCatalystItem (right-click) → leaveBody()
 *  2. Server spawns BodyShellEntity at player's location with inventory snapshot.
 *  3. Player is switched to SPECTATOR gamemode; their UUID is tracked.
 *  4. When player interacts with their body (or returnToBody() is called) → returnToBody()
 *  5. If body is killed while projecting → killProjectingPlayer()
 *
 * The shell UUID is stored per player UUID in a static map (server-only).
 * Data survives log-off via an event listener (BioForgeBodyEvents).
 */
public class BodyExitSystem {

    // playerUUID -> shell entity UUID (server-side)
    private static final Map<UUID, UUID> PROJECTING_PLAYERS = new HashMap<>();

    // playerUUID -> their saved game mode before projection
    private static final Map<UUID, GameType> SAVED_GAME_MODES = new HashMap<>();

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /** @return true if this player is currently projecting. */
    public static boolean isProjecting(Player player) {
        return PROJECTING_PLAYERS.containsKey(player.getUUID());
    }

    /**
     * Detach the player's consciousness from their body.
     * Spawns a BodyShellEntity, saves inventory to it, puts player in spectator.
     */
    public static boolean leaveBody(ServerPlayer player) {
        if (isProjecting(player)) {
            player.sendSystemMessage(Component.literal("§c[BioForge] You are already projecting."));
            return false;
        }

        net.minecraft.server.level.ServerLevel level = player.serverLevel();

        // Create the shell
        BodyShellEntity shell = ModBodyEntities.BODY_SHELL.get().create(level);
        if (shell == null) {
            player.sendSystemMessage(Component.literal("§c[BioForge] Failed to create body shell."));
            return false;
        }

        // Position at player's feet, same rotation
        shell.setPos(player.getX(), player.getY(), player.getZ());
        shell.setYRot(player.getYRot());
        shell.setXRot(player.getXRot());

        // Configure shell
        shell.setOwnerUuid(player.getUUID());

        // Determine skin (slim or classic) from player profile
        boolean slim = isSlimSkin(player);
        shell.setSlimModel(slim);
        shell.setSkinTextureUrl(""); // Renderer resolves from UUID on client

        // Save full inventory into shell
        List<ItemStack> snapshot = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) snapshot.add(stack.copy());
        }
        shell.setInventorySnapshot(snapshot);

        // Set shell health to match player's current HP
        shell.setHealth(player.getHealth());

        // Spawn the shell
        level.addFreshEntity(shell);

        // Register projection
        PROJECTING_PLAYERS.put(player.getUUID(), shell.getUUID());
        SAVED_GAME_MODES.put(player.getUUID(), player.gameMode.getGameModeForPlayer());

        // Switch player to spectator
        player.setGameMode(GameType.SPECTATOR);

        // Clear player inventory (it's stored in the shell)
        player.getInventory().clearContent();

        // Notify player
        player.sendSystemMessage(Component.literal("§b[BioForge] §7Your consciousness leaves your body. Press §bSneak+Use§7 on your body to return."));

        // Send packet to notify client (for HUD overlay)
        BioForgeNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new BodyExitPacket(shell.getId(), true)
        );

        return true;
    }

    /**
     * Return the projecting player into their body shell.
     * Restores inventory, reverts game mode, removes shell.
     */
    public static void returnToBody(ServerPlayer player, BodyShellEntity shell) {
        if (!isProjecting(player)) return;

        UUID shellUuid = PROJECTING_PLAYERS.get(player.getUUID());
        if (shellUuid == null || !shellUuid.equals(shell.getUUID())) {
            player.sendSystemMessage(Component.literal("§c[BioForge] That is not your body."));
            return;
        }

        // Teleport player to body position
        player.teleportTo(player.serverLevel(), shell.getX(), shell.getY(), shell.getZ(),
                shell.getYRot(), shell.getXRot());

        // Restore inventory from shell
        List<ItemStack> savedItems = shell.getSavedInventory();
        for (ItemStack stack : savedItems) {
            if (!stack.isEmpty()) {
                player.getInventory().add(stack.copy());
            }
        }

        // Restore health
        player.setHealth(shell.getHealth());

        // Restore game mode
        GameType savedMode = SAVED_GAME_MODES.getOrDefault(player.getUUID(), GameType.SURVIVAL);
        player.setGameMode(savedMode);

        // Clean up tracking
        PROJECTING_PLAYERS.remove(player.getUUID());
        SAVED_GAME_MODES.remove(player.getUUID());

        // Despawn the shell
        shell.discard();

        // Notify
        player.sendSystemMessage(Component.literal("§b[BioForge] §7You return to your body."));

        // Notify client
        BioForgeNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new BodyExitPacket(-1, false)
        );
    }

    /**
     * Called when the body shell is killed while the player is projecting.
     * The player dies as a consequence.
     */
    public static void killProjectingPlayer(ServerPlayer player, DamageSource cause) {
        if (!isProjecting(player)) return;

        PROJECTING_PLAYERS.remove(player.getUUID());
        SAVED_GAME_MODES.remove(player.getUUID());

        // Switch out of spectator first (can't die in spectator)
        player.setGameMode(GameType.SURVIVAL);
        player.setHealth(0.1f); // near death
        player.hurt(cause, 100f); // kill

        player.sendSystemMessage(Component.literal("§4[BioForge] §cYour body was destroyed. The connection is severed."));
    }

    /**
     * Force-return a player without a living body (emergency cleanup).
     * Used on player disconnect if they're still projecting.
     */
    public static void forceReturnOnDisconnect(ServerPlayer player) {
        if (!isProjecting(player)) return;

        UUID shellUuid = PROJECTING_PLAYERS.remove(player.getUUID());
        GameType saved = SAVED_GAME_MODES.remove(player.getUUID());

        // Find and discard shell by UUID scan
        if (shellUuid != null && player.serverLevel() != null) {
            final UUID targetUuid = shellUuid;
            player.serverLevel().getAllEntities().forEach(e -> {
                if (e instanceof BodyShellEntity shell && e.getUUID().equals(targetUuid)) {
                    shell.getSavedInventory().forEach(s -> shell.spawnAtLocation(s.copy()));
                    shell.discard();
                }
            });
        }

        // Restore game mode
        if (saved != null) player.setGameMode(saved);
    }

    /** Get the shell entity ID for this player (or -1 if not projecting). */
    public static UUID getShellUuid(UUID playerUuid) {
        return PROJECTING_PLAYERS.get(playerUuid);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static boolean isSlimSkin(ServerPlayer player) {
        // Slim (Alex model) is determined by skin model type set by the client.
        // We approximate: if the player's UUID has the 'Alex' skin type, use slim.
        // For offline/default: UUID bit 7 & 1 → 0 = Steve, 1 = Alex (Mojang's method)
        UUID uuid = player.getUUID();
        long lsb = uuid.getLeastSignificantBits();
        long msb = uuid.getMostSignificantBits();
        return ((msb >> 16) & 1) != 0;
    }
}
