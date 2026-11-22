package io.github.magicrpg.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.magicrpg.MagicRpg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

final class PortalLinks extends SavedData {
    private static final int[] EXIT_HEIGHTS = {0, 1, -1, 2, -2};
    private static final SavedDataType<PortalLinks> MEADOW_LINKS = type("meadow_portals");
    private static final SavedDataType<PortalLinks> GOLDENWOOD_LINKS = type("goldenwood_portals");

    private static SavedDataType<PortalLinks> type(String id) {
        return new SavedDataType<>(MagicRpg.id(id), () -> new PortalLinks(List.of()),
            Link.CODEC.listOf().fieldOf("links").xmap(PortalLinks::new, PortalLinks::links).codec());
    }

    private final Map<Origin, Link> byOrigin = new HashMap<>();
    private final Map<PortalFrame, Link> byDestination = new HashMap<>();

    private PortalLinks(List<Link> links) {
        links.forEach(this::index);
    }

    private List<Link> links() {
        return List.copyOf(byOrigin.values());
    }

    private void index(Link link) {
        var previous = byOrigin.put(link.origin(), link);
        if (previous != null) {
            byDestination.remove(previous.destination());
        }
        byDestination.put(link.destination(), link);
    }

    static @Nullable TeleportTransition destination(ServerLevel source, ServerPlayer player, PortalFrame frame,
                                                    PortalRoute route) {
        var server = source.getServer();
        var portals = forRoute(source, route);
        boolean arrivingOnIsland = !source.dimension().equals(route.dimension());
        ServerLevel target;
        BlockPos exit;
        if (source.dimension().equals(route.dimension())) {
            var link = portals.byDestination.get(frame);
            if (link == null) {
                return null;
            }
            var origin = link.origin();
            target = server.getLevel(origin.dimension());
            exit = target == null ? null : findExit(target, origin.frame(), player, route, false);
            if (exit == null) {
                target = server.findRespawnDimension();
                exit = findSurface(target, server.getRespawnData().pos(), player, 32);
            }
        } else {
            target = server.getLevel(route.dimension());
            if (target == null) {
                return null;
            }
            var origin = new Origin(source.dimension(), frame);
            var link = portals.prepareIslandLink(target, origin, route);
            if (link == null) {
                return null;
            }
            exit = findExit(target, link.destination(), player, route, arrivingOnIsland);
        }
        return exit == null ? null : new TeleportTransition(target, Vec3.atBottomCenterOf(exit), Vec3.ZERO,
            arrivingOnIsland ? 180.0F : player.getYRot(), arrivingOnIsland ? 0.0F : player.getXRot(),
            TeleportTransition.PLAY_PORTAL_SOUND
                .then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    private static PortalLinks forRoute(ServerLevel level, PortalRoute route) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            route == PortalRoute.GOLDENWOOD ? GOLDENWOOD_LINKS : MEADOW_LINKS);
    }

    static boolean prepareIslandArch(ServerLevel source, PortalFrame frame, ServerPlayer player, PortalRoute route) {
        var target = source.getServer().getLevel(route.dimension());
        if (target == null) {
            return false;
        }
        var portals = forRoute(source, route);
        var link = portals.prepareIslandLink(target, new Origin(source.dimension(), frame), route);
        return link != null && findExit(target, link.destination(), player, route, true) != null;
    }

    private @Nullable Link prepareIslandLink(ServerLevel level, Origin origin, PortalRoute route) {
        var previous = byOrigin.get(origin);
        if (previous != null) {
            var frame = previous.destination();
            if (frame.isOpen(level, route)) {
                return previous;
            }
            if (canBuild(level, frame, route, true)) {
                placeArrival(level, frame, route);
                return previous;
            }
        }
        // Keep the old return address until its replacement can be built.
        boolean meadows = route == PortalRoute.MEADOWS;
        int[] columns = meadows ? new int[]{0, -8, 8, -16, 16} : new int[]{0, -8, 8, -16, 16, -24, 24};
        int firstZ = meadows ? AuthoredIsland.MEADOW_FIRST_ARCH_Z : AuthoredIsland.FIRST_ARCH_Z;
        int y = meadows ? AuthoredIsland.MEADOW_ARRIVAL_Y : AuthoredIsland.ARRIVAL_Y;
        for (int row = 0; row < (meadows ? 2 : 5); row++) {
            int z = firstZ + row * 8;
            for (int x : columns) {
                var frame = new PortalFrame(new BlockPos(x, y, z), Direction.Axis.X);
                var occupant = byDestination.get(frame);
                if (!canBuild(level, frame, route, occupant != null)
                    || (occupant != null && !isAbandoned(level, occupant, route))) {
                    continue;
                }
                placeArrival(level, frame, route);
                if (occupant != null) {
                    byOrigin.remove(occupant.origin());
                    byDestination.remove(frame);
                }
                var link = new Link(origin, frame);
                index(link);
                setDirty();
                return link;
            }
        }
        return null;
    }

    private static boolean isAbandoned(ServerLevel island, Link link, PortalRoute route) {
        if (link.destination().isOpen(island, route)) {
            return false;
        }
        var origin = link.origin();
        var source = island.getServer().getLevel(origin.dimension());
        return source != null && !origin.frame().hasBricks(source, route);
    }

    private static PortalFrame placeArrival(ServerLevel level, PortalFrame frame, PortalRoute route) {
        buildPlatform(level, frame, route);
        frame.build(level, route);
        return frame;
    }

    private static Iterable<BlockPos> arrivalSpace(PortalFrame frame) {
        var base = frame.base();
        return frame.axis() == Direction.Axis.X
            ? BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(2, 3, 1))
            : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(1, 3, 2));
    }

    private static boolean canBuild(ServerLevel level, PortalFrame frame, PortalRoute route, boolean restore) {
        var base = frame.base();
        var border = restore ? frame.border() : List.<BlockPos>of();
        var interior = restore ? frame.interior() : List.<BlockPos>of();
        for (var pos : arrivalSpace(frame)) {
            if (!level.isInsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                return false;
            }
            var state = level.getBlockState(pos);
            boolean foundation = pos.getY() == base.getY() - 1;
            if (foundation) {
                if (route.isNaturalFloor(state)
                    || (route == PortalRoute.GOLDENWOOD && state.is(route.platform()))) {
                    continue;
                }
            } else if (state.isAir() || route.canClear(state)
                || (restore && state.is(route.portal()) && interior.contains(pos))) {
                continue;
            }
            if (restore && border.contains(pos)
                && (state.is(route.frameBlock()) || state.is(route.arrivalFrameBlock()))) {
                continue;
            }
            return false;
        }
        return true;
    }

    private static void buildPlatform(ServerLevel level, PortalFrame frame, PortalRoute route) {
        for (var pos : arrivalSpace(frame)) {
            if (pos.getY() == frame.base().getY() - 1) {
                level.setBlock(pos, route.platform().defaultBlockState(), Block.UPDATE_ALL);
            } else if (!level.isEmptyBlock(pos) && route.canClear(level.getBlockState(pos))) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static @Nullable BlockPos findExit(ServerLevel level, PortalFrame frame, ServerPlayer player,
                                               PortalRoute route, boolean preferNorth) {
        if (frame.isOpen(level, route)) {
            var exits = frame.exits();
            for (var pos : preferNorth ? exits.reversed() : exits) {
                if (isSafe(level, pos, player)) {
                    return pos;
                }
            }
        }
        for (var center : BlockPos.spiralAround(frame.base(), 6, Direction.EAST, Direction.SOUTH)) {
            for (int dy : EXIT_HEIGHTS) {
                var pos = center.above(dy);
                if (isSafe(level, pos, player)) {
                    return pos.immutable();
                }
            }
        }
        return findSurface(level, frame.base(), player, 16);
    }

    private static @Nullable BlockPos findSurface(ServerLevel level, BlockPos center, ServerPlayer player, int radius) {
        for (var column : BlockPos.spiralAround(center, radius, Direction.EAST, Direction.SOUTH)) {
            if (!level.getWorldBorder().isWithinBounds(column)) {
                continue;
            }
            var surface = surfaceAt(level, column);
            if (isSafe(level, surface, player)) {
                return surface;
            }
        }
        return null;
    }

    private static BlockPos surfaceAt(ServerLevel level, BlockPos column) {
        // The level height query returns minY for chunks that have not been loaded yet.
        var chunk = level.getChunkAt(column);
        int y = chunk.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column.getX() & 15, column.getZ() & 15) + 1;
        return column.atY(y);
    }

    static boolean isSafe(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var floor = pos.below();
        var floorState = level.getBlockState(floor);
        return level.isInsideBuildHeight(floor) && level.isInsideBuildHeight(pos.above())
            && level.getWorldBorder().isWithinBounds(pos)
            && floorState.isFaceSturdy(level, floor, Direction.UP)
            && !player.getType().isBlockDangerous(floorState) && !floorState.is(Blocks.MAGMA_BLOCK)
            && floorState.getFluidState().isEmpty()
            && isClear(level, pos, player) && isClear(level, pos.above(), player)
            && level.noCollision(player, player.getType().getSpawnAABB(Vec3.atBottomCenterOf(pos)));
    }

    private static boolean isClear(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var state = level.getBlockState(pos);
        return state.getFluidState().isEmpty() && !player.getType().isBlockDangerous(state)
            && !(state.getBlock() instanceof LinkedPortalBlock) && !state.is(Blocks.NETHER_PORTAL)
            && !state.is(Blocks.END_PORTAL) && !state.is(Blocks.END_GATEWAY);
    }

    private record Origin(ResourceKey<Level> dimension, PortalFrame frame) {
        private static final Codec<Origin> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Origin::dimension),
            PortalFrame.CODEC.fieldOf("frame").forGetter(Origin::frame)
        ).apply(instance, Origin::new));
    }

    private record Link(Origin origin, PortalFrame destination) {
        private static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Origin.CODEC.fieldOf("origin").forGetter(Link::origin),
            PortalFrame.CODEC.fieldOf("meadow").forGetter(Link::destination)
        ).apply(instance, Link::new));
    }
}
