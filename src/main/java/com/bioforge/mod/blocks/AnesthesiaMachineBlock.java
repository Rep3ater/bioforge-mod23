package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class AnesthesiaMachineBlock extends Block {
    public AnesthesiaMachineBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3f).requiresCorrectToolForDrops()); }
}
