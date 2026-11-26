package io.github.magicrpg.compat.reforged;

import static io.github.magicrpg.compat.reforged.ReforgedIntegration.diary;
import static io.github.magicrpg.compat.reforged.ReforgedIntegration.illustration;
import static io.github.magicrpg.compat.reforged.ReforgedIntegration.page;

import io.github.magicrpg.Goldenwood;
import io.github.magicrpg.MagicRpg;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import thaumcraft.aspect.Aspect;
import thaumcraft.aspect.AspectList;
import thaumcraft.aspect.AspectSync;
import thaumcraft.crafting.CraftingRecipeRef;
import thaumcraft.crafting.InfusionRecipe;
import thaumcraft.crafting.TcIngredient;
import thaumcraft.crafting.TcRecipes;
import thaumcraft.crafting.TcStack;
import thaumcraft.platform.Bauble;
import thaumcraft.platform.BaubleType;
import thaumcraft.platform.Baubles;
import thaumcraft.research.ResearchItem;
import thaumcraft.research.ResearchPage;
import org.jspecify.annotations.Nullable;

public final class GoldenwoodMagic {
    private static final String ENTRY = "MAGICRPG_GOLDENWOOD_ENTRY";
    private static final String INHABITANTS = "MAGICRPG_GOLDENWOOD_INHABITANTS";
    private static final String RUINS = "MAGICRPG_GOLDENWOOD_RUINS";
    private static final String MATERIALS = "MAGICRPG_GOLDENWOOD_MATERIALS";
    private static final String RING = "MAGICRPG_PREDATORY_SUN";
    private static final String RECIPE = "magicrpg:predatory_sun_ring";
    private static final int STREAK_TICKS = 80;
    private static final int MAX_BONUS_HITS = 5;
    private static final float BONUS_PER_HIT = 0.12F;
    private static final Map<ServerPlayer, Combo> COMBOS = new WeakHashMap<>();

    private GoldenwoodMagic() {}

    static void registerResearchAndRecipe() {
        diary(ENTRY, "thaumcraft:amber_block", ReforgedIntegration.FANG_RESEARCH, 6, 2,
            Aspect.FIRE, Aspect.ORDER, page(ENTRY, 1), page(ENTRY, 2));
        diary(INHABITANTS, "magicrpg:golden_resin", ENTRY, 8, 0, Aspect.EARTH, Aspect.AIR, page(INHABITANTS, 1),
            page(INHABITANTS, 2), page(INHABITANTS, 3), illustration("honey_moth"));
        diary(RUINS, "magicrpg:amber_heart", ENTRY, 8, 2, Aspect.ORDER, Aspect.ENTROPY,
            page(RUINS, 1), page(RUINS, 2), page(RUINS, 3));
        for (var decoration : List.of("goldenwood_planks", "honeystone_bricks")) {
            var id = MagicRpg.id(decoration);
            TcRecipes.register(new CraftingRecipeRef(id.toString(), MATERIALS, id, TcStack.of(id.toString(), 4)));
        }
        diary(MATERIALS, "magicrpg:honeystone_bricks", ENTRY, 8, 4, Aspect.EARTH, Aspect.ORDER, page(MATERIALS, 1),
            ResearchPage.crafting("magicrpg:goldenwood_planks", "magicrpg:honeystone_bricks"));

        var ring = TcStack.of(RECIPE);
        TcRecipes.register(new InfusionRecipe(RECIPE, RING, ring, 5,
            new AspectList().add(Aspect.FIRE, 24).add(Aspect.ENERGY, 20).add(Aspect.HUNGER, 16)
                .add(Aspect.MAGIC, 12),
            TcIngredient.item("thaumcraft:mundane_ring"),
            List.of(ingredient("amber_heart"), ingredient("golden_resin"), ingredient("amber_carapace"),
                ingredient("spark_shard"), ingredient("glowing_pollen"), ingredient("honey_moth_scale"))));

        new ResearchItem(RING, ReforgedIntegration.BEASTS_CATEGORY, new AspectList().add(Aspect.FIRE, 4)
            .add(Aspect.ENERGY, 4).add(Aspect.HUNGER, 3), 10, 1, 2, ring)
            .setParents(INHABITANTS, RUINS)
            .setParentsHidden("INFUSION")
            .setSecondary()
            .setPages(page(RING, 1), page(RING, 2), ResearchPage.infusion(RECIPE), page(RING, 3))
            .registerResearchItem();
    }

    static void registerAspects() {
        AspectSync.addRegistrar(registry -> {
            registry.register(Goldenwood.AMBER_GRASS_ITEM.get(), aspects(Aspect.EARTH, 2, Aspect.PLANT, 2));
            registry.register(Goldenwood.HONEYSTONE_ITEM.get(), aspects(Aspect.EARTH, 3, Aspect.FIRE, 1));
            registry.register(Goldenwood.HONEY_BRICKS_ITEM.get(), aspects(Aspect.EARTH, 3, Aspect.CRAFT, 1));
            registry.register(Goldenwood.GOLDEN_LOG_ITEM.get(), aspects(Aspect.TREE, 4, Aspect.LIGHT, 1));
            registry.register(Goldenwood.ROTTEN_LOG_ITEM.get(), aspects(Aspect.TREE, 2, Aspect.ENTROPY, 2));
            registry.register(Goldenwood.GOLDEN_PLANKS_ITEM.get(), aspects(Aspect.TREE, 1, Aspect.CRAFT, 1));
            registry.register(Goldenwood.GOLDEN_LEAVES_ITEM.get(), aspects(Aspect.PLANT, 2, Aspect.LIGHT, 1));
            registry.register(Goldenwood.SUNLIT_LEAVES_ITEM.get(), aspects(Aspect.PLANT, 2, Aspect.LIGHT, 4));
            registry.register(Goldenwood.HONEYBELL_ITEM.get(), aspects(Aspect.PLANT, 2, Aspect.LIGHT, 3));
            registry.register(Goldenwood.RESIN.get(), aspects(Aspect.TREE, 3, Aspect.ENERGY, 2));
            registry.register(Goldenwood.HEART.get(), aspects(Aspect.LIFE, 4, Aspect.ENERGY, 4).add(Aspect.MAGIC, 3));
            registry.register(Goldenwood.CARAPACE.get(), aspects(Aspect.ARMOR, 3, Aspect.BEAST, 2));
            registry.register(Goldenwood.SPARK_SHARD.get(), aspects(Aspect.FIRE, 3, Aspect.MAGIC, 3));
            registry.register(Goldenwood.POLLEN.get(), aspects(Aspect.LIGHT, 3, Aspect.PLANT, 2));
            registry.register(Goldenwood.MOTH_SCALE.get(), aspects(Aspect.FLIGHT, 2, Aspect.LIGHT, 2));
            registry.register(Goldenwood.SUN_RING.get(), aspects(Aspect.FIRE, 5, Aspect.HUNGER, 4)
                .add(Aspect.ENERGY, 4).add(Aspect.MAGIC, 3));
            registry.registerEntity(Goldenwood.SPROUT.get(), aspects(Aspect.TREE, 4, Aspect.LIFE, 2));
            registry.registerEntity(Goldenwood.GUARDIAN.get(), aspects(Aspect.TREE, 6, Aspect.ARMOR, 4));
            registry.registerEntity(Goldenwood.BEETLE.get(), aspects(Aspect.BEAST, 3, Aspect.ARMOR, 2));
            registry.registerEntity(Goldenwood.SPARK.get(), aspects(Aspect.FIRE, 4, Aspect.FLIGHT, 3));
            registry.registerEntity(Goldenwood.MOTH.get(), aspects(Aspect.FLIGHT, 3, Aspect.LIGHT, 2));
        });
    }

    private static AspectList aspects(Aspect first, int firstAmount, Aspect second, int secondAmount) {
        return new AspectList().add(first, firstAmount).add(second, secondAmount);
    }

    private static TcIngredient ingredient(String id) {
        return TcIngredient.item("magicrpg:" + id);
    }

    private static @Nullable ServerPlayer meleeAttacker(DamageSource source) {
        if ((source.is(DamageTypes.PLAYER_ATTACK) || source.is(DamageTypes.MACE_SMASH))
            && source.getDirectEntity() instanceof ServerPlayer player) {
            return player;
        }
        return null;
    }

    private static boolean hasRing(ServerPlayer player) {
        return Baubles.isEquipped(player, Goldenwood.SUN_RING.get());
    }

    private static int previousHits(ServerPlayer player, LivingEntity target) {
        var combo = COMBOS.get(player);
        if (combo == null || !combo.target.equals(target.getUUID()) || combo.level != target.level()
            || target.level().getGameTime() > combo.expiry) {
            return 0;
        }
        return combo.hits;
    }

    public static void boostMeleeDamage(LivingDamageEvent.Pre event) {
        var player = meleeAttacker(event.getSource());
        if (player != null && hasRing(player)) {
            event.setNewDamage(event.getNewDamage() * (1 + BONUS_PER_HIT * previousHits(player, event.getEntity())));
        }
    }

    public static void recordMeleeHit(LivingDamageEvent.Post event) {
        var player = meleeAttacker(event.getSource());
        if (player != null && hasRing(player) && event.getHealthDamage() > 0) {
            var target = event.getEntity();
            COMBOS.put(player, new Combo(target.getUUID(), target.level(),
                target.level().getGameTime() + STREAK_TICKS,
                Math.min(MAX_BONUS_HITS, previousHits(player, target) + 1)));
        }
    }

    private record Combo(java.util.UUID target, net.minecraft.world.level.Level level, long expiry, int hits) {}

    public static final class SunRing extends Item implements Bauble {
        public SunRing(Properties properties) {
            super(properties);
        }

        @Override
        public BaubleType getBaubleType(ItemStack stack) {
            return BaubleType.RING;
        }

        @Override
        public void onUnequipped(ItemStack stack, LivingEntity wearer) {
            if (wearer instanceof ServerPlayer player) {
                COMBOS.remove(player);
            }
        }
    }
}
