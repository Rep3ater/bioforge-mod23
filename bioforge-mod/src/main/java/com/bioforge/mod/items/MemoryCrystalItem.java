package com.bioforge.mod.items;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
public class MemoryCrystalItem extends Item {
    public MemoryCrystalItem() { super(new Properties().stacksTo(1)); }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity target, InteractionHand hand) {
        if (!user.level().isClientSide && target instanceof Player patient) {
            var tag = stack.getOrCreateTag(); tag.putString("owner_uuid", patient.getUUID().toString()); tag.putString("owner_name", patient.getName().getString());
            stack.setHoverName(Component.literal("Memory Crystal [" + patient.getName().getString() + "]").withStyle(ChatFormatting.LIGHT_PURPLE));
            user.sendSystemMessage(Component.literal("[BioForge] §dCrystal attuned to " + patient.getName().getString()).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        return InteractionResult.SUCCESS;
    }
}
