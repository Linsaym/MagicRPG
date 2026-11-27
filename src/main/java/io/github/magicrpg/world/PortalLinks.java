package io.github.magicrpg.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.magicrpg.MagicRpg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final SavedDataType<PortalLinks> WASTELANDS_LINKS = type("iron_wastelands_portals");
    private static final SavedDataType<PortalLinks> REEF_LINKS = type("storm_reefs_portals");

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
        var sharedArch = route.sharedArch();
        boolean arrivingOnIsland = !source.dimension().equals(route.dimension());
        ServerLevel target;
        BlockPos exit;
        var after = TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET);
        if (source.dimension().equals(route.dimension())) {
            var link = portals.byDestination.get(frame);
            boolean shared = frame.equals(sharedArch);
            var origin = shared ? SharedReturns.get(source, route).origins.get(player.getUUID()) : null;
            if (origin == null && link != null) {
                origin = link.origin();
            }
            if (origin == null && !shared) {
                return null;
            }
            target = origin == null ? null : server.getLevel(origin.dimension());
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
            if (sharedArch != null) {
                exit = prepareSharedArch(target, player, route, sharedArch);
                if (exit == null) {
                    player.sendOverlayMessage(Component.translatable(route.landingFullMessage()));
                }
                after = after.then(entity -> SharedReturns.get(source, route).record(entity.getUUID(), origin));
            } else {
                var link = portals.prepareIslandLink(target, origin, route);
                exit = link == null ? null : findExit(target, link.destination(), player, route, true);
            }
        }
        return exit == null ? null : new TeleportTransition(target, Vec3.atBottomCenterOf(exit), Vec3.ZERO,
            arrivingOnIsland ? 180.0F : player.getYRot(), arrivingOnIsland ? 0.0F : player.getXRot(), after);
    }

    private static PortalLinks forRoute(ServerLevel level, PortalRoute route) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(typeForRoute(route));
    }

    private static SavedDataType<PortalLinks> typeForRoute(PortalRoute route) {
        return switch (route) {
            case GOLDENWOOD -> GOLDENWOOD_LINKS;
            case MEADOWS -> MEADOW_LINKS;
            case IRON_WASTELANDS -> WASTELANDS_LINKS;
            case STORM_REEFS -> REEF_LINKS;
        };
    }

    static Map<BlockPos, BlockState> arrivalBlocksForExport(ServerLevel level, PortalRoute route) {
        var links = level.getServer().overworld().getDataStorage().get(typeForRoute(route));
        if (links == null) {
            return Map.of();
        }
        Map<BlockPos, BlockState> blocks = new HashMap<>();
        for (var frame : links.byDestination.keySet()) {
            if (frame.equals(route.sharedArch())) {
                continue;
            }
            for (var pos : arrivalSpace(frame)) {
                if (pos.getY() == frame.base().getY() - 1) {
                    blocks.put(pos.immutable(), route.platform().defaultBlockState());
                }
            }
            for (var pos : frame.border()) {
                blocks.put(pos, route.arrivalFrameBlock().defaultBlockState());
            }
        }
        return blocks;
    }

    static boolean prepareIslandArch(ServerLevel source, ServerLevel target, PortalFrame frame, ServerPlayer player,
                                   PortalRoute route) {
        var sharedArch = route.sharedArch();
        if (sharedArch != null) {
            return prepareSharedArch(target, player, route, sharedArch) != null;
        }
        var portals = forRoute(source, route);
        var link = portals.prepareIslandLink(target, new Origin(source.dimension(), frame), route);
        return link != null && findExit(target, link.destination(), player, route, true) != null;
    }

    private static @Nullable BlockPos prepareSharedArch(ServerLevel level, ServerPlayer player,
                                                         PortalRoute route, PortalFrame frame) {
        boolean open = frame.isOpen(level, route);
        if (!open) {
            for (var pos : frame.border()) {
                var state = level.getBlockState(pos);
                if (!level.getWorldBorder().isWithinBounds(pos) || !(state.is(route.frameBlock()) || state.isAir()
                    || (pos.getY() < frame.base().getY() && route.isNaturalFloor(state)))) {
                    return null;
                }
            }
            for (var pos : frame.interior()) {
                var state = level.getBlockState(pos);
                if (!level.getWorldBorder().isWithinBounds(pos) || !(state.isAir() || state.is(route.portal()))) {
                    return null;
                }
            }
        }
        for (var pos : frame.exits().reversed()) {
            if (isSafe(level, pos, player)) {
                if (!open) {
                    frame.build(level, route);
                }
                return pos;
            }
        }
        return null;
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
        int[] columns = {0, -8, 8, -16, 16, -24, 24};
        for (int row = 0; row < 5; row++) {
            int z = AuthoredIsland.FIRST_ARCH_Z + row * 8;
            for (int x : columns) {
                var frame = new PortalFrame(new BlockPos(x, AuthoredIsland.ARRIVAL_Y, z), Direction.Axis.X);
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

    private static final class SharedReturns extends SavedData {
        private static final SavedDataType<SharedReturns> MEADOWS = type("meadow_returns");
        private static final SavedDataType<SharedReturns> WASTELANDS = type("iron_wastelands_returns");
        private static final SavedDataType<SharedReturns> REEFS = type("storm_reefs_returns");

        private static SavedDataType<SharedReturns> type(String id) {
            return new SavedDataType<>(MagicRpg.id(id), () -> new SharedReturns(Map.of()),
                Codec.unboundedMap(UUIDUtil.STRING_CODEC, Origin.CODEC)
                    .fieldOf("origins").xmap(SharedReturns::new, data -> data.origins).codec());
        }

        private final Map<UUID, Origin> origins;

        private SharedReturns(Map<UUID, Origin> origins) {
            this.origins = new HashMap<>(origins);
        }

        private static SharedReturns get(ServerLevel level, PortalRoute route) {
            var type = switch (route) {
                case MEADOWS -> MEADOWS;
                case IRON_WASTELANDS -> WASTELANDS;
                case STORM_REEFS -> REEFS;
                case GOLDENWOOD -> throw new IllegalArgumentException("Goldenwood links individual arches");
            };
            return level.getServer().overworld().getDataStorage().computeIfAbsent(type);
        }

        private void record(UUID player, Origin origin) {
            if (!origin.equals(origins.put(player, origin))) {
                setDirty();
            }
        }
    }

    private record Link(Origin origin, PortalFrame destination) {
        private static final Codec<Link> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Origin.CODEC.fieldOf("origin").forGetter(Link::origin),
            PortalFrame.CODEC.fieldOf("meadow").forGetter(Link::destination)
        ).apply(instance, Link::new));
    }
}
