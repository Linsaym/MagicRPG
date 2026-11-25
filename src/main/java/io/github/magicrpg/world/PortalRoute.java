package io.github.magicrpg.world;

import io.github.magicrpg.Goldenwood;
import io.github.magicrpg.IronWastelands;
import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.compat.reforged.ReforgedIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public enum PortalRoute {
    MEADOWS("lilac_meadows", "portal_frame", "meadow_no_space", new BlockPos(0, 113, 49)),
    GOLDENWOOD("goldenwood", "goldenwood_frame", "goldenwood_no_space", null),
    IRON_WASTELANDS("iron_wastelands", "iron_wastelands_frame", "iron_wastelands_no_space",
        new BlockPos(0, 105, 56));

    private static final String MESSAGE_PREFIX = "item.magicrpg.charred_fang.";
    private final ResourceKey<Level> dimension;
    private final String invalidFrameMessage;
    private final String landingFullMessage;
    private final @Nullable PortalFrame sharedArch;

    PortalRoute(String dimension, String invalidFrameMessage, String landingFullMessage, @Nullable BlockPos arrival) {
        this.dimension = ResourceKey.create(Registries.DIMENSION, MagicRpg.id(dimension));
        this.invalidFrameMessage = MESSAGE_PREFIX + invalidFrameMessage;
        this.landingFullMessage = MESSAGE_PREFIX + landingFullMessage;
        this.sharedArch = arrival == null ? null : new PortalFrame(arrival, Direction.Axis.X);
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public String invalidFrameMessage() {
        return invalidFrameMessage;
    }

    public String landingFullMessage() {
        return landingFullMessage;
    }

    @Nullable PortalFrame sharedArch() {
        return sharedArch;
    }

    public Block frameBlock() {
        return switch (this) {
            case MEADOWS -> ReforgedIntegration.portalBrick();
            case GOLDENWOOD -> ReforgedIntegration.amberBlock();
            case IRON_WASTELANDS -> Blocks.IRON_BLOCK;
        };
    }

    Block arrivalFrameBlock() {
        return this == GOLDENWOOD ? Goldenwood.RETURN_ARCH.get() : frameBlock();
    }

    public LinkedPortalBlock portal() {
        return switch (this) {
            case MEADOWS -> MagicRpg.MEADOW_PORTAL.get();
            case GOLDENWOOD -> Goldenwood.PORTAL.get();
            case IRON_WASTELANDS -> IronWastelands.PORTAL.get();
        };
    }

    Block platform() {
        return switch (this) {
            case MEADOWS -> MagicRpg.WHITE_STONE.get();
            case GOLDENWOOD -> Goldenwood.HONEY_BRICKS.get();
            case IRON_WASTELANDS -> IronWastelands.METAL_PLATES.get();
        };
    }

    boolean isNaturalFloor(BlockState state) {
        return switch (this) {
            case MEADOWS -> state.is(MagicRpg.LILAC_GRASS.get()) || state.is(MagicRpg.WHITE_STONE.get());
            case GOLDENWOOD -> state.is(Goldenwood.AMBER_GRASS.get()) || state.is(Goldenwood.HONEYSTONE.get());
            case IRON_WASTELANDS -> state.is(IronWastelands.ASHEN_SOIL.get()) || state.is(IronWastelands.METAL_PLATES.get());
        };
    }

    boolean canClear(BlockState state) {
        return this == GOLDENWOOD && state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    public static @Nullable PortalRoute forFrame(BlockState state) {
        for (var route : values()) {
            if (state.is(route.frameBlock())) {
                return route;
            }
        }
        return null;
    }
}
