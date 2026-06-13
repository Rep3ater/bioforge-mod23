package com.bioforge.mod.items;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.network.SyncBioDataPacket;
import com.bioforge.mod.network.BioForgeNetwork;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
public class MorphineItem extends Item {
    public MorphineItem() { super(new Properties().stacksTo(4)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) dose(user.getItemInHand(hand), user, user);
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) dose(stack, user, target);
        return InteractionResult.SUCCESS;
    }
    private void dose(ItemStack stack, Player surgeon, LivingEntity target) {
        PlayerBioData bio = target instanceof Player p ? BioForgeCapabilities.get(p) : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio != null) { bio.reducePain(5f); bio.addCondition("on_morphine"); if (target instanceof ServerPlayer sp) BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio)); }
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600, 0));
        target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
        target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        stack.shrink(1);
        surgeon.sendSystemMessage(Component.literal("[BioForge] §aMorphine administered.").withStyle(ChatFormatting.GREEN));
    }
}
