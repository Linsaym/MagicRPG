package io.github.magicrpg;

import io.github.magicrpg.entity.StormPilot;
import io.github.magicrpg.item.Reefheart;
import io.github.magicrpg.item.StormBell;
import io.github.magicrpg.world.LinkedPortalBlock;
import io.github.magicrpg.world.PortalRoute;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class StormReefs {
    public static final BlockPos LODESTONE = new BlockPos(0, 104, -33);
    public static final BlockPos PILOT_PERCH = new BlockPos(0, 105, -41);

    public static final DeferredBlock<LinkedPortalBlock> PORTAL = MagicRpg.BLOCKS.registerBlock("storm_reefs_portal",
        properties -> new LinkedPortalBlock(properties, PortalRoute.STORM_REEFS),
        properties -> properties.noCollision().noOcclusion().instabreak().noLootTable().lightLevel(state -> 11));
    public static final DeferredItem<StormBell> BELL = MagicRpg.ITEMS.registerItem("storm_bell", StormBell::new);
    public static final DeferredItem<Reefheart> REEFHEART = MagicRpg.ITEMS.registerItem("reefheart", Reefheart::new,
        properties -> properties.stacksTo(1).rarity(Rarity.RARE));

    public static final DeferredHolder<EntityType<?>, EntityType<StormPilot>> STORM_PILOT = MagicRpg.ENTITIES.register(
        "storm_pilot", id -> EntityType.Builder.of(StormPilot::new, MobCategory.MONSTER)
            .sized(2.35F, 3.25F).eyeHeight(2.18F).clientTrackingRange(8).updateInterval(2).notInPeaceful()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));

    private StormReefs() {}

    public static void register(IEventBus bus) {
        bus.addListener(StormReefs::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(STORM_PILOT.get(), StormPilot.attributes().build());
    }
}
