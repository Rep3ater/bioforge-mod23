package com.bioforge.mod.items;
import com.bioforge.mod.capabilities.BioForgeCapabilities;
import com.bioforge.mod.capabilities.PlayerBioData;
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
public class CauterizerItem extends Item {
    public CauterizerItem() { super(new Properties().stacksTo(1).durability(32)); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player user, InteractionHand hand) {
        if (!level.isClientSide) cauterize(user.getItemInHand(hand), user, user, hand);
        return InteractionResultHolder.success(user.getItemInHand(hand));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide) cauterize(stack, user, target, hand);
        return InteractionResult.SUCCESS;
    }
    private void cauterize(ItemStack stack, Player surgeon, LivingEntity target, InteractionHand hand) {
        PlayerBioData bio = target instanceof Player p ? BioForgeCapabilities.get(p) : target instanceof Villager v ? BioForgeCapabilities.get(v) : null;
        if (bio != null) {
            bio.getActiveConditions().stream().filter(c -> c.startsWith("bleeding_")).findFirst().ifPresent(b -> { bio.removeCondition(b); bio.addPain(2f); });
            bio.removeCondition("incision_open");
            if (target instanceof ServerPlayer sp) BioForgeNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), new SyncBioDataPacket(bio));
        }
        target.setSecondsOnFire(1);
        BioForgeNetwork.CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(target.getX(), target.getY(), target.getZ(), 24, target.level().dimension())), new PlayProcedureEffectPacket(PlayProcedureEffectPacket.EffectType.CAUTERIZE, target.position(), target.getId()));
        stack.hurtAndBreak(1, surgeon, p -> p.broadcastBreakEvent(hand));
        surgeon.sendSystemMessage(Component.literal("[BioForge] §eWound cauterized.").withStyle(ChatFormatting.YELLOW));
    }
}
