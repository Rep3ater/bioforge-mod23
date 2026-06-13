package com.bioforge.mod.core;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.LimbState;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = BioForgeMod.MODID)
public class LimbModifiers {

    private static final UUID UUID_RA_MINING  = UUID.fromString("bf110000-0001-0001-0001-000000000001");
    private static final UUID UUID_RA_ATTACK  = UUID.fromString("bf110000-0001-0001-0001-000000000002");
    private static final UUID UUID_LA_ATTACK  = UUID.fromString("bf110000-0001-0001-0001-000000000003");
    private static final UUID UUID_LL_SPEED   = UUID.fromString("bf110000-0001-0001-0001-000000000004");
    private static final UUID UUID_RL_SPEED   = UUID.fromString("bf110000-0001-0001-0001-000000000005");
    private static final UUID UUID_JUMP       = UUID.fromString("bf110000-0001-0001-0001-000000000006");

    private static void applyLimbModifiers(net.minecraft.world.entity.player.Player player) {
        PlayerBioData bio = BioForgeCapabilities.get(player);

        LimbState rightArm = bio.getLimb("right_arm");
        LimbState leftArm  = bio.getLimb("left_arm");
        LimbState leftLeg  = bio.getLimb("left_leg");
        LimbState rightLeg = bio.getLimb("right_leg");

        clearModifier(player, Attributes.ATTACK_DAMAGE, UUID_RA_ATTACK);
        if (!rightArm.isPresent()) {
            addOrReplace(player, Attributes.ATTACK_DAMAGE, UUID_RA_ATTACK,
                    "bf_ra_attack", -0.5, AttributeModifier.Operation.MULTIPLY_TOTAL);
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.DIG_SLOWDOWN,
                    25, 255, false, false));
        } else if (rightArm.getType() == LimbState.LimbType.CYBERNETIC) {
            addOrReplace(player, Attributes.ATTACK_DAMAGE, UUID_RA_ATTACK,
                    "bf_ra_attack", +0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else if (rightArm.getType() == LimbState.LimbType.PROSTHETIC) {
            addOrReplace(player, Attributes.ATTACK_DAMAGE, UUID_RA_ATTACK,
                    "bf_ra_attack", -0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        clearModifier(player, Attributes.ATTACK_DAMAGE, UUID_LA_ATTACK);
        if (!leftArm.isPresent()) {
            addOrReplace(player, Attributes.ATTACK_DAMAGE, UUID_LA_ATTACK,
                    "bf_la_attack", -0.40, AttributeModifier.Operation.MULTIPLY_TOTAL);
        } else if (leftArm.getType() == LimbState.LimbType.CYBERNETIC) {
            addOrReplace(player, Attributes.ATTACK_DAMAGE, UUID_LA_ATTACK,
                    "bf_la_attack", +0.10, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        clearModifier(player, Attributes.MOVEMENT_SPEED, UUID_LL_SPEED);
        clearModifier(player, Attributes.MOVEMENT_SPEED, UUID_RL_SPEED);
        AttributeInstance jumpAttr = player.getAttribute(Attributes.JUMP_STRENGTH);
        if (jumpAttr != null) jumpAttr.removeModifier(UUID_JUMP);

        double speedPenalty = 0;
        boolean noLeg = false;
        for (LimbState leg : new LimbState[]{leftLeg, rightLeg}) {
            if (!leg.isPresent()) {
                speedPenalty -= 0.40;
                noLeg = true;
            } else if (leg.getType() == LimbState.LimbType.PROSTHETIC) {
                speedPenalty -= 0.10;
            } else if (leg.getType() == LimbState.LimbType.CYBERNETIC) {
                speedPenalty += 0.10;
            }
        }

        if (speedPenalty != 0) {
            addOrReplace(player, Attributes.MOVEMENT_SPEED, UUID_LL_SPEED,
                    "bf_leg_speed", speedPenalty / 2, AttributeModifier.Operation.MULTIPLY_TOTAL);
            addOrReplace(player, Attributes.MOVEMENT_SPEED, UUID_RL_SPEED,
                    "bf_leg_speed2", speedPenalty / 2, AttributeModifier.Operation.MULTIPLY_TOTAL);
        }

        if (noLeg) {
            AttributeInstance jumpAttr2 = player.getAttribute(Attributes.JUMP_STRENGTH);
            if (jumpAttr2 != null) {
                jumpAttr2.removeModifier(UUID_JUMP);
                jumpAttr2.addPermanentModifier(new AttributeModifier(UUID_JUMP, "bf_jump", -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.level().isClientSide()) return;
        if (event.player.tickCount % 20 != 0) return;
        applyLimbModifiers(event.player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            applyLimbModifiers((net.minecraft.world.entity.player.Player) event.getEntity());
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.getEntity().level().isClientSide()) {
            applyLimbModifiers((net.minecraft.world.entity.player.Player) event.getEntity());
        }
    }

    private static void addOrReplace(net.minecraft.world.entity.player.Player player,
                                     Attribute attr,
                                     UUID id, String name, double value,
                                     AttributeModifier.Operation op) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst == null) return;
        inst.removeModifier(id);
        inst.addPermanentModifier(new AttributeModifier(id, name, value, op));
    }

    private static void clearModifier(net.minecraft.world.entity.player.Player player,
                                      Attribute attr, UUID id) {
        AttributeInstance inst = player.getAttribute(attr);
        if (inst != null) inst.removeModifier(id);
    }
}
