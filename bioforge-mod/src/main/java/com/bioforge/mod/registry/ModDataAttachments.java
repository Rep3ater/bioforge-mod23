package com.bioforge.mod.registry;

/**
 * Data attachment registry stub.
 * Forge's data attachment system (AttachmentType) was added in Forge 1.20.4+.
 * In 1.20.1 we use capabilities instead (see BioForgeCapabilities).
 * This class exists so BioForgeMod.java compiles without changes.
 */
public class ModDataAttachments {

    /**
     * No-op register — capabilities handle all persistent data in 1.20.1.
     */
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        // Nothing to register; placeholder for future upgrade to 1.20.4+
    }
}
