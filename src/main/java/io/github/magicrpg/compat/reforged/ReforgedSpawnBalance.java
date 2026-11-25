package io.github.magicrpg.compat.reforged;

import com.mojang.serialization.MapCodec;
import io.github.magicrpg.MagicRpg;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import thaumcraft.registry.ModEntities;

public final class ReforgedSpawnBalance implements BiomeModifier {
    private static final int FIREBAT_WEIGHT = 2;
    private static final MapCodec<ReforgedSpawnBalance> CODEC = MapCodec.unit(new ReforgedSpawnBalance());
    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, MagicRpg.ID);

    static {
        SERIALIZERS.register("reforged_spawn_balance", () -> CODEC);
    }

    private ReforgedSpawnBalance() {}

    public static void register(IEventBus bus) {
        SERIALIZERS.register(bus);
    }

    @Override
    public void modify(RegistryAccess registries, Holder<Biome> biome, Phase phase,
                       ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        // Reforged adds its regular and seasonal spawn entries during the ADD phase.
        if (phase != Phase.MODIFY) {
            return;
        }

        var settings = builder.getMobSpawnSettings();
        var monsters = settings.getSpawner(MobCategory.MONSTER);
        if (monsters == null) {
            return;
        }

        boolean warpedForest = biome.is(Biomes.WARPED_FOREST);
        var firebat = ModEntities.FIREBAT.get();
        Map<EntityType<?>, Integer> weights = warpedForest
            ? Map.of(EntityTypes.ENDERMAN, 20, ModEntities.WISP.get(), 2, firebat, FIREBAT_WEIGHT)
            : Map.of(firebat, FIREBAT_WEIGHT);
        for (var entry : List.copyOf(monsters.getList())) {
            Integer weight = weights.get(entry.value().type());
            if (weight != null) {
                monsters.remove(entry).add(entry.value(), weight);
            }
        }

        if (warpedForest) {
            var endermanCost = settings.getCost(EntityTypes.ENDERMAN);
            if (endermanCost != null) {
                settings.addMobSpawnCost(EntityTypes.ENDERMAN, endermanCost.charge(), 0.24);
            }
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() {
        return CODEC;
    }
}
