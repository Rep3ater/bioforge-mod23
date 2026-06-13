package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class MedicalBedBlock extends Block {
    public MedicalBedBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2f)); }
}
