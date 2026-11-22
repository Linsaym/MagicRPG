package io.github.magicrpg.world;

import io.github.magicrpg.Goldenwood;
import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.compat.reforged.ReforgedIntegration;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public enum PortalRoute {
    MEADOWS("lilac_meadows", "item.magicrpg.charred_fang.portal_frame"),
    GOLDENWOOD("goldenwood", "item.magicrpg.charred_fang.goldenwood_frame");

    private final ResourceKey<Level> dimension;
    private final String invalidFrameMessage;

    PortalRoute(String dimension, String invalidFrameMessage) {
        this.dimension = ResourceKey.create(Registries.DIMENSION, MagicRpg.id(dimension));
        this.invalidFrameMessage = invalidFrameMessage;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public String invalidFrameMessage() {
        return invalidFrameMessage;
    }

    public Block frameBlock() {
        return this == GOLDENWOOD ? ReforgedIntegration.amberBlock() : ReforgedIntegration.portalBrick();
    }

    public LinkedPortalBlock portal() {
        return this == GOLDENWOOD ? Goldenwood.PORTAL.get() : MagicRpg.MEADOW_PORTAL.get();
    }

    Block platform() {
        return this == GOLDENWOOD ? Goldenwood.HONEY_BRICKS.get() : MagicRpg.WHITE_STONE.get();
    }

    boolean isNaturalFloor(BlockState state) {
        return this == GOLDENWOOD
            ? state.is(Goldenwood.AMBER_GRASS.get()) || state.is(Goldenwood.HONEYSTONE.get())
            : state.is(MagicRpg.LILAC_GRASS.get()) || state.is(MagicRpg.WHITE_STONE.get());
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
