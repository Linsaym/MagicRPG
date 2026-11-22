package io.github.magicrpg.compat.reforged;

import io.github.magicrpg.WayfarerMirrors;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.item.DyeColor;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;

final class WayfarerMirrorMagic {
    static final String RESEARCH = "MAGICRPG_WAYFARER_MIRRORS";
    private static final String PAIR_DATA = "magicrpg:mirror_pair";

    private WayfarerMirrorMagic() {}

    static void registerResearchAndRecipes() {
        // The workbench moves its result stack into the inventory before the shift-click craft callback.
        TcStack.registerDataHandler(PAIR_DATA, (stack, value) -> stack.set(WayfarerMirrors.PAIR.get(), UUID.randomUUID()));
        for (var color : DyeColor.VALUES) {
            var id = itemId(color);
            var pair = TcStack.withData(id, 2, Map.of(PAIR_DATA, "new"));
            TcRecipes.register(new ShapelessArcaneRecipe(id, RESEARCH, pair,
                new AspectList().add(Aspect.ORDER, 8).add(Aspect.AIR, 4),
                List.of(TcIngredient.item("magicrpg:white_stone"), TcIngredient.item("thaumcraft:mirrored_glass"),
                    TcIngredient.item("minecraft:" + color.getSerializedName() + "_dye"))));
        }

        new ResearchItem(RESEARCH, ReforgedIntegration.BEASTS_CATEGORY, new AspectList(), 8, -2, 1,
            TcStack.of(itemId(DyeColor.GREEN)))
            .setParents(ReforgedIntegration.MEADOWS_RESEARCH)
            .setPages(ReforgedIntegration.page(RESEARCH, 1), ReforgedIntegration.page(RESEARCH, 2),
                ResearchPage.arcane(itemId(DyeColor.RED), itemId(DyeColor.GREEN), itemId(DyeColor.PINK)))
            .registerResearchItem();
    }

    static void registerAspects() {
        AspectSync.addRegistrar(registry -> {
            for (var color : DyeColor.VALUES) {
                registry.register(WayfarerMirrors.block(color).asItem(),
                    new AspectList().add(Aspect.TRAVEL, 3).add(Aspect.CRYSTAL, 2).add(Aspect.MAGIC, 2));
            }
        });
    }

    private static String itemId(DyeColor color) {
        return "magicrpg:" + WayfarerMirrors.name(color);
    }
}
