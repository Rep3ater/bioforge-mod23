package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.blocks.*;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, BioForgeMod.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BioForgeMod.MODID);

    // ─── Core functional blocks ───────────────────────────────────────────────
    public static final RegistryObject<Block> SURGICAL_TABLE =
            registerBlock("surgical_table", SurgicalTableBlock::new);
    public static final RegistryObject<Block> CLONING_VAT =
            registerBlock("cloning_vat", CloningVatBlock::new);
    public static final RegistryObject<Block> DNA_SEQUENCER =
            registerBlock("dna_sequencer", DnaSequencerBlock::new);
    public static final RegistryObject<Block> BLOOD_CENTRIFUGE =
            registerBlock("blood_centrifuge", BloodCentrifugeBlock::new);
    public static final RegistryObject<Block> ORGAN_STASIS_TANK =
            registerBlock("organ_stasis_tank", OrganStasisTankBlock::new);
    public static final RegistryObject<Block> BLOOD_STORAGE_UNIT =
            registerBlock("blood_storage_unit", BloodStorageBlock::new);
    public static final RegistryObject<Block> CLONE_GROWTH_CHAMBER =
            registerBlock("clone_growth_chamber", CloneGrowthChamberBlock::new);
    public static final RegistryObject<Block> SPECIMEN_JAR =
            registerBlock("specimen_jar", SpecimenJarBlock::new);
    public static final RegistryObject<Block> ANESTHESIA_MACHINE =
            registerBlock("anesthesia_machine", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.5f)));
    public static final RegistryObject<Block> MEDICAL_BED =
            registerBlock("medical_bed", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(2.0f)));
    public static final RegistryObject<Block> STERILE_FLOOR_TILE =
            registerBlock("sterile_floor_tile", () -> new Block(BlockBehaviour.Properties.of().strength(1.5f)));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> supplier) {
        RegistryObject<T> block = BLOCKS.register(name, supplier);
        ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }
}
