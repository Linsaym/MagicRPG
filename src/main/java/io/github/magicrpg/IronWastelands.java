package io.github.magicrpg;

import io.github.magicrpg.entity.WastelandGiant;
import io.github.magicrpg.entity.WastelandIronGolem;
import io.github.magicrpg.entity.WastelandZombie;
import io.github.magicrpg.world.LinkedPortalBlock;
import io.github.magicrpg.world.PortalRoute;
import io.github.magicrpg.world.WastelandBattle;
import java.util.function.UnaryOperator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;

public final class IronWastelands {
    public static final DeferredBlock<Block> RUSTED_IRON = block("rusted_iron",
        properties -> properties.strength(4, 6).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> METAL_PLATES = block("metal_plates",
        properties -> properties.strength(3, 6).requiresCorrectToolForDrops().sound(SoundType.METAL));
    public static final DeferredBlock<Block> IRON_RUBBLE = block("iron_rubble",
        properties -> properties.strength(1.4F, 3).sound(SoundType.GRAVEL));
    public static final DeferredBlock<Block> ASHEN_SOIL = block("ashen_soil",
        properties -> properties.strength(.55F).sound(SoundType.SAND));
    public static final DeferredItem<BlockItem> RUSTED_IRON_ITEM = MagicRpg.ITEMS.registerSimpleBlockItem(RUSTED_IRON);
    public static final DeferredItem<BlockItem> METAL_PLATES_ITEM = MagicRpg.ITEMS.registerSimpleBlockItem(METAL_PLATES);
    public static final DeferredItem<BlockItem> IRON_RUBBLE_ITEM = MagicRpg.ITEMS.registerSimpleBlockItem(IRON_RUBBLE);
    public static final DeferredItem<BlockItem> ASHEN_SOIL_ITEM = MagicRpg.ITEMS.registerSimpleBlockItem(ASHEN_SOIL);
    public static final DeferredBlock<LinkedPortalBlock> PORTAL = MagicRpg.BLOCKS.registerBlock("iron_wastelands_portal",
        properties -> new LinkedPortalBlock(properties, PortalRoute.IRON_WASTELANDS),
        properties -> properties.noCollision().noOcclusion().instabreak().noLootTable().lightLevel(state -> 8));

    public static final DeferredHolder<EntityType<?>, EntityType<WastelandGiant>> IRON_COLOSSUS =
        mob("iron_colossus", WastelandGiant::new, 3.9F, 8F, 7.15F, 12);
    public static final DeferredHolder<EntityType<?>, EntityType<WastelandGiant>> FLESH_TITAN =
        mob("flesh_titan", WastelandGiant::new, 2.9F, 5F, 4.32F, 10);
    public static final DeferredHolder<EntityType<?>, EntityType<WastelandZombie>> HOLLOWED =
        mob("hollowed", WastelandZombie::new, .6F, 1.95F, 1.74F, 8);
    public static final DeferredHolder<EntityType<?>, EntityType<WastelandZombie>> ZOMBIE_THAUMATURGE =
        mob("zombie_thaumaturge", WastelandZombie::new, .6F, 1.95F, 1.74F, 8);
    public static final DeferredHolder<EntityType<?>, EntityType<WastelandIronGolem>> LARGE_IRON_GOLEM =
        mob("large_iron_golem", WastelandIronGolem::new, 1.4F, 2.7F, 2.295F, 10);

    private IronWastelands() {}

    public static boolean isConstruct(LivingEntity entity) {
        return entity instanceof WastelandIronGolem || entity.getType() == IRON_COLOSSUS.get();
    }

    public static boolean isInfected(LivingEntity entity) {
        return entity instanceof WastelandZombie || entity.getType() == FLESH_TITAN.get();
    }

    private static DeferredBlock<Block> block(String name, UnaryOperator<BlockBehaviour.Properties> properties) {
        return MagicRpg.BLOCKS.registerSimpleBlock(name, properties);
    }

    private static <T extends Mob> DeferredHolder<EntityType<?>, EntityType<T>> mob(
            String name, EntityType.EntityFactory<T> factory, float width, float height, float eyes, int trackingRange) {
        return MagicRpg.ENTITIES.register(name, id -> EntityType.Builder.of(factory, MobCategory.MONSTER)
            .sized(width, height).eyeHeight(eyes).clientTrackingRange(trackingRange).notInPeaceful()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, id)));
    }

    public static void register(IEventBus bus) {
        WastelandBattle.register();
        bus.addListener(IronWastelands::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(IRON_COLOSSUS.get(), WastelandGiant.attributes(170, .19, 16, 14, .9).build());
        event.put(FLESH_TITAN.get(), WastelandGiant.attributes(140, .26, 19, 7, .75).build());
        event.put(HOLLOWED.get(), Zombie.createAttributes().add(Attributes.FOLLOW_RANGE, 32).build());
        event.put(ZOMBIE_THAUMATURGE.get(), Zombie.createAttributes().add(Attributes.MOVEMENT_SPEED, .28).build());
        event.put(LARGE_IRON_GOLEM.get(), WastelandIronGolem.createAttributes()
            .add(Attributes.SCALE, 1.8).add(Attributes.FOLLOW_RANGE, 32).add(Attributes.STEP_HEIGHT, 1).build());
    }
}
