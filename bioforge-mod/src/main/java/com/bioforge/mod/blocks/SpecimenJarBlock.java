package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class SpecimenJarBlock extends Block {
    public SpecimenJarBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.5f).noOcclusion().lightLevel(s->3)); }
}
