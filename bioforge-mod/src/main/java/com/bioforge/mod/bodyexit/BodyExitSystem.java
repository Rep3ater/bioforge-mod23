package com.bioforge.mod.bodyexit;

import com.bioforge.mod.network.BioForgeNetwork;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.*;

/**
 * BodyExitSystem — server-side coordinator for the astral projection system.
 *
 * Thread model: all methods must be called on the server thread.
 * State is stored in static maps keyed by player UUID — safe for single-world
 * servers; if you run multiple integrated servers, replace with level-keyed maps.
 */
public class BodyExitSystem {

    private static final Map<UUID, UUID>     PROJECTING_PLAYERS = new HashMap<>();
    private static final Map<UUID, GameType> SAVED_GAME_MODES   = new HashMap<>();

    // ── Public API ────────────────────────────────────────────────────────────

    public static boolean isProjecting(Player player) {
        return PROJECTING_PLAYERS.containsKey(player.getUUID());
    }

    /**
     * Detaches the player's consciousness from their body:
     *  1. Spawns a BodyShellEntity at the player's position.
     *  2. Saves the full inventory into the shell (slots 0..35 + armour + offhand).
     *  3. Clears the player's inventory.
     *  4. Switches the player to SPECTATOR.
     *  5. Sends BodyExitPacket so the client HUD updates.
     */
    public static boolean leaveBody(ServerPlayer player) {
        if (isProjecting(player)) {
            player.sendSystemMessage(Component.literal("§c[BioForge] You are already projecting."));
            return false;
        }

        ServerLevel level = player.serverLevel();
        BodyShellEntity shell = ModBodyEntities.BODY_SHELL.get().create(level);
        if (shell == null) {
            player.sendSystemMessage(Component.literal("§c[BioForge] Failed to materialise body shell."));
            return false;
        }

        // Position
        shell.setPos(player.getX(), player.getY(), player.getZ());
        shell.setYRot(player.getYRot());
        shell.setXRot(0f); // bodies don't look up/down
        shell.setOwnerUuid(player.getUUID());
        shell.setSlimModel(isSlimSkin(player));
        shell.setHealth(player.getHealth());

        // Snapshot entire inventory (main 36 + armour 4 + offhand 1)
        List<ItemStack> snapshot = new ArrayList<>();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty()) snapshot.add(s.copy());
        }
        shell.setInventorySnapshot(snapshot);

        level.addFreshEntity(shell);

        PROJECTING_PLAYERS.put(player.getUUID(), shell.getUUID());
        SAVED_GAME_MODES.put(player.getUUID(), player.gameMode.getGameModeForPlayer());

        player.setGameMode(GameType.SPECTATOR);
        player.getInventory().clearContent();

        player.sendSystemMessage(Component.literal(
                "§b[BioForge] §7Your consciousness drifts free. §bRight-click your body§7 to return."));

        sendPacket(player, shell.getId(), true);
        return true;
    }

    /**
     * Returns the projecting player into their shell:
     *  1. Validates that the shell UUID matches.
     *  2. Teleports player to shell position.
     *  3. Restores inventory and health.
     *  4. Restores previous game mode.
     *  5. Discards the shell.
     */
    public static void returnToBody(ServerPlayer player, BodyShellEntity shell) {
        if (!isProjecting(player)) return;

        UUID expected = PROJECTING_PLAYERS.get(player.getUUID());
        if (expected == null || !expected.equals(shell.getUUID())) {
            player.sendSystemMessage(Component.literal("§c[BioForge] That is not your body."));
            return;
        }

        // Teleport back
        player.teleportTo(player.serverLevel(),
                shell.getX(), shell.getY(), shell.getZ(),
                shell.getYRot(), player.getXRot());

        // Restore inventory
        player.getInventory().clearContent();
        for (ItemStack stack : shell.getSavedInventory()) {
            if (!stack.isEmpty()) player.getInventory().add(stack.copy());
        }

        // Restore health (cap at max)
        player.setHealth(Math.min(shell.getHealth(), player.getMaxHealth()));

        // Restore game mode
        GameType mode = SAVED_GAME_MODES.getOrDefault(player.getUUID(), GameType.SURVIVAL);
        player.setGameMode(mode);

        // Cleanup
        PROJECTING_PLAYERS.remove(player.getUUID());
        SAVED_GAME_MODES.remove(player.getUUID());
        shell.discard();

        player.sendSystemMessage(Component.literal("§b[BioForge] §7You settle back into your body."));
        sendPacket(player, -1, false);
    }

    /**
     * Called when the shell is destroyed while the player is still projecting.
     * Switches the player out of spectator so they can actually die.
     */
    public static void killProjectingPlayer(ServerPlayer player, DamageSource cause) {
        if (!isProjecting(player)) return;

        PROJECTING_PLAYERS.remove(player.getUUID());
        SAVED_GAME_MODES.remove(player.getUUID());

        player.setGameMode(GameType.SURVIVAL);
        // setHealth(0) alone doesn't trigger death logic; hurt() does
        player.setHealth(1f);
        player.hurt(cause, Float.MAX_VALUE);

        player.sendSystemMessage(Component.literal(
                "§4[BioForge] §cThe tether snaps. Your body is gone."));

        sendPacket(player, -1, false);
    }

    /**
     * Emergency cleanup on disconnect: drops the shell's items in-world,
     * discards the shell, resets game mode.
     */
    public static void forceReturnOnDisconnect(ServerPlayer player) {
        if (!isProjecting(player)) return;

        UUID shellUuid = PROJECTING_PLAYERS.remove(player.getUUID());
        GameType saved  = SAVED_GAME_MODES.remove(player.getUUID());

        if (shellUuid != null && player.serverLevel() != null) {
            final UUID target = shellUuid;
            player.serverLevel().getAllEntities().forEach(e -> {
                if (e instanceof BodyShellEntity shell && shell.getUUID().equals(target)) {
                    shell.getSavedInventory().forEach(s -> shell.spawnAtLocation(s.copy()));
                    shell.discard();
                }
            });
        }

        if (saved != null) player.setGameMode(saved);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void sendPacket(ServerPlayer player, int shellId, boolean projecting) {
        BioForgeNetwork.CHANNEL.send(
                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new BodyExitPacket(shellId, projecting));
    }

    /**
     * Mojang's own UUID-based skin type heuristic:
     * hash bits at position (msb >> 16) & 1 → 0 = Steve (wide), 1 = Alex (slim).
     */
    private static boolean isSlimSkin(ServerPlayer player) {
        long msb = player.getUUID().getMostSignificantBits();
        return ((msb >> 16) & 1L) != 0L;
    }
}
