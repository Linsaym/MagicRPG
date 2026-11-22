package io.github.magicrpg;

import io.github.magicrpg.compat.reforged.ReforgedIntegration;
import io.github.magicrpg.entity.AshHound;
import io.github.magicrpg.entity.ChalkGuardian;
import io.github.magicrpg.entity.StarHare;
import io.github.magicrpg.item.CharredFang;
import io.github.magicrpg.item.SummoningSeal;
import io.github.magicrpg.network.ScanInventorySlotPayload;
import io.github.magicrpg.world.LinkedPortalBlock;
import io.github.magicrpg.world.PortalRoute;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MagicRpg.ID)
public final class MagicRpg {
    public static final String ID = "magicrpg";

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    private static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);

    public static final DeferredItem<SummoningSeal> SEAL = ITEMS.registerItem("ash_hound_seal", SummoningSeal::new);
    public static final DeferredItem<CharredFang> FANG = ITEMS.registerItem("charred_fang", CharredFang::new);
    public static final DeferredBlock<Block> WHITE_STONE = BLOCKS.registerSimpleBlock("white_stone",
        properties -> properties.strength(1.5F, 6).requiresCorrectToolForDrops().sound(SoundType.CALCITE));
    public static final DeferredBlock<Block> LILAC_GRASS = BLOCKS.registerSimpleBlock("lilac_grass",
        properties -> properties.strength(0.6F).sound(SoundType.GRASS));
    public static final DeferredBlock<LinkedPortalBlock> MEADOW_PORTAL = BLOCKS.registerBlock("meadow_portal",
        properties -> new LinkedPortalBlock(properties, PortalRoute.MEADOWS), properties -> properties.noCollision().noOcclusion().instabreak()
            .noLootTable().lightLevel(state -> 10));
    public static final DeferredItem<BlockItem> WHITE_STONE_ITEM = ITEMS.registerSimpleBlockItem(WHITE_STONE);
    public static final DeferredItem<BlockItem> LILAC_GRASS_ITEM = ITEMS.registerSimpleBlockItem(LILAC_GRASS);

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

    public MagicRpg(IEventBus bus) {
        Goldenwood.register(bus);
        WayfarerMirrors.register(bus, BLOCKS, ITEMS);
        ITEMS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        CREATIVE_TABS.register(bus);
        bus.addListener(this::registerAttributes);
        bus.addListener(this::registerSpawnPlacements);
        bus.addListener(ReforgedIntegration::setup);
        bus.addListener(ScanInventorySlotPayload::register);
        NeoForge.EVENT_BUS.addListener(ReforgedIntegration::unlockFieldNotes);
    }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ASH_HOUND.get(), AshHound.attributes().build());
        event.put(STAR_HARE.get(), StarHare.attributes().build());
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
