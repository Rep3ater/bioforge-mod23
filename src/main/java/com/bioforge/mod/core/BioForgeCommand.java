package com.bioforge.mod.core;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.procedures.ProcedureRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * /bioforge command tree.
 *
 * Subcommands:
 *   /bioforge version               — Shows current mod version in chat
 *   /bioforge status [player]       — Shows bio data status for yourself or another player
 *   /bioforge prompt                — Prints the AI-readable "latest prompt" (to-do list)
 *   /bioforge procedure <id> <target> [args] — Runs a named procedure (OP only)
 *   /bioforge limb set <slot> <type> [model] — Directly set a limb (OP only)
 *   /bioforge blood set <player> <type>      — Set blood type (OP only)
 *   /bioforge record <player>                — Print full medical history
 */
@Mod.EventBusSubscriber(modid = BioForgeMod.MODID)
public class BioForgeCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("bioforge")

            // ── /bioforge version ─────────────────────────────────────────────
            .then(Commands.literal("version")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "§aBioForge §fv" + BioForgeMod.VERSION +
                            " §7(build " + BioForgeMod.BUILD_DATE + ")"), false);
                    return 1;
                }))

            // ── /bioforge status [player] ─────────────────────────────────────
            .then(Commands.literal("status")
                .executes(ctx -> {
                    CommandSourceStack src = ctx.getSource();
                    if (src.getEntity() instanceof ServerPlayer sp) {
                        printStatus(sp, sp);
                    }
                    return 1;
                })
                .then(Commands.argument("target", StringArgumentType.word())
                    .requires(src -> src.hasPermission(2))
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "target");
                        ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
                        if (target == null) {
                            ctx.getSource().sendFailure(Component.literal("Player '" + name + "' not found online."));
                            return 0;
                        }
                        if (ctx.getSource().getEntity() instanceof ServerPlayer surgeon) {
                            printStatus(surgeon, target);
                        } else {
                            printStatus(target, target);
                        }
                        return 1;
                    })))

            // ── /bioforge record <player> ─────────────────────────────────────
            .then(Commands.literal("record")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.word())
                    .executes(ctx -> {
                        String name = StringArgumentType.getString(ctx, "target");
                        ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
                        if (target == null) {
                            ctx.getSource().sendFailure(Component.literal("Player not found."));
                            return 0;
                        }
                        PlayerBioData bio = BioForgeCapabilities.get(target);
                        ctx.getSource().sendSuccess(() -> Component.literal(
                                "§6Medical History — " + target.getName().getString()), false);
                        List<String> history = bio.getMedicalHistory();
                        if (history.isEmpty()) {
                            ctx.getSource().sendSuccess(() -> Component.literal("  §7No records."), false);
                        } else {
                            history.forEach(entry ->
                                    ctx.getSource().sendSuccess(() -> Component.literal("  §f" + entry), false));
                        }
                        return 1;
                    })))

            // ── /bioforge blood set <player> <type> ──────────────────────────
            .then(Commands.literal("blood")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("type", StringArgumentType.word())
                            .executes(ctx -> {
                                String name = StringArgumentType.getString(ctx, "target");
                                String typeStr = StringArgumentType.getString(ctx, "type");
                                ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(name);
                                if (target == null) { ctx.getSource().sendFailure(Component.literal("Player not found.")); return 0; }
                                com.bioforge.mod.core.BloodType bt = com.bioforge.mod.core.BloodType.fromString(typeStr);
                                BioForgeCapabilities.get(target).setBloodType(bt);
                                ctx.getSource().sendSuccess(() -> Component.literal("§aBlood type set to " + bt.getDisplay() + " for " + name), false);
                                return 1;
                            })))))

            // ── /bioforge limb set <target> <slot> <type> [model] ────────────
            .then(Commands.literal("limb")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("set")
                    .then(Commands.argument("target", StringArgumentType.word())
                        .then(Commands.argument("slot", StringArgumentType.word())
                            .then(Commands.argument("type", StringArgumentType.word())
                                .executes(ctx -> {
                                    return setLimb(ctx.getSource(),
                                            StringArgumentType.getString(ctx, "target"),
                                            StringArgumentType.getString(ctx, "slot"),
                                            StringArgumentType.getString(ctx, "type"), "");
                                })
                                .then(Commands.argument("model", StringArgumentType.greedyString())
                                    .executes(ctx -> {
                                        return setLimb(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "target"),
                                                StringArgumentType.getString(ctx, "slot"),
                                                StringArgumentType.getString(ctx, "type"),
                                                StringArgumentType.getString(ctx, "model"));
                                    })))))))

            // ── /bioforge prompt ──────────────────────────────────────────────
            // This is the AI-readable endpoint. It prints the current dev to-do
            // list and session context so another AI instance can pick up where
            // the last left off. Keep this up to date.
            .then(Commands.literal("prompt")
                .executes(ctx -> {
                    printPrompt(ctx.getSource());
                    return 1;
                }));

        dispatcher.register(root);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static void printStatus(Player viewer, Player target) {
        PlayerBioData bio = BioForgeCapabilities.get(target);
        CommandSourceStack src = ((ServerPlayer) viewer).createCommandSourceStack();

        viewer.sendSystemMessage(Component.literal("§6═══ BioForge Status: " + target.getName().getString() + " ═══"));
        viewer.sendSystemMessage(Component.literal("§fBlood: §c" + String.format("%.1f/%.1fL", bio.getBloodVolume(), bio.getMaxBloodVolume())
                + " §7(" + bio.getBloodType().getDisplay() + ")"));
        viewer.sendSystemMessage(Component.literal("§fPain: §e" + String.format("%.1f", bio.getPainLevel()) + "/10"));
        viewer.sendSystemMessage(Component.literal("§fSedated: " + (bio.isSedated() ? "§aYes (" + bio.getSedationTicksRemaining()/20 + "s)" : "§cNo")));
        viewer.sendSystemMessage(Component.literal("§fDNA on file: " + (bio.hasDnaOnFile() ? "§aYes" : "§cNo")));
        viewer.sendSystemMessage(Component.literal("§fClone Gen: §b" + bio.getCloneGeneration()));
        viewer.sendSystemMessage(Component.literal("§fPatient ID: §7" + (bio.getPatientId().isEmpty() ? "Unassigned" : bio.getPatientId())));

        // Limbs
        viewer.sendSystemMessage(Component.literal("§fLimbs:"));
        bio.getAllLimbs().forEach((slot, state) -> {
            String color = state.isPresent() ? "§a" : "§c";
            String typeStr = state.getType().name();
            viewer.sendSystemMessage(Component.literal("  " + color + slot + " §7[" + typeStr + "]"
                    + (state.getModelOverride().isEmpty() ? "" : " §9[custom model]")));
        });

        // Active conditions
        if (!bio.getActiveConditions().isEmpty()) {
            viewer.sendSystemMessage(Component.literal("§fConditions: §e" + String.join(", ", bio.getActiveConditions())));
        }
    }

    private static int setLimb(CommandSourceStack src, String playerName, String slot, String typeStr, String model) {
        ServerPlayer target;
        if (playerName.equalsIgnoreCase("self") || playerName.equals("@s")) {
            target = src.getPlayer();
            if (target == null) { src.sendFailure(Component.literal("Must be run by a player for 'self'.")); return 0; }
        } else {
            target = src.getServer().getPlayerList().getPlayerByName(playerName);
            if (target == null) { src.sendFailure(Component.literal("Player not found: " + playerName)); return 0; }
        }
        PlayerBioData bio = BioForgeCapabilities.get(target);
        com.bioforge.mod.core.LimbState state = switch (typeStr.toLowerCase()) {
            case "none", "absent", "remove" -> com.bioforge.mod.core.LimbState.absent();
            case "prosthetic"               -> com.bioforge.mod.core.LimbState.prosthetic(model);
            case "cybernetic"               -> com.bioforge.mod.core.LimbState.cybernetic(model);
            case "natural", "bio"           -> com.bioforge.mod.core.LimbState.naturalPresent();
            default -> { src.sendFailure(Component.literal("Unknown type. Use: none/remove, prosthetic, cybernetic, natural")); yield null; }
        };
        if (state == null) return 0;
        bio.setLimb(slot, state);
        com.bioforge.mod.network.BioForgeNetwork.CHANNEL.send(
            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> target),
            new com.bioforge.mod.network.SyncBioDataPacket(bio));
        final String finalName = target.getName().getString();
        src.sendSuccess(() -> Component.literal("§aSet " + slot + " to " + typeStr
                + " for " + finalName + (model.isEmpty() ? "" : " [model: " + model + "]")), true);
        return 1;
    }

    private static void printPrompt(CommandSourceStack src) {
        // ════════════════════════════════════════════════════════════════════
        // AI PROMPT — BIOFORGE LATEST STATE
        // This is the machine-readable context block for AI continuation.
        // Keep this updated as dev progresses.
        // ════════════════════════════════════════════════════════════════════
        String[] lines = {
            "§d╔══ BIOFORGE AI PROMPT ══ v" + BioForgeMod.VERSION + " (" + BioForgeMod.BUILD_DATE + ") ══╗",
            "§fMod: BioForge | Loader: Forge 1.20.1-47.4.20 | Package: com.bioforge.mod",
            "§fGoal: Biological simulation + roleplay mod with surgical procedures, cloning, and limb systems.",
            "",
            "§e── COMPLETED SYSTEMS ──",
            "§a✓ PlayerBioData capability (limbs, blood, organs, DNA, pain, sedation, history)",
            "§a✓ BloodType enum with full ABO/Rh compatibility matrix",
            "§a✓ LimbState (BIOLOGICAL, PROSTHETIC, CYBERNETIC, NONE) with stat modifiers",
            "§a✓ ProcedureRegistry (blood_draw, transfusion, dna_sample, amputate, limb_attach x3, organ remove/transplant, sedation, morphine, cauterize, stitch, assign_id)",
            "§a✓ CloningSystem + CloneEntity (DNA capture, growth vat, spawn, instability levels)",
            "§a✓ BioForgeEvents (per-tick bleeding, pain, organ rejection, sedation, respawn sync)",
            "§a✓ Network layer (7 packets: sync, FX, GUI open, limb model, blood splatter, request, confirm)",
            "§a✓ BioForgeHud (version string, blood bar, pain bar, limb status, sedation timer)",
            "§a✓ ProcedureFX (particle + sound FX for all 12 effect types)",
            "§a✓ BioForgeConfig (common + client config, all gameplay toggles)",
            "§a✓ /bioforge command (version, status, record, blood set, limb set, prompt)",
            "§a✓ ModItems (35+ items), ModBlocks (16 blocks), ModSounds (13 sounds), ModEntities (clone)",
            "",
            "§e── NEXT TODO (priority order) ──",
            "§c1. Block entity + GUI for SurgicalTableBlock (container, slot validation, procedure dispatch)",
            "§c2. CloningVatBlock block entity (growth timer, NBT, GUI with DNA slot + catalyst slots)",
            "§c3. Item NBT tooltips (blood type on bag, DNA hash on sample, patient ID on wristband)",
            "§c4. BloodSyringeItem use logic (right-click player → blood draw via ProcedureRegistry)",
            "§c5. AnestheticSyringeItem use logic (right-click → administer_sedation)",
            "§c6. Client-side limb model layer renderer (hide/replace arms/legs based on LimbState)",
            "§c7. Sound file JSON (assets/bioforge/sounds.json) + placeholder .ogg files",
            "§c8. Model JSON files for all items and blocks (or use placeholder cube models)",
            "§c9. Lang file (en_us.json) for all item/block names and config tooltips",
            "§c10. Recipes: surgical crafting recipes via JSON + custom workbench recipe type",
            "§c11. Loot tables for all blocks",
            "§c12. MedicalRecordItem: readable book GUI showing patient history",
            "§c13. PatientMonitorBlock: displays nearby medical bed occupant's bio data on screen",
            "§c14. Roleplay consent system (/bioforge confirm, /bioforge deny) for multiplayer surgery",
            "§c15. Camera shake effect during high-pain or sedation states",
            "",
            "§e── DESIGN NOTES FOR AI ──",
            "§7- All procedures go through ProcedureRegistry.execute() — never bypass it",
            "§7- Sync bio data after every server-side change via SyncBioDataPacket",
            "§7- Model overrides use ResourceLocation strings ('bioforge:models/entity/clone_arm')",
            "§7- Config flags wrap gameplay-altering features; check before enforcing constraints",
            "§7- /bioforge prompt is the canonical to-do handoff; update it each session",
            "§7- Version string is BioForgeMod.VERSION + BUILD_DATE; change both each session",
            "§d╚════════════════════════════════════════════════════════════════╝"
        };
        for (String line : lines) {
            src.sendSuccess(() -> Component.literal(line), false);
        }
    }
}
