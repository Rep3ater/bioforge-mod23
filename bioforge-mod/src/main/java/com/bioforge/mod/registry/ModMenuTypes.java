package com.bioforge.mod.registry;

import com.bioforge.mod.BioForgeMod;
import com.bioforge.mod.client.SurgicalTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, BioForgeMod.MODID);

    /**
     * IForgeMenuType.create() builds a MenuType<T> whose factory calls
     * the (id, inventory, buf) constructor — exactly what SurgicalTableMenu provides.
     * This avoids the illegal MenuType.GENERIC_9x3 cast that caused the compile error.
     */
    public static final RegistryObject<MenuType<SurgicalTableMenu>> SURGICAL_TABLE =
            MENUS.register("surgical_table",
                    () -> IForgeMenuType.create(SurgicalTableMenu::new));
}
