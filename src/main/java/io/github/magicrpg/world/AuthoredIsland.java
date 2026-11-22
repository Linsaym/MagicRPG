package io.github.magicrpg.world;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.entity.CitySentry;
import io.github.magicrpg.entity.MeadowPaladin;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jspecify.annotations.Nullable;

/** Copies the shipped block layout into new chunks. Minecraft owns the chunks from then on. */
public final class AuthoredIsland extends ChunkGenerator {
    static final int ARRIVAL_Y = 112;
    static final int FIRST_ARCH_Z = 118;
    private static final int HEIGHT = 192;
    private static final Set<Heightmap.Types> TERRAIN_HEIGHTMAPS =
        Set.of(Heightmap.Types.WORLD_SURFACE_WG, Heightmap.Types.OCEAN_FLOOR_WG);
    static final int MEADOW_ARRIVAL_Y = 113;
    static final int MEADOW_FIRST_ARCH_Z = 49;

    private enum MapData {
        GOLDENWOOD("goldenwood", 83),
        MEADOWS("lilac_meadows", 112);

        private final String name;
        private final int seaLevel;
        private final MapCodec<AuthoredIsland> codec;
        private Layout layout;

        MapData(String name, int seaLevel) {
            this.name = name;
            this.seaLevel = seaLevel;
            codec = Biome.CODEC.fieldOf("biome").xmap(biome -> new AuthoredIsland(this, biome), island -> island.biome);
        }

        synchronized Layout layout() {
            if (layout == null) {
                layout = Layout.read("/data/magicrpg/island/" + name + ".bin.gz");
            }
            return layout;
        }
    }

    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> GENERATORS =
        DeferredRegister.create(Registries.CHUNK_GENERATOR, MagicRpg.ID);

    private final Holder<Biome> biome;
    private final MapData map;

    public static void register(IEventBus bus) {
        GENERATORS.register("goldenwood_island", () -> MapData.GOLDENWOOD.codec);
        GENERATORS.register("lilac_meadows_island", () -> MapData.MEADOWS.codec);
        GENERATORS.register(bus);
    }

    private AuthoredIsland(MapData map, Holder<Biome> biome) {
        super(new FixedBiomeSource(biome));
        this.map = map;
        this.biome = biome;
    }

    @Override
    protected MapCodec<AuthoredIsland> codec() {
        return map.codec;
    }

    @Override
    public void validate() {
        map.layout();
    }

    @Override
    public ChunkGeneratorStructureState createState(HolderLookup<StructureSet> sets, RandomState random, long seed) {
        return ChunkGeneratorStructureState.createForFlat(random, seed, new ChunkPos(0, 0),
            biomeSource, Stream.empty());
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel level, ChunkAccess chunk, StructureManager structures) {}

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        if (map != MapData.MEADOWS) {
            return;
        }
        var tile = map.layout().tiles().get(region.getCenter().pack());
        if (tile == null) {
            return;
        }
        for (var fixture : tile.fixtures()) {
            if (!region.getBlockState(fixture.pos()).isAir()) {
                continue;
            }
            var type = BuiltInRegistries.ENTITY_TYPE.getValue(fixture.id());
            if (!(type.create(region.getLevel(), EntitySpawnReason.CHUNK_GENERATION) instanceof Mob mob)) {
                throw new IllegalStateException("Invalid island inhabitant " + fixture.id());
            }
            mob.snapTo(Vec3.atBottomCenterOf(fixture.pos()), 0, 0);
            mob.setPersistenceRequired();
            if (mob instanceof MeadowPaladin paladin) {
                paladin.guard(fixture.pos());
            } else if (mob instanceof CitySentry sentry) {
                sentry.station(fixture.pos());
            } else {
                mob.setHomeTo(fixture.pos(), 10);
            }
            region.addFreshEntity(mob);
        }
    }

    @Override
    public int getGenDepth() { return HEIGHT; }

    @Override
    public int getMinY() { return 0; }

    @Override
    public int getSeaLevel() { return map.seaLevel; }

    @Override
    public void addDebugScreenInfo(List<String> lines, RandomState state, BlockPos pos, SamplerContext context) {}

    @Override
    public CompletableFuture<ChunkAccess> buildTerrain(ChunkAccess chunk, Blender blender, RandomState random,
                                                       StructureManager structures, BiomeManager biomes,
                                                       WorldGenRegion region, Set<Holder<Biome>> biomeSet) {
        var layout = map.layout();
        var pos = chunk.getPos();
        var tile = layout.tiles().get(pos.pack());
        if (tile != null) {
            var cursor = new BlockPos.MutableBlockPos();
            for (int column = 0; column < tile.columns().length; column++) {
                int x = column & 15;
                int z = column >> 4;
                byte[] runs = tile.columns()[column];
                for (int i = 0; i < runs.length; i += 3) {
                    int start = Byte.toUnsignedInt(runs[i]);
                    int end = start + Byte.toUnsignedInt(runs[i + 1]);
                    var state = layout.palette()[Byte.toUnsignedInt(runs[i + 2])];
                    for (int y = start; y < end; y++) {
                        chunk.setBlockState(cursor.set(pos.getBlockX(x), y, pos.getBlockZ(z)), state);
                    }
                }
            }
            for (var fixture : tile.fixtures()) {
                fixture.install(chunk);
            }
        }
        Heightmap.primeHeightmaps(chunk, TERRAIN_HEIGHTMAPS);
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        var layout = map.layout();
        var runs = layout.column(x, z);
        for (int i = runs.length - 3; i >= 0; i -= 3) {
            if (type.isOpaque().test(layout.palette()[Byte.toUnsignedInt(runs[i + 2])])) {
                return Byte.toUnsignedInt(runs[i]) + Byte.toUnsignedInt(runs[i + 1]);
            }
        }
        return level.getMinY();
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        var layout = map.layout();
        var runs = layout.column(x, z);
        var states = new BlockState[HEIGHT];
        Arrays.fill(states, Blocks.AIR.defaultBlockState());
        for (int i = 0; i < runs.length; i += 3) {
            int start = Byte.toUnsignedInt(runs[i]);
            Arrays.fill(states, start, start + Byte.toUnsignedInt(runs[i + 1]),
                layout.palette()[Byte.toUnsignedInt(runs[i + 2])]);
        }
        return new NoiseColumn(0, states);
    }

    @Override
    public @Nullable Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel level,
            HolderSet<Structure> structures, BlockPos origin, int radius, boolean skipKnown) {
        Pair<BlockPos, Holder<Structure>> nearest = null;
        double maxDistance = radius * 16.0;
        double best = Double.POSITIVE_INFINITY;
        for (var structure : structures) {
            var id = structure.unwrapKey().orElse(null);
            var pos = id == null ? null : map.layout().landmarks().get(id.identifier());
            if (pos != null && Math.abs((double) pos.getX() - origin.getX()) <= maxDistance
                && Math.abs((double) pos.getZ() - origin.getZ()) <= maxDistance && pos.distSqr(origin) < best) {
                nearest = Pair.of(pos, structure);
                best = pos.distSqr(origin);
            }
        }
        return nearest != null ? nearest : super.findNearestMapStructure(level, structures, origin, radius, skipKnown);
    }

    private record Fixture(BlockPos pos, Identifier id) {
        private void install(ChunkAccess chunk) {
            var state = chunk.getBlockState(pos);
            if (state.is(Blocks.SPAWNER)) {
                var spawner = new SpawnerBlockEntity(pos, state);
                spawner.setEntityId(BuiltInRegistries.ENTITY_TYPE.getValue(id), RandomSource.create(pos.asLong()));
                chunk.setBlockEntity(spawner);
            } else if (state.getBlock() instanceof EntityBlock block
                && block.newBlockEntity(pos, state) instanceof RandomizableContainerBlockEntity container) {
                container.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, id));
                chunk.setBlockEntity(container);
            } else if (!state.isAir()) {
                throw new IllegalStateException("Invalid island fixture at " + pos);
            }
        }
    }

    private record Tile(byte[][] columns, Fixture[] fixtures) {}

    private record Layout(BlockState[] palette, Map<Long, Tile> tiles, Map<Identifier, BlockPos> landmarks) {
        private static final byte[] EMPTY_COLUMN = new byte[0];
        private static final int FORMAT = 0x47574931; // GWI1
        private byte[] column(int x, int z) {
            var tile = tiles.get(ChunkPos.pack(x >> 4, z >> 4));
            return tile == null ? EMPTY_COLUMN : tile.columns()[(z & 15) * 16 + (x & 15)];
        }

        private static Layout read(String resource) {
            try (var stream = AuthoredIsland.class.getResourceAsStream(resource)) {
                if (stream == null) {
                    throw new IOException("Missing " + resource);
                }
                try (var input = new DataInputStream(new GZIPInputStream(stream))) {
                    if (input.readInt() != FORMAT) {
                        throw new IOException("Unknown island format");
                    }
                    var palette = new BlockState[input.readUnsignedShort()];
                    for (int i = 0; i < palette.length; i++) {
                        palette[i] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, input.readUTF(), false).blockState();
                    }
                    Map<Identifier, BlockPos> landmarks = new HashMap<>();
                    int landmarkCount = input.readUnsignedByte();
                    for (int i = 0; i < landmarkCount; i++) {
                        var id = Identifier.parse(input.readUTF());
                        landmarks.put(id, new BlockPos(input.readShort(), input.readUnsignedByte(), input.readShort()));
                    }
                    Map<Long, Tile> tiles = new HashMap<>();
                    int tileCount = input.readUnsignedShort();
                    for (int i = 0; i < tileCount; i++) {
                        int x = input.readShort();
                        int z = input.readShort();
                        var columns = new byte[256][];
                        for (int column = 0; column < columns.length; column++) {
                            int size = input.readUnsignedByte() * 3;
                            columns[column] = size == 0 ? EMPTY_COLUMN : new byte[size];
                            input.readFully(columns[column]);
                        }
                        var fixtures = new Fixture[input.readUnsignedByte()];
                        for (int fixture = 0; fixture < fixtures.length; fixture++) {
                            int blockX = x * 16 + input.readUnsignedByte();
                            int y = input.readUnsignedByte();
                            int blockZ = z * 16 + input.readUnsignedByte();
                            fixtures[fixture] = new Fixture(new BlockPos(blockX, y, blockZ), Identifier.parse(input.readUTF()));
                        }
                        tiles.put(ChunkPos.pack(x, z), new Tile(columns, fixtures));
                    }
                    if (input.read() != -1) {
                        throw new IOException("Unexpected data after island map");
                    }
                    return new Layout(palette, Map.copyOf(tiles), Map.copyOf(landmarks));
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Could not load " + resource, ex);
            }
        }
    }
}
