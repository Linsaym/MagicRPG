package io.github.magicrpg.compat.reforged;

import static io.github.magicrpg.compat.reforged.ReforgedIntegration.page;

import io.github.magicrpg.MagicRpg;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;
import thaumcraft.wand.WandKeys;
import thaumcraft.wand.Wands;

public final class TimeTurnerMagic {
    private static final String RESEARCH = "MAGICRPG_TIME_TURNER";
    private static final String RECIPE = "magicrpg:time_turner";
    // Reforged stores vis in hundredths.
    private static final int VIS_COST = 10 * 100;

    private TimeTurnerMagic() {}

    static void registerResearchAndRecipe() {
        var timeTurner = TcStack.of(RECIPE);
        TcRecipes.register(new InfusionRecipe(RECIPE, RESEARCH, timeTurner, 6,
            new AspectList().add(Aspect.MOTION, 32).add(Aspect.MECHANISM, 24).add(Aspect.ORDER, 24)
                .add(Aspect.MAGIC, 16),
            TcIngredient.item("magicrpg:oath_core"),
            List.of(TcIngredient.item("minecraft:clock"), TcIngredient.item("magicrpg:star_prism"),
                TcIngredient.item("magicrpg:resonance_coil"), TcIngredient.item("magicrpg:chalk_heart"),
                TcIngredient.item("magicrpg:echo_rivet"), TcIngredient.item("magicrpg:city_seal"))));

        new ResearchItem(RESEARCH, ReforgedIntegration.BEASTS_CATEGORY,
            new AspectList().add(Aspect.MOTION, 4).add(Aspect.MECHANISM, 4).add(Aspect.ORDER, 3),
            8, -4, 2, timeTurner)
            .setParents(ReforgedIntegration.MEADOWS_RESEARCH)
            .setParentsHidden("INFUSION")
            .setSecondary()
            .setPages(page(RESEARCH, 1), page(RESEARCH, 2), ResearchPage.infusion(RECIPE))
            .registerResearchItem();
    }

    static void registerAspects() {
        AspectSync.addRegistrar(registry -> registry.register(MagicRpg.TIME_TURNER.get(),
            new AspectList().add(Aspect.MOTION, 8).add(Aspect.MECHANISM, 6).add(Aspect.MAGIC, 6)));
    }

    public static boolean consumeVis(ItemStack wand) {
        if (!Wands.isWand(wand)) {
            return false;
        }

        var remaining = Wands.getAllVisData(wand);
        for (Aspect aspect : Aspect.getPrimalAspects()) {
            int stored = remaining.get(aspect);
            if (stored < VIS_COST) {
                return false;
            }
            remaining = remaining.with(aspect, stored - VIS_COST);
        }
        WandKeys.VIS.set(wand, remaining);
        return true;
    }
}
