package com.bioforge.mod.bodyexit;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * BioForgeBodyEvents — handles body-exit edge cases on the Forge event bus.
 *
 *  - PlayerLoggedOutEvent: if player was projecting, drop their body's
 *    inventory at body location and clean up.
 *  - PlayerRespawnEvent: ensure projection state is cleared on respawn
 *    (shouldn't happen normally, but safety net).
 *  - LivingDeathEvent: if a BodyShellEntity dies, handled inside the
 *    entity itself, but we also guard against the projecting player dying
 *    directly (e.g. /kill command) so we clean up the orphaned shell.
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID)
public class BioForgeBodyEvents {

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            // If they disconnect while projecting, clean up their shell gracefully
            BodyExitSystem.forceReturnOnDisconnect(sp);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp) {
            // Safety: clear any stale projection state after death/respawn
            if (BodyExitSystem.isProjecting(sp)) {
                BodyExitSystem.forceReturnOnDisconnect(sp);
            }
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // If a projecting player dies (e.g. from /kill or fall while spectator glitch),
        // we need to also discard their body shell so it doesn't ghost.
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (BodyExitSystem.isProjecting(sp)) {
                // The body shell's die() will handle killProjectingPlayer,
                // but if the player themselves die directly, just clean up.
                BodyExitSystem.forceReturnOnDisconnect(sp);
            }
        }
    }
}
