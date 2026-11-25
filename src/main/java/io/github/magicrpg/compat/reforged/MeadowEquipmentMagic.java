package io.github.magicrpg.compat.reforged;

import static io.github.magicrpg.compat.reforged.ReforgedIntegration.illustration;
import static io.github.magicrpg.compat.reforged.ReforgedIntegration.page;

import io.github.magicrpg.MagicRpg;
import java.util.List;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class MeadowEquipmentMagic {
    private static final String MERCHANT = "MAGICRPG_ANCIENT_MERCHANT";
    private static final String POUCH = "MAGICRPG_ARMOR_POUCH";
    private static final String SEAL_RECIPE = "magicrpg:ancient_merchant_seal";
    private static final String POUCH_RECIPE = "magicrpg:armor_pouch";

    private MeadowEquipmentMagic() {}

    static void registerResearchAndRecipes() {
        var seal = TcStack.of(SEAL_RECIPE);
        TcRecipes.register(new ShapelessArcaneRecipe(SEAL_RECIPE, MERCHANT, seal,
            new AspectList().add(Aspect.ORDER, 12).add(Aspect.EARTH, 8),
            List.of(TcIngredient.item("minecraft:paper"), TcIngredient.item("magicrpg:chalk_heart"),
                TcIngredient.item("magicrpg:echo_rivet"))));

        new ResearchItem(MERCHANT, ReforgedIntegration.BEASTS_CATEGORY, new AspectList(), 6, -4, 1, seal)
            .setParents(ReforgedIntegration.MEADOWS_RESEARCH)
            .setAutoUnlock()
            .setPages(page(MERCHANT, 1), ResearchPage.arcane(SEAL_RECIPE),
                page(MERCHANT, 2), page(MERCHANT, 3), illustration("lilac_armor_front"), illustration("lilac_armor_back"))
            .registerResearchItem();

        TcRecipes.register(new ShapelessArcaneRecipe(POUCH_RECIPE, POUCH, TcStack.of(POUCH_RECIPE),
            new AspectList().add(Aspect.ORDER, 10).add(Aspect.AIR, 8),
            List.of(TcIngredient.item("minecraft:leather"), TcIngredient.item("minecraft:string"),
                TcIngredient.item("minecraft:leather"), TcIngredient.item("magicrpg:echo_rivet"))));
        ReforgedIntegration.diary(POUCH, POUCH_RECIPE, MERCHANT, 6, -6, Aspect.CLOTH, Aspect.MOTION,
            page(POUCH, 1), page(POUCH, 2), ResearchPage.arcane(POUCH_RECIPE));
    }

    static void registerAspects() {
        AspectSync.addRegistrar(registry -> {
            registry.register(MagicRpg.MERCHANT_SEAL.get(),
                new AspectList().add(Aspect.MAGIC, 4).add(Aspect.ORDER, 4).add(Aspect.EXCHANGE, 2));
            registry.register(MagicRpg.ARMOR_POUCH.get(),
                new AspectList().add(Aspect.CLOTH, 5).add(Aspect.VOID, 4).add(Aspect.MOTION, 3));
            for (var part : List.of(MagicRpg.LILAC_HELMET, MagicRpg.LILAC_CHESTPLATE,
                     MagicRpg.LILAC_LEGGINGS, MagicRpg.LILAC_BOOTS)) {
                registry.register(part.get(), new AspectList().add(Aspect.ARMOR, 4).add(Aspect.HEAL, 4)
                    .add(Aspect.METAL, 3));
            }
            registry.registerEntity(MagicRpg.ANCIENT_MERCHANT.get(),
                new AspectList().add(Aspect.MAN, 4).add(Aspect.EXCHANGE, 6).add(Aspect.ORDER, 3));
        });
    }
}
