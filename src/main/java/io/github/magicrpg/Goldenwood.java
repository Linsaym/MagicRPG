package io.github.magicrpg;

import io.github.magicrpg.compat.reforged.GoldenwoodMagic;
import io.github.magicrpg.entity.AmberBolt;
import io.github.magicrpg.entity.HoneyMoth;
import io.github.magicrpg.entity.WoodlandMonster;
import io.github.magicrpg.world.HoneybellBlock;
import io.github.magicrpg.world.LinkedPortalBlock;
import io.github.magicrpg.world.PortalRoute;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Goldenwood {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MagicRpg.ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MagicRpg.ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, MagicRpg.ID);

    public static final DeferredBlock<Block> AMBER_GRASS = BLOCKS.registerSimpleBlock("amber_grass",
        properties -> properties.strength(0.6F).sound(SoundType.GRASS));
    public static final DeferredBlock<Block> HONEYSTONE = stone("honeystone");
    public static final DeferredBlock<Block> HONEY_BRICKS = stone("honeystone_bricks");
    public static final DeferredBlock<RotatedPillarBlock> GOLDEN_LOG = BLOCKS.registerBlock("goldenwood_log",
        RotatedPillarBlock::new, properties -> properties.strength(2).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<RotatedPillarBlock> ROTTEN_LOG = BLOCKS.registerBlock("rotten_goldenwood_log",
        RotatedPillarBlock::new, properties -> properties.strength(1).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<Block> GOLDEN_PLANKS = BLOCKS.registerSimpleBlock("goldenwood_planks",
        properties -> properties.strength(2, 3).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<LeavesBlock> GOLDEN_LEAVES = BLOCKS.registerBlock("goldenwood_leaves",
        properties -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties),
        properties -> properties.strength(0.2F).sound(SoundType.GRASS).noOcclusion().ignitedByLava());
    public static final DeferredBlock<LeavesBlock> SUNLIT_LEAVES = BLOCKS.registerBlock("sunlit_leaves",
        properties -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties),
        properties -> properties.strength(0.2F).sound(SoundType.GRASS).noOcclusion().ignitedByLava()
            .lightLevel(state -> 13));
    public static final DeferredBlock<HoneybellBlock> HONEYBELL = BLOCKS.registerBlock("honeybell",
        HoneybellBlock::new, properties -> properties.noCollision().noOcclusion().instabreak()
            .sound(SoundType.GRASS).lightLevel(state -> state.getValue(BlockStateProperties.LIT) ? 12 : 7));
    public static final DeferredBlock<LinkedPortalBlock> PORTAL = BLOCKS.registerBlock("goldenwood_portal",
        properties -> new LinkedPortalBlock(properties, PortalRoute.GOLDENWOOD),
        properties -> properties.noCollision().noOcclusion().instabreak().noLootTable().lightLevel(state -> 11));

    public static final DeferredItem<BlockItem> AMBER_GRASS_ITEM = ITEMS.registerSimpleBlockItem(AMBER_GRASS);
    public static final DeferredItem<BlockItem> HONEYSTONE_ITEM = ITEMS.registerSimpleBlockItem(HONEYSTONE);
    public static final DeferredItem<BlockItem> HONEY_BRICKS_ITEM = ITEMS.registerSimpleBlockItem(HONEY_BRICKS);
    public static final DeferredItem<BlockItem> GOLDEN_LOG_ITEM = ITEMS.registerSimpleBlockItem(GOLDEN_LOG);
    public static final DeferredItem<BlockItem> ROTTEN_LOG_ITEM = ITEMS.registerSimpleBlockItem(ROTTEN_LOG);
    public static final DeferredItem<BlockItem> GOLDEN_PLANKS_ITEM = ITEMS.registerSimpleBlockItem(GOLDEN_PLANKS);
    public static final DeferredItem<BlockItem> GOLDEN_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(GOLDEN_LEAVES);
    public static final DeferredItem<BlockItem> SUNLIT_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(SUNLIT_LEAVES);
    public static final DeferredItem<BlockItem> HONEYBELL_ITEM = ITEMS.registerSimpleBlockItem(HONEYBELL);
    public static final DeferredItem<Item> RESIN = ITEMS.registerSimpleItem("golden_resin");
    public static final DeferredItem<Item> HEART = ITEMS.registerSimpleItem("amber_heart");
    public static final DeferredItem<Item> CARAPACE = ITEMS.registerSimpleItem("amber_carapace");
    public static final DeferredItem<Item> SPARK_SHARD = ITEMS.registerSimpleItem("spark_shard");
    public static final DeferredItem<Item> POLLEN = ITEMS.registerSimpleItem("glowing_pollen");
    public static final DeferredItem<Item> MOTH_SCALE = ITEMS.registerSimpleItem("honey_moth_scale");
    public static final DeferredItem<GoldenwoodMagic.SunRing> SUN_RING = ITEMS.registerItem("predatory_sun_ring", GoldenwoodMagic.SunRing::new,
        properties -> properties.stacksTo(1));

    public static final DeferredHolder<EntityType<?>, EntityType<WoodlandMonster>> SPROUT = monster(WoodlandMonster.Kind.SPROUT, 0.8F, 1.15F, 0.84F);
    public static final DeferredHolder<EntityType<?>, EntityType<WoodlandMonster>> GUARDIAN = monster(WoodlandMonster.Kind.GUARDIAN, 1.25F, 2.95F, 2.48F);
    public static final DeferredHolder<EntityType<?>, EntityType<WoodlandMonster>> BEETLE = monster(WoodlandMonster.Kind.BEETLE, 0.65F, 0.44F, 0.27F);
    public static final DeferredHolder<EntityType<?>, EntityType<WoodlandMonster>> SPARK = monster(WoodlandMonster.Kind.SPARK, 0.8F, 1.05F, 0.56F);
    public static final DeferredHolder<EntityType<?>, EntityType<HoneyMoth>> MOTH = entity("honey_moth", EntityType.Builder.of(HoneyMoth::new, MobCategory.AMBIENT)
        .sized(0.53F, 0.33F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<AmberBolt>> BOLT = entity("amber_bolt", EntityType.Builder.<AmberBolt>of(AmberBolt::new, MobCategory.MISC)
        .sized(0.23F, 0.23F).clientTrackingRange(6).updateInterval(2));

    private Goldenwood() {}

    private static DeferredBlock<Block> stone(String name) {
        return BLOCKS.registerSimpleBlock(name,
            properties -> properties.strength(1.5F, 6).requiresCorrectToolForDrops().sound(SoundType.STONE));
    }

    private static DeferredHolder<EntityType<?>, EntityType<WoodlandMonster>> monster(WoodlandMonster.Kind kind,
                                                                                      float width, float height,
                                                                                      float eyeHeight) {
        return entity(kind.id(), EntityType.Builder.<WoodlandMonster>of(
            (type, level) -> new WoodlandMonster(type, level, kind), MobCategory.MONSTER)
            .sized(width, height).eyeHeight(eyeHeight).clientTrackingRange(10));
    }

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> entity(String id,
                                                                                           EntityType.Builder<T> builder) {
        return ENTITIES.register(id, key -> builder.build(ResourceKey.create(Registries.ENTITY_TYPE, key)));
    }

    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        bus.addListener(Goldenwood::attributes);
        bus.addListener(Goldenwood::spawnPlacements);
        NeoForge.EVENT_BUS.addListener(GoldenwoodMagic::boostMeleeDamage);
        NeoForge.EVENT_BUS.addListener(GoldenwoodMagic::recordMeleeHit);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(SPROUT.get(), WoodlandMonster.attributes(WoodlandMonster.Kind.SPROUT).build());
        event.put(GUARDIAN.get(), WoodlandMonster.attributes(WoodlandMonster.Kind.GUARDIAN).build());
        event.put(BEETLE.get(), WoodlandMonster.attributes(WoodlandMonster.Kind.BEETLE).build());
        event.put(SPARK.get(), WoodlandMonster.attributes(WoodlandMonster.Kind.SPARK).build());
        event.put(MOTH.get(), HoneyMoth.attributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        for (var type : java.util.List.of(SPROUT, GUARDIAN, BEETLE, SPARK)) {
            event.register(type.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                WoodlandMonster::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        }
        event.register(MOTH.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            HoneyMoth::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    static void addCreativeItems(CreativeModeTab.Output output) {
        ITEMS.getEntries().forEach(item -> output.accept(item.get()));
    }
}
