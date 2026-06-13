package com.bioforge.mod.items;

import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
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

/**
 * Organ item — right-click entity OR right-click air (self).
 * Transplants the organ. Requires sedation OR sneak to force.
 */
public class OrganItem extends Item {
    private final String organSlot;

    private OrganItem(String slot) {
        super(new Properties().stacksTo(1));
        this.organSlot = slot;
    }

    public static OrganItem heart()  { return new OrganItem("heart"); }
    public static OrganItem liver()  { return new OrganItem("liver"); }
    public static OrganItem kidney() { return new OrganItem("kidney"); }
    public static OrganItem lung()   { return new OrganItem("lung"); }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) transplant(user.getItemInHand(hand), user, user);
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user,
                                                   LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) transplant(stack, user, target);
        return InteractionResult.SUCCESS;
    }

    private void transplant(ItemStack stack, Player surgeon, LivingEntity target) {
        PlayerBioData bio = bio(target);
        if (bio == null) return;
        boolean isSelf = target == surgeon;

        if (!bio.isSedated() && !isSelf && !surgeon.isShiftKeyDown()) {
            surgeon.sendSystemMessage(Component.literal(
                "[BioForge] Target must be sedated. Sneak+click to force.").withStyle(ChatFormatting.RED));
            return;
        }

        String donorDna = stack.getOrCreateTag().getString("donor_dna");
        bio.transplantOrgan(organSlot, donorDna);
        bio.removeBlood(0.3f);
        bio.addPain(isSelf ? 6f : 4f);
        bio.addMedicalHistoryEntry("[T] " + organSlot + " transplanted"
            + (isSelf ? " (self)" : " by " + surgeon.getName().getString()));

        BioForgeNetwork.CHANNEL.send(
            PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                target.getX(), target.getY(), target.getZ(), 24, target.level().dimension())),
            new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.ORGAN_IMPLANT,
                target.position(), target.getId()));

        if (target instanceof ServerPlayer sp)
            BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio));

        stack.shrink(1);
        surgeon.sendSystemMessage(Component.literal(
            "[BioForge] §a" + organSlot + " transplanted. Watch for rejection.").withStyle(ChatFormatting.GREEN));
    }

    private static PlayerBioData bio(LivingEntity e) {
        if (e instanceof Player p) return BioForgeCapabilities.get(p);
        if (e instanceof Villager v) return BioForgeCapabilities.get(v);
        return null;
    }
}
