package io.github.magicrpg.compat.reforged;

import static io.github.magicrpg.compat.reforged.ReforgedIntegration.diary;
import static io.github.magicrpg.compat.reforged.ReforgedIntegration.page;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.StormReefs;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchPage;

final class StormReefsMagic {
    private static final String ENTRY = "MAGICRPG_STORM_REEFS";
    private static final String PILOT = "MAGICRPG_STORM_PILOT";
    private static final String REEFHEART = "MAGICRPG_REEFHEART";
    private static final String BELL = "magicrpg:storm_bell";

    private StormReefsMagic() {}

    static void registerResearchAndRecipe() {
        diary(ENTRY, "minecraft:dark_prismarine", ReforgedIntegration.FANG_RESEARCH, 4, 2,
            Aspect.WATER, Aspect.WEATHER, page(ENTRY, 1), page(ENTRY, 2));
        TcRecipes.register(new CraftingRecipeRef(BELL, PILOT, MagicRpg.id("storm_bell"), TcStack.of(BELL)));
        diary(PILOT, BELL, ENTRY, 4, 4, Aspect.WEATHER, Aspect.MECHANISM, page(PILOT, 1), page(PILOT, 2),
            ResearchPage.crafting(BELL));
        diary(REEFHEART, "magicrpg:reefheart", PILOT, 4, 6, Aspect.WATER, Aspect.LIFE, page(REEFHEART, 1));
    }

    static void registerAspects() {
        AspectSync.addRegistrar(registry -> {
            registry.register(StormReefs.BELL.get(), new AspectList().add(Aspect.METAL, 3).add(Aspect.CRYSTAL, 3)
                .add(Aspect.WEATHER, 4));
            registry.register(StormReefs.REEFHEART.get(), new AspectList().add(Aspect.WATER, 8).add(Aspect.MOTION, 6)
                .add(Aspect.LIFE, 4).add(Aspect.MAGIC, 4));
            registry.registerEntity(StormReefs.STORM_PILOT.get(), new AspectList().add(Aspect.WEATHER, 10)
                .add(Aspect.MECHANISM, 8).add(Aspect.WATER, 6).add(Aspect.ENERGY, 5));
        });
    }
}
