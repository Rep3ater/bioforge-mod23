package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import net.minecraft.world.inventory.MenuType;
import com.bioforge.mod.client.SurgicalTableMenu;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BioForgeMod.MODID);

    public static final RegistryObject<MenuType<SurgicalTableMenu>> SURGICAL_TABLE =
            MENUS.register("surgical_table", () -> (MenuType<SurgicalTableMenu>) MenuType.GENERIC_9x3);
}
