package io.github.magicrpg.compat.reforged;

import io.github.magicrpg.MagicRpg;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

public final class ReforgedIntegration {
    private static final String CATEGORY = "MAGICRPG_BEASTS";
    private static final String RESEARCH = "MAGICRPG_ASH_HOUND";
    private static final String RECIPE = "magicrpg:ash_hound_seal";

    private ReforgedIntegration() {}

    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(ReforgedIntegration::registerResearchAndRecipe);
        AspectSync.addRegistrar(registry -> {
            registry.register(MagicRpg.FANG.get(), new AspectList().add(Aspect.BEAST, 2).add(Aspect.FIRE, 2));
            registry.registerEntity(MagicRpg.ASH_HOUND.get(), new AspectList()
                .add(Aspect.BEAST, 4).add(Aspect.FIRE, 3).add(Aspect.ENTROPY, 2).add(Aspect.MOTION, 2));
        });
    }

    private static void registerResearchAndRecipe() {
        ResearchCategories.registerCategory(CATEGORY, MagicRpg.id("textures/gui/beasts.png"),
            Identifier.parse("thaumcraft:textures/gui/gui_researchback.png"));

        TcStack seal = TcStack.of(RECIPE);
        TcRecipes.register(new ShapelessArcaneRecipe(RECIPE, RESEARCH, seal,
            new AspectList().add(Aspect.FIRE, 8).add(Aspect.ENTROPY, 4),
            List.of(TcIngredient.item("minecraft:coal"), TcIngredient.item("minecraft:bone"),
                TcIngredient.item("minecraft:gunpowder"))));

        new ResearchItem(RESEARCH, CATEGORY, new AspectList().add(Aspect.FIRE, 3).add(Aspect.ENTROPY, 3),
            0, 0, 1, seal)
            .setParentsHidden("RESEARCH")
            .setSecondary()
            .setPages(ResearchPage.text("tc.research_page." + RESEARCH + ".1"),
                ResearchPage.arcane(RECIPE))
            .registerResearchItem();
    }
}
