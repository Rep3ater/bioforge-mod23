package com.bioforge.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public class MedicalWallPanelBlock extends Block {
    public MedicalWallPanelBlock() { super(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(2f).requiresCorrectToolForDrops()); }
}
