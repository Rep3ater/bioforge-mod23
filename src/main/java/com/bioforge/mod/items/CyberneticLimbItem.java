package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.LimbState;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import com.bioforge.mod.network.SyncBioDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;

public class CyberneticLimbItem extends Item {
    private final boolean isLeg;
    private CyberneticLimbItem(boolean leg) { super(new Properties().stacksTo(1)); this.isLeg = leg; }
    public static CyberneticLimbItem ofArm() { return new CyberneticLimbItem(false); }
    public static CyberneticLimbItem ofLeg() { return new CyberneticLimbItem(true); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) attach(user.getItemInHand(hand), user, user, hand);
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                   LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) attach(stack, user, target, hand);
        return InteractionResult.SUCCESS;
    }

    private void attach(ItemStack stack, Player surgeon, LivingEntity target, InteractionHand hand) {
        PlayerBioData bio = bio(target);
        if (bio == null) return;

        String[] slots = isLeg ? new String[]{"left_leg","right_leg"} : new String[]{"left_arm","right_arm"};
        String empty = null;
        for (String s : slots) if (!bio.hasLimb(s)) { empty = s; break; }

        if (empty == null) {
            surgeon.sendSystemMessage(Component.literal("[BioForge] No empty slot.").withStyle(ChatFormatting.RED));
            return;
        }

        String model = stack.getOrCreateTag().getString("model_override");
        bio.setLimb(empty, LimbState.cybernetic(model));
        bio.addMedicalHistoryEntry("[T] Cybernetic → " + empty
            + (target == surgeon ? " (self)" : " by " + surgeon.getName().getString()));

        BioForgeNetwork.CHANNEL.send(
            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                target.getX(), target.getY(), target.getZ(), 24, target.level().dimension())),
            new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.LIMB_ATTACH,
                target.position(), target.getId()));

        if (target instanceof ServerPlayer sp)
            BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio));

        stack.shrink(1);
        surgeon.sendSystemMessage(Component.literal(
            "[BioForge] §bCybernetic attached: " + empty).withStyle(ChatFormatting.AQUA));
    }

    private static PlayerBioData bio(LivingEntity e) {
        if (e instanceof Player p) return BioForgeCapabilities.get(p);
        if (e instanceof Villager v) return BioForgeCapabilities.get(v);
        return null;
    }
}
