package com.bioforge.mod.items;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.BloodType;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.PlayProcedureEffectPacket;
import com.bioforge.mod.network.SyncBioDataPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
public class BloodSyringeFilled extends Item {
    public BloodSyringeFilled() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) transfuse(user.getItemInHand(hand), user, user);
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) transfuse(stack, user, target);
        return InteractionResult.SUCCESS;
    }
    private void transfuse(ItemStack stack, Player surgeon, LivingEntity target) {
        var tag = stack.getTag(); if (tag == null) return;
        float vol = tag.getFloat("volume");
        BloodType donor = BloodType.fromString(tag.getString("blood_type"));
        PlayerBioData bio = target instanceof Player p ? BioForgeCapabilities.get(p) : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio != null) {
            if (!bio.getBloodType().canReceiveFrom(donor)) { target.hurt(target.damageSources().generic(), 4f); bio.addCondition("transfusion_reaction"); surgeon.sendSystemMessage(Component.literal("[BioForge] §4TRANSFUSION REACTION!").withStyle(ChatFormatting.DARK_RED)); }
            else { bio.addBlood(vol); surgeon.sendSystemMessage(Component.literal("[BioForge] §aTransfused " + vol + "L " + donor.getDisplay()).withStyle(ChatFormatting.GREEN)); }
            if (target instanceof ServerPlayer sp) BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio));
        }
        BioForgeNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(target.getX(), target.getY(), target.getZ(), 24, target.level().dimension())), new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.TRANSFUSION, target.position(), target.getId()));
        stack.shrink(1);
    }
}
