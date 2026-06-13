package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.items.*;
import com.bioforge.mod.items.SpiritCatalystItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BioForgeMod.MODID);

    // ─── Blood & DNA ──────────────────────────────────────────────────────────
    public static final RegistryObject<Item> BLOOD_SYRINGE_EMPTY =
            ITEMS.register("blood_syringe_empty", BloodSyringeItem::new);
    public static final RegistryObject<Item> BLOOD_SYRINGE_FILLED =
            ITEMS.register("blood_syringe_filled", BloodSyringeFilled::new);
    public static final RegistryObject<Item> DNA_EXTRACTOR =
            ITEMS.register("dna_extractor", DnaExtractorItem::new);
    public static final RegistryObject<Item> DNA_SAMPLE =
            ITEMS.register("dna_sample", DnaSampleItem::new);
    public static final RegistryObject<Item> BLOOD_BAG =
            ITEMS.register("blood_bag", BloodBagItem::new);
    public static final RegistryObject<Item> BLOOD_TYPE_KIT =
            ITEMS.register("blood_type_kit", BloodTypeKitItem::new);

    // ─── Cloning ──────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> CLONING_CATALYST =
            ITEMS.register("cloning_catalyst", CloningCatalystItem::new);
    public static final RegistryObject<Item> CLONE_MATRIX =
            ITEMS.register("clone_matrix", CloneMatrixItem::new);
    public static final RegistryObject<Item> MEMORY_CRYSTAL =
            ITEMS.register("memory_crystal", MemoryCrystalItem::new);

    // ─── Surgical Tools ───────────────────────────────────────────────────────
    public static final RegistryObject<Item> SCALPEL =
            ITEMS.register("scalpel", ScalpelItem::new);
    public static final RegistryObject<Item> BONE_SAW =
            ITEMS.register("bone_saw", BoneSawItem::new);
    public static final RegistryObject<Item> CAUTERIZER =
            ITEMS.register("cauterizer", CauterizerItem::new);

    // ─── Anesthesia ───────────────────────────────────────────────────────────
    public static final RegistryObject<Item> ANESTHETIC_SYRINGE =
            ITEMS.register("anesthetic_syringe", AnestheticSyringeItem::new);
    public static final RegistryObject<Item> SEDATIVE_DART =
            ITEMS.register("sedative_dart", SedativeDartItem::new);

    // ─── Limbs ────────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> PROSTHETIC_ARM =
            ITEMS.register("prosthetic_arm", ProstheticLimbItem::ofArm);
    public static final RegistryObject<Item> PROSTHETIC_LEG =
            ITEMS.register("prosthetic_leg", ProstheticLimbItem::ofLeg);
    public static final RegistryObject<Item> BIOLOGICAL_ARM =
            ITEMS.register("biological_arm", BiologicalLimbItem::ofArm);
    public static final RegistryObject<Item> BIOLOGICAL_LEG =
            ITEMS.register("biological_leg", BiologicalLimbItem::ofLeg);
    public static final RegistryObject<Item> CYBERNETIC_ARM =
            ITEMS.register("cybernetic_arm", CyberneticLimbItem::ofArm);
    public static final RegistryObject<Item> CYBERNETIC_LEG =
            ITEMS.register("cybernetic_leg", CyberneticLimbItem::ofLeg);

    // ─── Organs ───────────────────────────────────────────────────────────────
    public static final RegistryObject<Item> HEART =
            ITEMS.register("heart", OrganItem::heart);
    public static final RegistryObject<Item> LIVER =
            ITEMS.register("liver", OrganItem::liver);
    public static final RegistryObject<Item> KIDNEY =
            ITEMS.register("kidney", OrganItem::kidney);
    public static final RegistryObject<Item> LUNG =
            ITEMS.register("lung", OrganItem::lung);

    // ─── Pain Management ──────────────────────────────────────────────────────
    public static final RegistryObject<Item> MORPHINE =
            ITEMS.register("morphine", MorphineItem::new);

    // ─── Doctor Equipment ─────────────────────────────────────────────────────
    public static final RegistryObject<Item> DOCTOR_COAT =
            ITEMS.register("doctor_coat", DoctorCoatItem::new);

    // ─── Body Exit (Astral Projection) ───────────────────────────────────────
    public static final RegistryObject<Item> SPIRIT_CATALYST =
            ITEMS.register("spirit_catalyst", SpiritCatalystItem::new);

}
