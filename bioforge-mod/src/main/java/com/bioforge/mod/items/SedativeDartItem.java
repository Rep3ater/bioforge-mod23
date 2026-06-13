package com.bioforge.mod.items;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
import com.bioforge.mod.network.BioForgeNetwork;
import com.bioforge.mod.network.SyncBioDataPacket;
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
public class SedativeDartItem extends Item {
    public SedativeDartItem() { super(new Properties().stacksTo(16)); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) sedate(stack, user, target);
        return InteractionResult.SUCCESS;
    }
    private void sedate(ItemStack stack, Player user, LivingEntity target) {
        int dur = 1200;
        PlayerBioData bio = target instanceof Player p ? BioForgeCapabilities.get(p) : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio != null) { bio.setSedated(true, dur); bio.addCondition("sedated"); if (target instanceof ServerPlayer sp) BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio)); }
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, dur, 0));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, dur, 10));
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, dur, 2));
        stack.shrink(1);
        user.sendSystemMessage(Component.literal("[BioForge] §aTarget sedated.").withStyle(ChatFormatting.GREEN));
    }
}
