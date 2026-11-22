package io.github.magicrpg;

import io.github.magicrpg.world.WayfarerMirrorBlock;
import io.github.magicrpg.world.WayfarerMirrorEntity;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WayfarerMirrors {
    private static final DeferredRegister<DataComponentType<?>> COMPONENTS =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MagicRpg.ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MagicRpg.ID);
    private static final Map<DyeColor, DeferredBlock<WayfarerMirrorBlock>> BLOCKS_BY_COLOR =
        new EnumMap<>(DyeColor.class);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> PAIR =
        COMPONENTS.register("mirror_pair", () -> DataComponentType.<UUID>builder()
            .persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC).build());

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WayfarerMirrorEntity>> BLOCK_ENTITY =
        BLOCK_ENTITIES.register("wayfarer_mirror", () -> new BlockEntityType<>(WayfarerMirrorEntity::new,
            BLOCKS_BY_COLOR.values().stream().map(DeferredBlock::get).toArray(Block[]::new)));

    private WayfarerMirrors() {}

    public static String name(DyeColor color) {
        return color.getSerializedName() + "_wayfarer_mirror";
    }

    public static Block block(DyeColor color) {
        return BLOCKS_BY_COLOR.get(color).get();
    }

    static void register(IEventBus bus, DeferredRegister.Blocks blocks, DeferredRegister.Items items) {
        for (var color : DyeColor.VALUES) {
            var mirror = blocks.registerBlock(name(color), WayfarerMirrorBlock::new,
                properties -> properties.strength(0.4F).sound(SoundType.GLASS).noCollision().noOcclusion()
                    .noLootTable().pushReaction(PushReaction.IMMOVEABLE));
            items.registerSimpleBlockItem(mirror, properties -> properties.stacksTo(2));
            BLOCKS_BY_COLOR.put(color, mirror);
        }
        COMPONENTS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }
}
