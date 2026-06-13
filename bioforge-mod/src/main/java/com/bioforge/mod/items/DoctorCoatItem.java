package com.bioforge.mod.items;

import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.common.ForgeMod;

/**
 * Doctor Coat — chestplate slot armor item.
 * Wearing this enables the BioForge HUD and allows scanning nearby entities.
 * Detected in BioForgeHud and BioForgeEvents via ArmorItem type check.
 */
public class DoctorCoatItem extends ArmorItem {

    public static final ArmorMaterial DOCTOR_MATERIAL = new ArmorMaterial() {
        @Override public int getDurabilityForType(Type t) { return 80; }
        @Override public int getDefenseForType(Type t) { return t == Type.CHESTPLATE ? 4 : 0; }
        @Override public int getEnchantmentValue() { return 10; }
        @Override public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_LEATHER; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(net.minecraft.world.item.Items.WHITE_WOOL); }
        @Override public String getName() { return "bioforge:doctor_coat"; }
        @Override public float getToughness() { return 0f; }
        @Override public float getKnockbackResistance() { return 0f; }
    };

    public DoctorCoatItem() {
        super(DOCTOR_MATERIAL, Type.CHESTPLATE, new Item.Properties());
    }
}
