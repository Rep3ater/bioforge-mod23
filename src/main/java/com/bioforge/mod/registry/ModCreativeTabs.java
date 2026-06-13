package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, BioForgeMod.MODID);

    public static final RegistryObject<CreativeModeTab> BIOFORGE_TAB = TABS.register("bioforge_tab", () ->
            CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.bioforge"))
                    .icon(() -> ModItems.SCALPEL.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        // Doctor equipment
                        output.accept(ModItems.DOCTOR_COAT.get());
                        // Tools
                        output.accept(ModItems.SCALPEL.get());
                        output.accept(ModItems.BONE_SAW.get());
                        output.accept(ModItems.CAUTERIZER.get());
                        // Anesthesia
                        output.accept(ModItems.ANESTHETIC_SYRINGE.get());
                        output.accept(ModItems.SEDATIVE_DART.get());
                        // Blood & DNA
                        output.accept(ModItems.BLOOD_SYRINGE_EMPTY.get());
                        output.accept(ModItems.BLOOD_SYRINGE_FILLED.get());
                        output.accept(ModItems.BLOOD_BAG.get());
                        output.accept(ModItems.BLOOD_TYPE_KIT.get());
                        output.accept(ModItems.DNA_EXTRACTOR.get());
                        output.accept(ModItems.DNA_SAMPLE.get());
                        // Cloning
                        output.accept(ModItems.CLONING_CATALYST.get());
                        output.accept(ModItems.CLONE_MATRIX.get());
                        output.accept(ModItems.MEMORY_CRYSTAL.get());
                        // Limbs
                        output.accept(ModItems.PROSTHETIC_ARM.get());
                        output.accept(ModItems.PROSTHETIC_LEG.get());
                        output.accept(ModItems.BIOLOGICAL_ARM.get());
                        output.accept(ModItems.BIOLOGICAL_LEG.get());
                        output.accept(ModItems.CYBERNETIC_ARM.get());
                        output.accept(ModItems.CYBERNETIC_LEG.get());
                        // Organs
                        output.accept(ModItems.HEART.get());
                        output.accept(ModItems.LIVER.get());
                        output.accept(ModItems.KIDNEY.get());
                        output.accept(ModItems.LUNG.get());
                        // Pain
                        output.accept(ModItems.MORPHINE.get());
                        // Body Exit
                        output.accept(ModItems.SPIRIT_CATALYST.get());
                        // Blocks
                        output.accept(ModBlocks.SURGICAL_TABLE.get());
                        output.accept(ModBlocks.CLONING_VAT.get());
                        output.accept(ModBlocks.DNA_SEQUENCER.get());
                        output.accept(ModBlocks.BLOOD_CENTRIFUGE.get());
                        output.accept(ModBlocks.ORGAN_STASIS_TANK.get());
                        output.accept(ModBlocks.BLOOD_STORAGE_UNIT.get());
                        output.accept(ModBlocks.CLONE_GROWTH_CHAMBER.get());
                        output.accept(ModBlocks.SPECIMEN_JAR.get());
                    })
                    .build()
    );
}
