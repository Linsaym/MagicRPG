package io.github.magicrpg.compat.reforged;

import io.github.magicrpg.MagicRpg;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.ShapelessArcaneRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.infusion.InfusionMatrixBlockEntity;
import thaumcraft.research.ResearchCategories;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchManager;
import thaumcraft.research.ResearchPage;
import thaumcraft.registry.ModBlocks;

public final class ReforgedIntegration {
    public static final String BEASTS_CATEGORY = "MAGICRPG_BEASTS";
    public static final String ILLUSTRATION_PATH = "textures/gui/research/";
    private static final String ASH_HOUND_RESEARCH = "MAGICRPG_ASH_HOUND";
    static final String FANG_RESEARCH = "MAGICRPG_CHARRED_FANG";
    static final String MEADOWS_RESEARCH = "MAGICRPG_LILAC_MEADOWS";
    private static final String INVENTORY_SCANNING_RESEARCH = "MAGICRPG_INVENTORY_SCANNING";
    private static final String WOODEN_DOORS_RESEARCH = "MAGICRPG_WOODEN_DOORS";
    private static final String RECIPE = "magicrpg:ash_hound_seal";

    private ReforgedIntegration() {}

    public static Block portalBrick() {
        return ModBlocks.ARCANE_STONE_BRICKS.get();
    }

    public static Block amberBlock() {
        return ModBlocks.AMBER_BLOCK.get();
    }

    public static void setup(FMLCommonSetupEvent event) {
        event.enqueueWork(ReforgedIntegration::registerResearchAndRecipe);
        GoldenwoodMagic.registerAspects();
        IronWastelandsMagic.registerAspects();
        StormReefsMagic.registerAspects();
        WayfarerMirrorMagic.registerAspects();
        TimeTurnerMagic.registerAspects();
        MeadowEquipmentMagic.registerAspects();
        AspectSync.addRegistrar(registry -> {
            registry.register(MagicRpg.FANG.get(), new AspectList().add(Aspect.BEAST, 2).add(Aspect.FIRE, 2));
            registry.register(MagicRpg.SEAL.get(), new AspectList()
                .add(Aspect.FIRE, 2).add(Aspect.BEAST, 1).add(Aspect.MAGIC, 1));
            registry.register(MagicRpg.WHITE_STONE_ITEM.get(), new AspectList().add(Aspect.EARTH, 3).add(Aspect.ORDER, 1));
            registry.register(MagicRpg.LILAC_GRASS_ITEM.get(), new AspectList().add(Aspect.EARTH, 2).add(Aspect.PLANT, 1));
            registry.register(MagicRpg.MEADOW_PENNANT_ITEM.get(), new AspectList().add(Aspect.CLOTH, 3).add(Aspect.ORDER, 1));
            registry.register(MagicRpg.ANCIENT_CRYSTALS_ITEM.get(), new AspectList().add(Aspect.CRYSTAL, 4).add(Aspect.LIGHT, 2));
            registry.register(MagicRpg.LILAC_LOG_ITEM.get(), new AspectList().add(Aspect.TREE, 3));
            registry.register(MagicRpg.LILAC_LEAVES_ITEM.get(), new AspectList().add(Aspect.PLANT, 2));
            registry.register(MagicRpg.LILAC_SAPLING_ITEM.get(), new AspectList().add(Aspect.PLANT, 2).add(Aspect.LIFE, 1));
            registry.register(MagicRpg.SILVERWOOD_DOOR_ITEM.get(), new AspectList()
                .add(Aspect.TREE, 3).add(Aspect.MECHANISM, 1).add(Aspect.MAGIC, 1));
            registry.register(MagicRpg.GREATWOOD_DOOR_ITEM.get(), new AspectList()
                .add(Aspect.TREE, 3).add(Aspect.MECHANISM, 1));
            registry.register(MagicRpg.CHALK_HEART.get(), new AspectList().add(Aspect.EARTH, 4).add(Aspect.MAGIC, 2));
            registry.register(MagicRpg.ECHO_RIVET.get(), new AspectList().add(Aspect.METAL, 3).add(Aspect.MOTION, 2));
            registry.register(MagicRpg.OATH_CORE.get(), new AspectList().add(Aspect.MAGIC, 8).add(Aspect.ORDER, 6));
            registry.register(MagicRpg.STAR_PRISM.get(), new AspectList().add(Aspect.LIGHT, 5).add(Aspect.ORDER, 3));
            registry.register(MagicRpg.RESONANCE_COIL.get(), new AspectList().add(Aspect.METAL, 4).add(Aspect.ENERGY, 3));
            registry.register(MagicRpg.CITY_SEAL.get(), new AspectList().add(Aspect.ORDER, 5).add(Aspect.METAL, 3));
            registry.registerEntity(MagicRpg.ASH_HOUND.get(), new AspectList()
                .add(Aspect.BEAST, 4).add(Aspect.FIRE, 3).add(Aspect.ENTROPY, 2).add(Aspect.MOTION, 2));
        });
    }

    private static void registerResearchAndRecipe() {
        ResearchCategories.registerCategory(BEASTS_CATEGORY, MagicRpg.id("textures/gui/beasts.png"),
            Identifier.parse("thaumcraft:textures/gui/gui_researchback.png"));

        TcStack seal = TcStack.of(RECIPE);
        TcRecipes.register(new ShapelessArcaneRecipe(RECIPE, ASH_HOUND_RESEARCH, seal,
            new AspectList().add(Aspect.FIRE, 8).add(Aspect.ENTROPY, 4),
            List.of(TcIngredient.item("minecraft:coal"), TcIngredient.item("minecraft:bone"),
                TcIngredient.item("minecraft:gunpowder"))));

        new ResearchItem(INVENTORY_SCANNING_RESEARCH, BEASTS_CATEGORY, new AspectList(), 0, 0, 1,
            TcStack.of("thaumcraft:thaumometer"))
            .setAutoUnlock()
            .setPages(page(INVENTORY_SCANNING_RESEARCH, 1))
            .registerResearchItem();

        var silverwoodDoor = MagicRpg.SILVERWOOD_DOOR.getId();
        var greatwoodDoor = MagicRpg.GREATWOOD_DOOR.getId();
        for (var id : List.of(silverwoodDoor, greatwoodDoor)) {
            TcRecipes.register(new CraftingRecipeRef(id.toString(), WOODEN_DOORS_RESEARCH, id, TcStack.of(id.toString(), 3)));
        }
        diary(WOODEN_DOORS_RESEARCH, silverwoodDoor.toString(), INVENTORY_SCANNING_RESEARCH, 0, 2,
            Aspect.TREE, Aspect.MECHANISM, page(WOODEN_DOORS_RESEARCH, 1),
            ResearchPage.crafting(silverwoodDoor.toString(), greatwoodDoor.toString()));

        new ResearchItem(ASH_HOUND_RESEARCH, BEASTS_CATEGORY, new AspectList().add(Aspect.FIRE, 3).add(Aspect.ENTROPY, 3),
            2, 0, 1, seal)
            .setParents(INVENTORY_SCANNING_RESEARCH)
            .setParentsHidden("RESEARCH")
            .setSecondary()
            .setPages(page(ASH_HOUND_RESEARCH, 1), ResearchPage.arcane(RECIPE))
            .registerResearchItem();

        new ResearchItem(FANG_RESEARCH, BEASTS_CATEGORY, new AspectList(), 4, 0, 1,
            TcStack.of("magicrpg:charred_fang"))
            .setParents(ASH_HOUND_RESEARCH)
            .setPages(page(FANG_RESEARCH, 1), illustration("charred_fang"))
            .registerResearchItem();

        diary(MEADOWS_RESEARCH, "magicrpg:white_stone", FANG_RESEARCH, 6, -2, Aspect.EARTH, Aspect.ORDER,
            page(MEADOWS_RESEARCH, 1), page(MEADOWS_RESEARCH, 2),
            page(MEADOWS_RESEARCH, 3), page(MEADOWS_RESEARCH, 4), page(MEADOWS_RESEARCH, 5), page(MEADOWS_RESEARCH, 6));
        TimeTurnerMagic.registerResearchAndRecipe();
        MeadowEquipmentMagic.registerResearchAndRecipes();
        WayfarerMirrorMagic.registerResearchAndRecipes();
        GoldenwoodMagic.registerResearchAndRecipe();
        IronWastelandsMagic.registerResearch();
        StormReefsMagic.registerResearchAndRecipe();
    }

    static void diary(String key, String icon, String parent, int x, int y, Aspect first, Aspect second,
                      ResearchPage... pages) {
        new ResearchItem(key, BEASTS_CATEGORY, new AspectList().add(first, 1).add(second, 1), x, y, 1, TcStack.of(icon))
            .setParents(parent).setSecondary().setPages(pages).registerResearchItem();
    }

    static ResearchPage page(String key, int page) {
        return ResearchPage.text("tc.research_page." + key + "." + page);
    }

    static ResearchPage illustration(String name) {
        return ResearchPage.image(MagicRpg.id(ILLUSTRATION_PATH + name + ".png").toString(), "");
    }

    public static void unlockFieldNotes(PlayerTickEvent.Post event) {
        // Reforged's login auto-unlock bypasses prerequisites. Check old and new knowledge through the same gate.
        if (event.getEntity() instanceof ServerPlayer player && player.tickCount % 20 == 0) {
            unlockWhenReady(player, FANG_RESEARCH);
            unlockWhenReady(player, WayfarerMirrorMagic.RESEARCH);
        }
    }

    public static void showMatrixStability(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer player
            && event.getLevel().getBlockEntity(event.getPos()) instanceof InfusionMatrixBlockEntity matrix) {
            matrix.recalculateSurroundings();
            // Reforged adds symmetry to recipe instability. Invert it to show the altar's stability bonus.
            int stability = -matrix.getSymmetry();
            String value = stability > 0 ? "+" + stability : Integer.toString(stability);
            player.sendSystemMessage(Component.translatable("message.magicrpg.matrix_stability", value));
            MatrixAsymmetry.report(player, matrix);
        }
    }

    private static void unlockWhenReady(ServerPlayer player, String key) {
        if (!ResearchManager.isComplete(player, key) && ResearchManager.hasRequisites(player, key)) {
            ResearchManager.complete(player, key);
        }
    }
}
