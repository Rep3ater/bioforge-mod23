package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.config.BioForgeConfig;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import com.bioforge.mod.network.SyncBioDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

/**
 * Anesthetic Syringe.
 * Right-click on a player → sedates them for the configured duration.
 * Consumed on use (stacksTo 4).
 *
 * Applies: Blindness, extreme Slowness, Weakness.
 * Marks the player's bio data as sedated so surgical validation passes.
 */
public class AnestheticSyringeItem extends Item {

    public AnestheticSyringeItem() {
        super(new Properties().stacksTo(4));
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) {
            var bio = com.bioforge.mod.capabilities.BioForgeCapabilities.get(user);
            if (bio.isSedated()) {
                user.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "[BioForge] Already sedated.").withStyle(net.minecraft.ChatFormatting.YELLOW));
            } else {
                int duration = 2400;
                bio.setSedated(true, duration);
                bio.addCondition("sedated");
                bio.addMedicalHistoryEntry("[T] Anesthetic self-administered");
                user.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
                user.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.WEAKNESS, duration, 3));
                user.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 5));
                BioForgeNetwork.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.NEAR.with(() -> new net.minecraftforge.network.PacketDistributor.TargetPoint(
                        user.getX(), user.getY(), user.getZ(), 24, level.dimension())),
                    new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.SEDATION, user.position(), user.getId()));
                if (user instanceof net.minecraft.server.level.ServerPlayer sp)
                    BioForgeNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio));
                user.getItemInHand(hand).shrink(1);
                user.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "[BioForge] Self-sedated for " + (duration/20) + "s.")
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
            }
        }
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player surgeon,
                                                   LivingEntity target, InteractionHand hand) {
        Level level = surgeon.level();
        if (level.isClientSide) return InteractionResult.SUCCESS;

        if (!(target instanceof Player patient)) {
            // Can still sedate hostile entities (just apply effects without BioData)
            int duration = 2400;
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 10));
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }

        PlayerBioData bio = BioForgeCapabilities.get(patient);

        if (bio.isSedated()) {
            surgeon.sendSystemMessage(Component.literal("[BioForge] Patient is already sedated.").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }

        int duration = 2400; // default 2 min; TODO: read from syringe NBT for custom doses

        bio.setSedated(true, duration);
        bio.addCondition("sedated");

        patient.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, duration, 0));
        patient.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, duration, 3));
        patient.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, duration, 10));

        bio.addMedicalHistoryEntry("[T] Anesthetic administered by " + surgeon.getName().getString()
                + ". Duration: " + (duration / 20) + "s");

        // FX
        BioForgeNetwork.CHANNEL.send(
                PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                        patient.getX(), patient.getY(), patient.getZ(), 24, level.dimension())),
                new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.SEDATION, patient.position(), patient.getId())
        );

        stack.shrink(1);
        surgeon.sendSystemMessage(Component.literal("[BioForge] Patient sedated for " + (duration / 20) + "s.")
                .withStyle(ChatFormatting.GREEN));
        return InteractionResult.SUCCESS;
    }
}
