package com.bioforge.mod.client;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.config.BioForgeConfig;
import com.bioforge.mod.core.LimbState;
import com.bioforge.mod.items.DoctorCoatItem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

/**
 * BioForge HUD — only renders when the player wears a Doctor Coat.
 *
 * Self panel (bottom-left):
 *   ┌─────────────────────────────┐
 *   │ ♥ BLOOD  ████████░░  4.5L  │
 *   │ ⚡ PAIN   ██░░░░░░░░  2.0  │
 *   │ [LA✓] [RA✓] [LL✓] [RL✓]   │
 *   │ SEDATED 45s                 │
 *   └─────────────────────────────┘
 *
 * Target panel (right side, when looking at a player/villager ≤10 blocks):
 *   ┌─────────────────────────────┐
 *   │ ◈ Steve          [A+]       │
 *   │ ♥ ████████░░  4.5L         │
 *   │ ⚡ ██░░░░░░░░  2.0          │
 *   │ [LA✓] [RA✗] [LL✓] [RL✓]   │
 *   │ Conditions: bleeding_left_arm│
 *   └─────────────────────────────┘
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID, value = Dist.CLIENT)
public class BioForgeHud {

    // ── Panel colors ──────────────────────────────────────────────────────────
    private static final int BG          = 0xCC0A0F14;   // near-black teal bg
    private static final int BORDER      = 0xFF1A3A4A;   // dark teal border
    private static final int BORDER_HI   = 0xFF2A6A8A;   // highlight border
    private static final int TEXT_TITLE  = 0xFF7AEEFF;   // bright cyan title
    private static final int TEXT_LABEL  = 0xFF8899AA;   // muted label
    private static final int TEXT_VALUE  = 0xFFDDEEFF;   // bright value
    private static final int TEXT_WARN   = 0xFFFF4444;   // warning red
    private static final int TEXT_GOOD   = 0xFF44FF88;   // ok green
    private static final int TEXT_CYBER  = 0xFF00CCFF;   // cybernetic cyan
    private static final int TEXT_PROS   = 0xFFAAAAAA;   // prosthetic grey
    private static final int SEDATED_COL = 0xFF8844FF;   // purple

    // ── Bar colors ────────────────────────────────────────────────────────────
    private static final int BAR_BG      = 0xFF0A1520;
    private static final int BLOOD_HI    = 0xFFCC2222;
    private static final int BLOOD_LO    = 0xFF550808;
    private static final int PAIN_HI     = 0xFFFF8800;
    private static final int PAIN_LO     = 0xFFAA4400;

    // ── Panel dimensions ─────────────────────────────────────────────────────
    private static final int PANEL_W     = 148;
    private static final int PANEL_PAD   = 5;

    @SubscribeEvent
    public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // Gate: only render when wearing Doctor Coat
        if (!wearingDoctorCoat(mc.player)) return;

        GuiGraphics gfx = event.getGuiGraphics();
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // ── Self panel (bottom-left) ──────────────────────────────────────────
        PlayerBioData selfBio = BioForgeCapabilities.get(mc.player);
        int panelH = computePanelHeight(selfBio);
        int px = 6;
        int py = sh - panelH - 22; // above hotbar
        renderPanel(gfx, mc, px, py, PANEL_W, panelH, selfBio,
                mc.player.getName().getString(), true);

        // ── Target panel (right side when looking at entity ≤10 blocks) ───────
        LivingEntity target = getLookedAtEntity(mc);
        if (target != null) {
            PlayerBioData targetBio = BioForgeCapabilities.get(target);
            if (targetBio != null) {
                int tpH = computePanelHeight(targetBio);
                int tpX = sw - PANEL_W - 6;
                int tpY = sh / 2 - tpH / 2;
                String tName = target instanceof Player p2
                        ? p2.getName().getString() : "Living Entity";
                renderPanel(gfx, mc, tpX, tpY, PANEL_W, tpH, targetBio, tName, false);
            }
        }
    }

    // ── Panel renderer ────────────────────────────────────────────────────────
    private static void renderPanel(GuiGraphics gfx, Minecraft mc,
                                     int x, int y, int w, int h,
                                     PlayerBioData bio, String name, boolean isSelf) {
        // Background
        gfx.fill(x, y, x + w, y + h, BG);
        // Double border
        drawBorder(gfx, x, y, w, h, BORDER);
        drawBorder(gfx, x + 1, y + 1, w - 2, h - 2, BORDER_HI);

        int cx = x + PANEL_PAD;
        int cy = y + PANEL_PAD;

        // ── Title row ─────────────────────────────────────────────────────────
        String titleLabel = isSelf ? "◈ YOU" : "◈ " + name;
        gfx.drawString(mc.font, titleLabel, cx, cy, TEXT_TITLE, false);
        String btStr = "  [" + bio.getBloodType().getDisplay() + "]";
        gfx.drawString(mc.font, btStr, cx + mc.font.width(titleLabel), cy,
                bloodTypeColor(bio), false);
        cy += 11;

        // ── Blood bar ─────────────────────────────────────────────────────────
        float bPct = bio.getBloodPercent();
        gfx.drawString(mc.font, "♥", cx, cy, 0xFFCC3333, false);
        int barX = cx + 10;
        renderBar(gfx, barX, cy + 1, w - PANEL_PAD * 2 - 38, 7, bPct,
                lerpColor(BLOOD_LO, BLOOD_HI, bPct));
        String bVal = String.format("%.1fL", bio.getBloodVolume());
        gfx.drawString(mc.font, bVal, x + w - PANEL_PAD - mc.font.width(bVal),
                cy, bPct < 0.3f ? TEXT_WARN : TEXT_VALUE, false);
        cy += 11;

        // ── Pain bar ──────────────────────────────────────────────────────────
        if (bio.getPainLevel() > 0.05f) {
            float pPct = bio.getPainLevel() / 10f;
            gfx.drawString(mc.font, "⚡", cx, cy, 0xFFFFAA00, false);
            renderBar(gfx, barX, cy + 1, w - PANEL_PAD * 2 - 38, 7, pPct,
                    lerpColor(PAIN_LO, PAIN_HI, pPct));
            String pVal = String.format("%.1f", bio.getPainLevel());
            gfx.drawString(mc.font, pVal, x + w - PANEL_PAD - mc.font.width(pVal),
                    cy, pPct > 0.7f ? TEXT_WARN : TEXT_VALUE, false);
            cy += 11;
        }

        // ── Limb row ──────────────────────────────────────────────────────────
        cy += 1;
        String[] slots  = {"left_arm","right_arm","left_leg","right_leg"};
        String[] labels = {"LA","RA","LL","RL"};
        int lx = cx;
        for (int i = 0; i < 4; i++) {
            LimbState ls = bio.getLimb(slots[i]);
            int col = limbColor(ls);
            String sym = ls.isPresent() ? "✓" : "✗";
            String tag = "[" + labels[i] + sym + "]";
            gfx.drawString(mc.font, tag, lx, cy, col, false);
            lx += mc.font.width(tag) + 2;
        }
        cy += 10;

        // ── Sedation ──────────────────────────────────────────────────────────
        if (bio.isSedated()) {
            int secsLeft = bio.getSedationTicksRemaining() / 20;
            gfx.drawString(mc.font, "◉ SEDATED " + secsLeft + "s", cx, cy, SEDATED_COL, false);
            cy += 10;
        }

        // ── Active conditions (non-self only, or if self has serious ones) ────
        if (!bio.getActiveConditions().isEmpty()) {
            String condStr = String.join(", ", bio.getActiveConditions());
            if (condStr.length() > 20) condStr = condStr.substring(0, 18) + "…";
            gfx.drawString(mc.font, condStr, cx, cy, TEXT_WARN, false);
        }
    }

    // ── Geometry helpers ──────────────────────────────────────────────────────

    private static int computePanelHeight(PlayerBioData bio) {
        int h = PANEL_PAD * 2;
        h += 11; // title
        h += 11; // blood
        if (bio.getPainLevel() > 0.05f) h += 11;
        h += 11; // limbs
        if (bio.isSedated()) h += 10;
        if (!bio.getActiveConditions().isEmpty()) h += 10;
        return h + 1;
    }

    private static void renderBar(GuiGraphics gfx, int x, int y, int w, int h,
                                   float pct, int color) {
        pct = Math.max(0, Math.min(1, pct));
        // Background track
        gfx.fill(x, y, x + w, y + h, BAR_BG);
        // Fill
        int filled = (int)(w * pct);
        if (filled > 0) gfx.fill(x, y, x + filled, y + h, color | 0xFF000000);
        // Shine strip
        if (filled > 2) gfx.fill(x, y, x + filled, y + 1,
                brighten(color, 60) | 0xAA000000);
        // Border
        gfx.hLine(x - 1, x + w, y - 1, BORDER_HI);
        gfx.hLine(x - 1, x + w, y + h, BORDER_HI);
        gfx.vLine(x - 1, y - 1, y + h, BORDER_HI);
        gfx.vLine(x + w, y - 1, y + h, BORDER_HI);
    }

    private static void drawBorder(GuiGraphics gfx, int x, int y, int w, int h, int color) {
        gfx.hLine(x, x + w - 1, y, color);
        gfx.hLine(x, x + w - 1, y + h - 1, color);
        gfx.vLine(x, y, y + h - 1, color);
        gfx.vLine(x + w - 1, y, y + h - 1, color);
    }

    // ── Entity look-at detection ──────────────────────────────────────────────

    private static LivingEntity getLookedAtEntity(Minecraft mc) {
        if (mc.hitResult == null) return null;
        if (mc.hitResult.getType() != HitResult.Type.ENTITY) return null;
        if (!(mc.hitResult instanceof EntityHitResult ehr)) return null;
        if (!(ehr.getEntity() instanceof LivingEntity le)) return null;
        if (le == mc.player) return null;
        // Only players and villagers
        if (!(le instanceof LivingEntity)) return null;
        // Within 10 blocks
        if (mc.player.distanceTo(le) > 10) return null;
        return le;
    }

    // ── Coat check ────────────────────────────────────────────────────────────

    public static boolean wearingDoctorCoat(Player player) {
        ItemStack chest = player.getInventory().armor.get(2); // index 2 = chestplate
        return !chest.isEmpty() && chest.getItem() instanceof DoctorCoatItem;
    }

    // ── Color utils ──────────────────────────────────────────────────────────

    private static int limbColor(LimbState ls) {
        if (!ls.isPresent()) return TEXT_WARN;
        return switch (ls.getType()) {
            case CYBERNETIC -> TEXT_CYBER;
            case PROSTHETIC -> TEXT_PROS;
            default         -> TEXT_GOOD;
        };
    }

    private static int bloodTypeColor(PlayerBioData bio) {
        return switch (bio.getBloodType()) {
            case O_NEG, O_POS -> 0xFFFF6644;
            case AB_POS, AB_NEG -> 0xFF44AAFF;
            default -> 0xFFFF9966;
        };
    }

    private static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        return ((int)(ar + (br - ar) * t) << 16)
             | ((int)(ag + (bg - ag) * t) << 8)
             |  (int)(ab + (bb - ab) * t);
    }

    private static int brighten(int color, int amount) {
        int r = Math.min(255, ((color >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((color >> 8)  & 0xFF) + amount);
        int b = Math.min(255, ( color        & 0xFF) + amount);
        return (r << 16) | (g << 8) | b;
    }
}
