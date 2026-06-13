package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.blocks.CloningVatBlockEntity;
import com.bioforge.mod.blocks.SurgicalTableBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, BioForgeMod.MODID);

    public static final RegistryObject<BlockEntityType<SurgicalTableBlockEntity>> SURGICAL_TABLE =
            BLOCK_ENTITY_TYPES.register("surgical_table", () ->
                    BlockEntityType.Builder.of(SurgicalTableBlockEntity::new,
                            ModBlocks.SURGICAL_TABLE.get()).build(null));

    public static final RegistryObject<BlockEntityType<CloningVatBlockEntity>> CLONING_VAT =
            BLOCK_ENTITY_TYPES.register("cloning_vat", () ->
                    BlockEntityType.Builder.of(CloningVatBlockEntity::new,
                            ModBlocks.CLONING_VAT.get()).build(null));
}
