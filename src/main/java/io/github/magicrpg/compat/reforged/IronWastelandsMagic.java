package io.github.magicrpg.compat.reforged;

import static io.github.magicrpg.compat.reforged.ReforgedIntegration.diary;
import static io.github.magicrpg.compat.reforged.ReforgedIntegration.page;

import io.github.magicrpg.IronWastelands;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;

final class IronWastelandsMagic {
    private static final String ENTRY = "MAGICRPG_IRON_WASTELANDS";
    private static final String HISTORY = "MAGICRPG_IRON_WASTELANDS_HISTORY";
    private static final String INHABITANTS = "MAGICRPG_IRON_WASTELANDS_INHABITANTS";

    private IronWastelandsMagic() {}

    static void registerResearch() {
        diary(ENTRY, "minecraft:iron_block", ReforgedIntegration.FANG_RESEARCH, 4, -2,
            Aspect.METAL, Aspect.ENTROPY, page(ENTRY, 1), page(ENTRY, 2));
        diary(HISTORY, "magicrpg:rusted_iron", ENTRY, 2, -4, Aspect.METAL, Aspect.MECHANISM, page(HISTORY, 1));
        diary(INHABITANTS, "minecraft:rotten_flesh", ENTRY, 4, -4, Aspect.LIFE, Aspect.ENTROPY,
            page(INHABITANTS, 1), page(INHABITANTS, 2), page(INHABITANTS, 3), page(INHABITANTS, 4));
    }

    static void registerAspects() {
        AspectSync.addRegistrar(registry -> {
            registry.register(IronWastelands.RUSTED_IRON_ITEM.get(), new AspectList()
                .add(Aspect.METAL, 4).add(Aspect.ENTROPY, 2));
            registry.register(IronWastelands.METAL_PLATES_ITEM.get(), new AspectList()
                .add(Aspect.METAL, 4).add(Aspect.ORDER, 2));
            registry.register(IronWastelands.IRON_RUBBLE_ITEM.get(), new AspectList()
                .add(Aspect.METAL, 2).add(Aspect.EARTH, 1).add(Aspect.ENTROPY, 1));
            registry.register(IronWastelands.ASHEN_SOIL_ITEM.get(), new AspectList()
                .add(Aspect.EARTH, 2).add(Aspect.FIRE, 1));
            registry.registerEntity(IronWastelands.IRON_COLOSSUS.get(), new AspectList()
                .add(Aspect.METAL, 10).add(Aspect.MECHANISM, 6).add(Aspect.MOTION, 4));
            registry.registerEntity(IronWastelands.LARGE_IRON_GOLEM.get(), new AspectList()
                .add(Aspect.METAL, 7).add(Aspect.MECHANISM, 4).add(Aspect.MOTION, 3));
            registry.registerEntity(IronWastelands.FLESH_TITAN.get(), new AspectList()
                .add(Aspect.FLESH, 9).add(Aspect.ENTROPY, 5).add(Aspect.BEAST, 4));
            registry.registerEntity(IronWastelands.HOLLOWED.get(), new AspectList()
                .add(Aspect.UNDEAD, 4).add(Aspect.FLESH, 3));
            registry.registerEntity(IronWastelands.ZOMBIE_THAUMATURGE.get(), new AspectList()
                .add(Aspect.UNDEAD, 4).add(Aspect.MAGIC, 3).add(Aspect.MOTION, 2));
        });
    }
}
