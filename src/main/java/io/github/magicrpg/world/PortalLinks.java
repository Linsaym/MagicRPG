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
        var portals = server.overworld().getDataStorage().computeIfAbsent(
            route == PortalRoute.GOLDENWOOD ? GOLDENWOOD_LINKS : MEADOW_LINKS);
        ServerLevel target;
        BlockPos exit;
        if (source.dimension().equals(route.dimension())) {
            var link = portals.byDestination.get(frame);
            if (link == null) {
                return null;
            }
            var origin = link.origin();
            target = server.getLevel(origin.dimension());
            exit = target == null ? null : findExit(target, origin.frame(), player, route);
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
            var link = portals.byOrigin.get(origin);
            if (link == null || !link.destination().isOpen(target, route)) {
                var built = portals.buildArrival(target, frame.base(), route);
                if (built == null) {
                    return null;
                }
                link = new Link(origin, built);
                portals.index(link);
                portals.setDirty();
            }
            exit = findExit(target, link.destination(), player, route);
        }
        return exit == null ? null : new TeleportTransition(target, Vec3.atBottomCenterOf(exit), Vec3.ZERO,
            player.getYRot(), player.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND
                .then(TeleportTransition.PLACE_PORTAL_TICKET));
    }

    private @Nullable PortalFrame buildArrival(ServerLevel level, BlockPos origin, PortalRoute route) {
        var center = level.getWorldBorder().clampToBounds(origin);
        PortalFrame waterside = null;
        // Check spaced sites so two origins at the same coordinates never share a return arch.
        for (var site : BlockPos.spiralAround(center, 10, Direction.EAST, Direction.SOUTH)) {
            int x = center.getX() + (site.getX() - center.getX()) * 8;
            int z = center.getZ() + (site.getZ() - center.getZ()) * 8;
            var column = new BlockPos(x, 0, z);
            if (!level.getWorldBorder().isWithinBounds(column)) {
                continue;
            }
            var surface = surfaceAt(level, column);
            var frame = new PortalFrame(surface, Direction.Axis.X);
            if (byDestination.containsKey(frame)) {
                continue;
            }
            var floor = level.getBlockState(surface.below());
            if (route.isNaturalFloor(floor) && canBuild(level, surface, route, false)) {
                return placeArrival(level, frame, route);
            }
            if (route == PortalRoute.GOLDENWOOD && waterside == null && floor.is(Blocks.WATER)
                && canBuild(level, surface, route, true)) {
                waterside = frame;
            }
            if (waterside != null && (Math.abs(site.getX() - center.getX()) >= 3
                || Math.abs(site.getZ() - center.getZ()) >= 3)) {
                break;
            }
        }
        return waterside == null ? null : placeArrival(level, waterside, route);
    }

    private static PortalFrame placeArrival(ServerLevel level, PortalFrame frame, PortalRoute route) {
        buildPlatform(level, frame.base(), route);
        frame.build(level, route);
        return frame;
    }

    private static boolean canBuild(ServerLevel level, BlockPos base, PortalRoute route, boolean onWater) {
        for (var pos : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(2, 3, 1))) {
            if (!level.isInsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                return false;
            }
            var state = level.getBlockState(pos);
            boolean foundation = pos.getY() == base.getY() - 1;
            boolean clear = foundation
                ? route.isNaturalFloor(state) || (onWater && state.is(Blocks.WATER))
                : route.canClear(state);
            if (!state.isAir() && !clear) {
                return false;
            }
        }
        return true;
    }

    private static void buildPlatform(ServerLevel level, BlockPos base, PortalRoute route) {
        for (var pos : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(2, 3, 1))) {
            if (pos.getY() == base.getY() - 1) {
                level.setBlock(pos, route.platform().defaultBlockState(), Block.UPDATE_ALL);
            } else if (!level.isEmptyBlock(pos) && route.canClear(level.getBlockState(pos))) {
                level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static @Nullable BlockPos findExit(ServerLevel level, PortalFrame frame, ServerPlayer player, PortalRoute route) {
        if (frame.isOpen(level, route)) {
            for (var pos : frame.exits()) {
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
