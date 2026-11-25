package io.github.magicrpg;

import io.github.magicrpg.compat.reforged.ReforgedIntegration;
import io.github.magicrpg.compat.reforged.ReforgedSpawnBalance;
import io.github.magicrpg.entity.AncientMerchant;
import io.github.magicrpg.entity.AshHound;
import io.github.magicrpg.entity.ChalkGuardian;
import io.github.magicrpg.entity.CitySentry;
import io.github.magicrpg.entity.MeadowPaladin;
import io.github.magicrpg.entity.StarHare;
import io.github.magicrpg.item.ArmorPouch;
import io.github.magicrpg.item.CharredFang;
import io.github.magicrpg.item.LilacArmor;
import io.github.magicrpg.item.SummoningSeal;
import io.github.magicrpg.item.TimeTurner;
import io.github.magicrpg.menu.ArmorPouchMenu;
import io.github.magicrpg.network.ScanInventorySlotPayload;
import io.github.magicrpg.world.IslandExport;
import io.github.magicrpg.world.LinkedPortalBlock;
import io.github.magicrpg.world.MeadowPennantBlock;
import io.github.magicrpg.world.PortalRoute;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MagicRpg.ID)
public final class MagicRpg {
    public static final String ID = "magicrpg";

    static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final DeferredItem<SummoningSeal> SEAL = ITEMS.registerItem("ash_hound_seal", SummoningSeal::new);
    public static final DeferredItem<CharredFang> FANG = ITEMS.registerItem("charred_fang", CharredFang::new);
    public static final DeferredItem<Item> CHALK_HEART = ITEMS.registerSimpleItem("chalk_heart");
    public static final DeferredItem<Item> ECHO_RIVET = ITEMS.registerSimpleItem("echo_rivet");
    public static final DeferredItem<Item> OATH_CORE = ITEMS.registerSimpleItem("oath_core",
        properties -> properties.rarity(Rarity.RARE));
    public static final DeferredItem<Item> STAR_PRISM = ITEMS.registerSimpleItem("star_prism",
        properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> RESONANCE_COIL = ITEMS.registerSimpleItem("resonance_coil");
    public static final DeferredItem<Item> CITY_SEAL = ITEMS.registerSimpleItem("city_seal",
        properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<TimeTurner> TIME_TURNER = ITEMS.registerItem("time_turner", TimeTurner::new,
        properties -> properties.stacksTo(1).rarity(Rarity.RARE));
    public static final DeferredItem<SummoningSeal> MERCHANT_SEAL = ITEMS.registerItem("ancient_merchant_seal",
        MagicRpg::merchantSeal, properties -> properties.rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> LILAC_HELMET = lilacArmor("lilac_helmet", ArmorType.HELMET);
    public static final DeferredItem<Item> LILAC_CHESTPLATE = lilacArmor("lilac_chestplate", ArmorType.CHESTPLATE);
    public static final DeferredItem<Item> LILAC_LEGGINGS = lilacArmor("lilac_leggings", ArmorType.LEGGINGS);
    public static final DeferredItem<Item> LILAC_BOOTS = lilacArmor("lilac_boots", ArmorType.BOOTS);
    public static final DeferredItem<ArmorPouch> ARMOR_POUCH = ITEMS.registerItem("armor_pouch", ArmorPouch::new,
        properties -> properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    public static final DeferredHolder<MenuType<?>, MenuType<ArmorPouchMenu>> ARMOR_POUCH_MENU = MENUS.register(
        "armor_pouch", () -> IMenuTypeExtension.create(ArmorPouchMenu::new));
    public static final DeferredBlock<Block> WHITE_STONE = BLOCKS.registerSimpleBlock("white_stone",
        properties -> properties.strength(1.5F, 6).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> LILAC_GRASS = BLOCKS.registerSimpleBlock("lilac_grass",
        properties -> properties.strength(0.6F).sound(SoundType.GRASS));
    public static final DeferredBlock<MeadowPennantBlock> MEADOW_PENNANT = BLOCKS.registerBlock("meadow_pennant",
        MeadowPennantBlock::new, properties -> properties.noCollision().noOcclusion().instabreak()
            .sound(SoundType.WOOL).ignitedByLava());
    public static final DeferredBlock<AmethystClusterBlock> ANCIENT_CRYSTALS = BLOCKS.registerBlock("ancient_crystals",
        properties -> new AmethystClusterBlock(12, 3, properties), properties -> properties.noOcclusion()
            .strength(1.5F).sound(SoundType.AMETHYST_CLUSTER).lightLevel(state -> 11));
    public static final DeferredBlock<RotatedPillarBlock> LILAC_LOG = BLOCKS.registerBlock("lilac_log",
        RotatedPillarBlock::new, properties -> properties.strength(2).sound(SoundType.WOOD).ignitedByLava());
    public static final DeferredBlock<LeavesBlock> LILAC_LEAVES = BLOCKS.registerBlock("lilac_leaves",
        properties -> new LeavesBlock(AmbientLeavesBlockSoundPlayer.noAmbientSound(), properties),
        properties -> properties.strength(0.2F).sound(SoundType.GRASS).noOcclusion().ignitedByLava());
    private static final ResourceKey<Feature> LILAC_TREE_FEATURE = ResourceKey.create(Registries.FEATURE, id("lilac_tree"));
    private static final TreeGrower LILAC_TREE = new TreeGrower("magicrpg:lilac_tree",
        WeightedList.of(LILAC_TREE_FEATURE), WeightedList.of(), WeightedList.of(), LILAC_TREE_FEATURE);
    public static final DeferredBlock<SaplingBlock> LILAC_SAPLING = BLOCKS.registerBlock("lilac_sapling",
        properties -> new SaplingBlock(LILAC_TREE, properties),
        properties -> properties.noCollision().instabreak().randomTicks().sound(SoundType.GRASS));
    public static final DeferredBlock<LinkedPortalBlock> MEADOW_PORTAL = BLOCKS.registerBlock("meadow_portal",
        properties -> new LinkedPortalBlock(properties, PortalRoute.MEADOWS), properties -> properties.noCollision().noOcclusion().instabreak()
            .noLootTable().lightLevel(state -> 10));
    public static final DeferredItem<BlockItem> WHITE_STONE_ITEM = ITEMS.registerSimpleBlockItem(WHITE_STONE);
    public static final DeferredItem<BlockItem> LILAC_GRASS_ITEM = ITEMS.registerSimpleBlockItem(LILAC_GRASS);
    public static final DeferredItem<BlockItem> MEADOW_PENNANT_ITEM = ITEMS.registerSimpleBlockItem(MEADOW_PENNANT);
    public static final DeferredItem<BlockItem> ANCIENT_CRYSTALS_ITEM = ITEMS.registerSimpleBlockItem(ANCIENT_CRYSTALS);
    public static final DeferredItem<BlockItem> LILAC_LOG_ITEM = ITEMS.registerSimpleBlockItem(LILAC_LOG);
    public static final DeferredItem<BlockItem> LILAC_LEAVES_ITEM = ITEMS.registerSimpleBlockItem(LILAC_LEAVES);
    public static final DeferredItem<BlockItem> LILAC_SAPLING_ITEM = ITEMS.registerSimpleBlockItem(LILAC_SAPLING);

    private static final DeferredHolder<CreativeModeTab, CreativeModeTab> CREATIVE_TAB = CREATIVE_TABS.register(
        "magicrpg", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.magicrpg"))
            .icon(SEAL::toStack)
            .displayItems((parameters, output) -> {
                ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                Goldenwood.addCreativeItems(output);
            })
            .build());

    public static final DeferredHolder<EntityType<?>, EntityType<AshHound>> ASH_HOUND = ENTITIES.register(
        "ash_hound", id -> EntityType.Builder.of(AshHound::new, MobCategory.MONSTER)
            .sized(0.95F, 1.25F)
            .eyeHeight(0.98F)
            .clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<StarHare>> STAR_HARE = ENTITIES.register(
        "star_hare", id -> EntityType.Builder.of(StarHare::new, MobCategory.CREATURE)
            .sized(0.55F, 0.9F).eyeHeight(0.48F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<ChalkGuardian>> CHALK_GUARDIAN = ENTITIES.register(
        "chalk_guardian", id -> EntityType.Builder.of(ChalkGuardian::new, MobCategory.MONSTER)
            .sized(0.75F, 1.1F).eyeHeight(0.91F).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    public static final DeferredHolder<EntityType<?>, EntityType<CitySentry>> CITY_STONE_GUARD =
        citySentry("city_stone_guard", 1.05F, 2.15F);
    public static final DeferredHolder<EntityType<?>, EntityType<CitySentry>> HOLLOW_ARMOR =
        citySentry("hollow_armor", 0.8F, 1.95F);
    public static final DeferredHolder<EntityType<?>, EntityType<CitySentry>> BURIED_WORKER =
        citySentry("buried_worker", 0.64F, 1.45F);
    public static final DeferredHolder<EntityType<?>, EntityType<MeadowPaladin>> MEADOW_PALADIN = ENTITIES.register(
        "meadow_paladin", id -> EntityType.Builder.of(MeadowPaladin::new, MobCategory.MONSTER)
            .sized(2.1F, 5.05F).eyeHeight(4.4F).clientTrackingRange(12).updateInterval(2)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );
    public static final DeferredHolder<EntityType<?>, EntityType<AncientMerchant>> ANCIENT_MERCHANT = ENTITIES.register(
        "ancient_merchant", id -> EntityType.Builder.of(AncientMerchant::new, MobCategory.CREATURE)
            .sized(0.6F, 1.95F).eyeHeight(1.62F).clientTrackingRange(10)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id))
    );

    private static SummoningSeal merchantSeal(Item.Properties properties) {
        return new SummoningSeal(properties, ANCIENT_MERCHANT, ParticleTypes.ENCHANT,
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL);
    }

    private static DeferredItem<Item> lilacArmor(String name, ArmorType type) {
        return ITEMS.registerSimpleItem(name, properties -> LilacArmor.properties(properties, type));
    }

    private static DeferredHolder<EntityType<?>, EntityType<CitySentry>> citySentry(
            String name, float width, float height) {
        return ENTITIES.register(name, id -> EntityType.Builder.of(CitySentry::new, MobCategory.MONSTER)
            .sized(width, height).clientTrackingRange(8)
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    public MagicRpg(IEventBus bus) {
        ReforgedSpawnBalance.register(bus);
        Goldenwood.register(bus);
        IronWastelands.register(bus);
        WayfarerMirrors.register(bus, BLOCKS, ITEMS);
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        CREATIVE_TABS.register(bus);
        bus.addListener(this::registerAttributes);
        bus.addListener(this::registerSpawnPlacements);
        bus.addListener(ReforgedIntegration::setup);
        bus.addListener(ScanInventorySlotPayload::register);
        NeoForge.EVENT_BUS.addListener(ReforgedIntegration::unlockFieldNotes);
        NeoForge.EVENT_BUS.addListener(ReforgedIntegration::showMatrixStability);
        NeoForge.EVENT_BUS.addListener(TimeTurner::onServerTick);
        NeoForge.EVENT_BUS.addListener(LilacArmor::regenerate);
        NeoForge.EVENT_BUS.addListener(IslandExport::register);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ASH_HOUND.get(), AshHound.attributes().build());
        event.put(STAR_HARE.get(), StarHare.attributes().build());
        event.put(MEADOW_PALADIN.get(), MeadowPaladin.attributes().build());
        event.put(ANCIENT_MERCHANT.get(), Villager.createAttributes().build());
        event.put(CITY_STONE_GUARD.get(), CitySentry.attributes(48, .22, 9, 10)
            .add(Attributes.KNOCKBACK_RESISTANCE, .65).build());
        event.put(HOLLOW_ARMOR.get(), CitySentry.attributes(34, .31, 7, 7).build());
        event.put(BURIED_WORKER.get(), CitySentry.attributes(18, .25, 4, 2).build());
        event.put(CHALK_GUARDIAN.get(), ChalkGuardian.attributes().build());
    }

    private void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(STAR_HARE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            StarHare::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CHALK_GUARDIAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            ChalkGuardian::canSpawn, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }
}
