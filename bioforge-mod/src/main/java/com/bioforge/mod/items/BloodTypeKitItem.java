package com.bioforge.mod.items;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.core.BloodType;
import com.bioforge.mod.network.BioForgeNetwork;
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
public class BloodTypeKitItem extends Item {
    public BloodTypeKitItem() { super(new Properties().stacksTo(8)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) test(user.getItemInHand(hand), user, user);
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) test(stack, user, target);
        return InteractionResult.SUCCESS;
    }
    private void test(ItemStack stack, Player user, LivingEntity target) {
        PlayerBioData bio = target instanceof Player p ? BioForgeCapabilities.get(p) : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio == null) { user.sendSystemMessage(Component.literal("[BioForge] Can't test this entity.").withStyle(ChatFormatting.RED)); return; }
        if (bio.getBloodType() == BloodType.UNKNOWN) { bio.setBloodType(BloodType.random(target.level().random)); if (target instanceof ServerPlayer sp) BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio)); }
        user.sendSystemMessage(Component.literal("[BioForge] Blood Type: §c" + bio.getBloodType().getDisplay()).withStyle(ChatFormatting.RED));
        stack.shrink(1);
    }
}
