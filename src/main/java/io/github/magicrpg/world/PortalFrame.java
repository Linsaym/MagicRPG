package io.github.magicrpg.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;

record PortalFrame(BlockPos base, Direction.Axis axis) {
    static final Codec<PortalFrame> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        BlockPos.CODEC.fieldOf("base").forGetter(PortalFrame::base),
        Direction.Axis.CODEC.fieldOf("axis").forGetter(PortalFrame::axis)
    ).apply(instance, PortalFrame::new));
    static final List<Direction.Axis> AXES = List.of(Direction.Axis.X, Direction.Axis.Z);
    static final List<BlockPos> BORDER = offsets(true);
    private static final List<BlockPos> INTERIOR = offsets(false);

    static Direction along(Direction.Axis axis) {
        return Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
    }

    private static List<BlockPos> offsets(boolean border) {
        List<BlockPos> positions = new ArrayList<>();
        for (int y = -1; y <= 3; y++) {
            for (int x = -1; x <= 2; x++) {
                if ((x == -1 || x == 2 || y == -1 || y == 3) == border) {
                    positions.add(new BlockPos(x, y, 0));
                }
            }
        }
        return List.copyOf(positions);
    }

    static PortalFrame fromPortal(ServerLevel level, BlockPos pos, Direction.Axis axis, PortalRoute route) {
        var previous = along(axis).getOpposite();
        BlockPos base = pos;
        for (int i = 0; i < 2 && isPortal(level, base.below(), axis, route); i++) {
            base = base.below();
        }
        if (isPortal(level, base.relative(previous), axis, route)) {
            base = base.relative(previous);
        }
        return new PortalFrame(base, axis);
    }

    private static boolean isPortal(ServerLevel level, BlockPos pos, Direction.Axis axis, PortalRoute route) {
        var state = level.getBlockState(pos);
        return state.is(route.portal()) && state.getValue(LinkedPortalBlock.AXIS) == axis;
    }

    private List<BlockPos> positions(List<BlockPos> offsets) {
        return offsets.stream().map(offset -> base.relative(along(axis), offset.getX()).above(offset.getY())).toList();
    }

    List<BlockPos> interior() {
        return positions(INTERIOR);
    }

    List<BlockPos> border() {
        return positions(BORDER);
    }

    boolean hasBricks(ServerLevel level, PortalRoute route) {
        return hasBricks(level, route.frameBlock());
    }

    private boolean hasBricks(ServerLevel level, Block brick) {
        return border().stream().allMatch(pos -> level.getBlockState(pos).is(brick));
    }

    boolean isOpen(ServerLevel level, PortalRoute route) {
        boolean complete = hasBricks(level, route)
            || (route == PortalRoute.GOLDENWOOD && level.dimension().equals(route.dimension())
                && hasBricks(level, route.arrivalFrameBlock()));
        return complete && interior().stream().allMatch(pos -> isPortal(level, pos, axis, route));
    }

    void build(ServerLevel level, PortalRoute route) {
        var brick = route.arrivalFrameBlock().defaultBlockState();
        for (var pos : border()) {
            level.setBlockAndUpdate(pos, brick);
        }
        open(level, route);
    }

    void open(ServerLevel level, PortalRoute route) {
        var portal = route.portal().defaultBlockState().setValue(LinkedPortalBlock.AXIS, axis);
        for (var pos : interior()) {
            level.setBlockAndUpdate(pos, portal);
        }
        level.scheduleTick(base, route.portal(), 20);
    }

    List<BlockPos> exits() {
        var side = along(axis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
        var other = base.relative(along(axis));
        return List.of(base.relative(side), other.relative(side),
            base.relative(side.getOpposite()), other.relative(side.getOpposite()));
    }
}
