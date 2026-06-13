package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.bodyexit.BodyExitClientState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import com.bioforge.mod.bodyexit.BodyShellEntity;

/**
 * ProjectionHudOverlay — renders a thin UI when the player is projecting:
 *
 *  ┌──────────────────────────────┐
 *  │  ◈ ASTRAL PROJECTION         │
 *  │  Body: 18.0 ♥  [return: Use] │
 *  └──────────────────────────────┘
 *
 * The overlay is drawn in the top-left corner with a semi-transparent
 * dark teal panel consistent with the main BioForge HUD style.
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID, value = Dist.CLIENT)
public class ProjectionHudOverlay {

    private static final int BG       = 0xCC0A0F14;
    private static final int BORDER   = 0xFF1A3A4A;
    private static final int TITLE    = 0xFF7AEEFF;
    private static final int TEXT     = 0xFFDDEEFF;
    private static final int WARN     = 0xFFFF4444;
    private static final int GOOD     = 0xFF44FF88;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        if (!BodyExitClientState.isProjecting()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();

        // Find shell entity for health display
        int shellId = BodyExitClientState.getShellEntityId();
        BodyShellEntity shell = null;
        if (mc.level != null && shellId >= 0) {
            Entity e = mc.level.getEntity(shellId);
            if (e instanceof BodyShellEntity bs) shell = bs;
        }

        // Panel dimensions
        int pw = 160;
        int ph = shell != null ? 38 : 26;
        int px = 6;
        int py = 6;

        // Background + border
        gfx.fill(px - 1, py - 1, px + pw + 1, py + ph + 1, BORDER);
        gfx.fill(px, py, px + pw, py + ph, BG);

        // Title line
        gfx.drawString(mc.font, "◈ ASTRAL PROJECTION", px + 4, py + 4, TITLE, false);

        if (shell != null) {
            // Body health
            float hp = shell.getHealth();
            float maxHp = shell.getMaxHealth();
            int hpColor = hp < 6f ? WARN : (hp < 12f ? 0xFFFFAA00 : GOOD);
            String hpStr = String.format("Body: %.1f / %.0f ♥", hp, maxHp);
            gfx.drawString(mc.font, hpStr, px + 4, py + 16, hpColor, false);

            // Return hint
            gfx.drawString(mc.font, "§8[Use] on body to return", px + 4, py + 27, 0xFF556677, false);
        } else {
            // Shell not loaded in client
            gfx.drawString(mc.font, "§cBody out of range", px + 4, py + 16, WARN, false);
        }
    }
}
