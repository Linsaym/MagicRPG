package io.github.magicrpg.world;

import com.mojang.logging.LogUtils;
import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.world.AuthoredIsland.Layout;
import io.github.magicrpg.world.AuthoredIsland.Tile;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPOutputStream;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

/** Takes an authoring snapshot without loading or generating chunks in the world. */
public final class IslandExport {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int MARGIN = 2;
    private static final int MAX_EXTRA_CHUNKS = 4096;
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private IslandExport() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(MagicRpg.ID)
            .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("export")
                .executes(context -> export(context.getSource(), null))
                .then(Commands.argument("from", ColumnPosArgument.columnPos())
                    .then(Commands.argument("to", ColumnPosArgument.columnPos()).executes(context -> {
                        var from = ColumnPosArgument.getColumnPos(context, "from");
                        var to = ColumnPosArgument.getColumnPos(context, "to");
                        return export(context.getSource(), new Area(
                            Math.min(from.x(), to.x()) >> 4, Math.min(from.z(), to.z()) >> 4,
                            Math.max(from.x(), to.x()) >> 4, Math.max(from.z(), to.z()) >> 4));
                    })))));
    }

    private static int export(CommandSourceStack source, @Nullable Area extraArea) {
        var level = source.getLevel();
        if (!(level.getChunkSource().getGenerator() instanceof AuthoredIsland island) || !island.isMapFor(level)) {
            source.sendFailure(Component.translatable("commands.magicrpg.export.dimension"));
            return 0;
        }
        if (extraArea != null && !extraArea.isSupported()) {
            source.sendFailure(Component.translatable("commands.magicrpg.export.area", MAX_EXTRA_CHUNKS));
            return 0;
        }

        var id = level.dimension().identifier();
        var target = source.getServer().getServerDirectory().toAbsolutePath().normalize()
            .resolve("magicrpg-exports").resolve(id.getPath() + ".bin.gz");
        try {
            var layout = snapshot(level, island.layout(), extraArea);
            write(target, layout);
            source.sendSuccess(() -> Component.translatable("commands.magicrpg.export.success", target.toString()), false);
            return 1;
        } catch (IOException | RuntimeException ex) {
            LOGGER.error("Could not export island {}", id, ex);
            source.sendFailure(Component.translatable("commands.magicrpg.export.failure", target.toString()));
            return 0;
        }
    }

    private static Layout snapshot(ServerLevel level, Layout original, @Nullable Area extraArea) throws IOException {
        var positions = new HashSet<ChunkPos>();
        for (long key : original.tiles().keySet()) {
            ChunkPos.rangeClosed(ChunkPos.unpack(key), MARGIN).filter(IslandExport::fitsMap).forEach(positions::add);
        }
        if (extraArea != null) {
            extraArea.positions().forEach(positions::add);
        }
        Map<BlockState, Integer> palette = new HashMap<>();
        // Retain the original indices so untouched chunks can keep their encoded columns.
        for (int i = 0; i < original.palette().length; i++) {
            palette.putIfAbsent(original.palette()[i], i);
        }
        var states = new ArrayList<>(Arrays.asList(original.palette()));
        var tiles = new HashMap<>(original.tiles());
        var arrivalBlocks = new HashMap<BlockPos, BlockState>();
        for (var route : PortalRoute.values()) {
            if (route.dimension().equals(level.dimension())) {
                arrivalBlocks.putAll(PortalLinks.arrivalBlocksForExport(level, route));
            }
        }
        for (var pos : positions.stream().sorted(Comparator.comparingInt(ChunkPos::x)
            .thenComparingInt(ChunkPos::z)).toList()) {
            var saved = readChunk(level, pos);
            if (saved == null) {
                continue;
            }
            var tile = capture(saved, original, arrivalBlocks, states, palette);
            if (tile == null) {
                tiles.remove(pos.pack());
            } else {
                tiles.put(pos.pack(), tile);
            }
        }
        if (states.size() > 0xFFFF || tiles.size() > 0xFFFF) {
            throw new IOException("The island exceeds the map format limits");
        }
        return new Layout(states.toArray(BlockState[]::new), tiles, original.landmarks());
    }

    private static @Nullable SerializableChunkData readChunk(ServerLevel level, ChunkPos pos) throws IOException {
        var cache = level.getChunkSource();
        var holder = cache.chunkMap.getUpdatingChunkIfPresent(pos.pack());
        var full = holder == null ? null : holder.getChunkIfPresentUnchecked(ChunkStatus.FULL);
        if (full instanceof ImposterProtoChunk imposter) {
            full = imposter.getWrapped();
        }
        if (full instanceof LevelChunk chunk) {
            return SerializableChunkData.copyOf(level, chunk);
        }
        var visible = cache.getChunkNow(pos.x(), pos.z());
        if (visible != null) {
            return SerializableChunkData.copyOf(level, visible);
        }
        var saved = cache.chunkMap.read(pos).join().orElse(null);
        if (saved == null) {
            return null;
        }
        var context = ChunkMap.getChunkDataFixContextTag(level.dimension(), cache.getGenerator().getTypeNameForDataFixer());
        var tag = cache.chunkMap.upgradeChunkTag(saved, -1, context, SharedConstants.getCurrentVersion().dataVersion().version());
        if (SerializableChunkData.getChunkStatusFromTag(tag) != ChunkStatus.FULL) {
            return null;
        }
        var data = SerializableChunkData.parse(level, level.palettedContainerFactory(), tag);
        if (data == null || !pos.equals(data.chunkPos())) {
            throw new IOException("Invalid saved chunk " + pos);
        }
        return data;
    }

    private static @Nullable Tile capture(SerializableChunkData chunk, Layout original,
            Map<BlockPos, BlockState> arrivalBlocks, ArrayList<BlockState> states, Map<BlockState, Integer> palette) {
        var pos = chunk.chunkPos();
        var sections = new LevelChunkSection[AuthoredIsland.HEIGHT / 16];
        for (var section : chunk.sectionData()) {
            if (section.y() >= 0 && section.y() < sections.length) {
                sections[section.y()] = section.chunkSection();
            }
        }
        var restored = new HashMap<BlockPos, BlockState>();
        for (var entry : arrivalBlocks.entrySet()) {
            var block = entry.getKey();
            if (pos.contains(block) && stateAt(sections, block.getX() & 15, block.getY(), block.getZ() & 15) == entry.getValue()) {
                restored.put(block, original.stateAt(block));
            }
        }

        var columns = new int[AuthoredIsland.COLUMNS_PER_TILE][];
        var runs = new int[AuthoredIsland.HEIGHT * 3];
        var cursor = new BlockPos.MutableBlockPos();
        boolean hasBlocks = false;
        for (int column = 0; column < columns.length; column++) {
            int x = column & 15;
            int z = column >> 4;
            int count = 0;
            int start = 0;
            var current = AIR;
            for (int y = 0; y <= AuthoredIsland.HEIGHT; y++) {
                var state = y == AuthoredIsland.HEIGHT ? AIR : stateAt(sections, x, y, z);
                if (!restored.isEmpty()) {
                    state = restored.getOrDefault(cursor.set(pos.getBlockX(x), y, pos.getBlockZ(z)), state);
                }
                if (state.getBlock() instanceof LinkedPortalBlock || state.isAir()) {
                    state = AIR;
                }
                if (state == current) {
                    continue;
                }
                if (!current.isAir()) {
                    runs[count++] = start;
                    runs[count++] = y - start;
                    runs[count++] = palette.computeIfAbsent(current, block -> {
                        int index = states.size();
                        states.add(block);
                        return index;
                    });
                }
                current = state;
                start = y;
            }
            columns[column] = count == 0 ? Tile.EMPTY_COLUMN : Arrays.copyOf(runs, count);
            hasBlocks |= count != 0;
        }
        var previous = original.tiles().get(pos.pack());
        var fixtures = new ArrayList<AuthoredIsland.Fixture>();
        if (previous != null) {
            for (var fixture : previous.fixtures()) {
                var block = fixture.pos();
                if (original.stateAt(block).isAir()
                    && stateAt(sections, block.getX() & 15, block.getY(), block.getZ() & 15).isAir()) {
                    fixtures.add(fixture);
                }
            }
        }
        var blockEntities = new ArrayList<CompoundTag>();
        for (var tag : chunk.blockEntities()) {
            var block = BlockEntity.getPosFromTag(pos, tag);
            if (!restored.containsKey(block) && stateAt(sections, block.getX() & 15, block.getY(), block.getZ() & 15).hasBlockEntity()) {
                blockEntities.add(tag);
            }
        }
        return hasBlocks || !fixtures.isEmpty() ? new Tile(columns, fixtures, blockEntities) : null;
    }

    private static BlockState stateAt(LevelChunkSection[] sections, int x, int y, int z) {
        if (y < 0 || y >= AuthoredIsland.HEIGHT) {
            return AIR;
        }
        var section = sections[y >> 4];
        return section == null || section.hasOnlyAir() ? AIR : section.getBlockState(x, y & 15, z);
    }

    private static boolean fitsMap(ChunkPos pos) {
        return pos.x() == (short) pos.x() && pos.z() == (short) pos.z();
    }

    private static void write(Path target, Layout layout) throws IOException {
        Files.createDirectories(target.getParent());
        var temporary = Files.createTempFile(target.getParent(), "island-", ".tmp");
        try {
            try (var output = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(temporary)))) {
                layout.write(output);
            }
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private record Area(int minX, int minZ, int maxX, int maxZ) {
        boolean isSupported() {
            long count = (maxX - (long) minX + 1) * (maxZ - (long) minZ + 1);
            return count <= MAX_EXTRA_CHUNKS && fitsMap(new ChunkPos(minX, minZ)) && fitsMap(new ChunkPos(maxX, maxZ));
        }

        java.util.stream.Stream<ChunkPos> positions() {
            return ChunkPos.rangeClosed(new ChunkPos(minX, minZ), new ChunkPos(maxX, maxZ));
        }
    }
}
