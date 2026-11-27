package io.github.magicrpg.boss;

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
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@EventBusSubscriber(modid = MagicRpg.ID)
public final class BossRespawns extends SavedData {
    private static final int BLOCK_FLAGS = Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS
        | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS;
    private static final SavedDataType<BossRespawns> TYPE = new SavedDataType<>(
        MagicRpg.id("boss_respawns"), () -> new BossRespawns(List.of()),
        Entry.CODEC.listOf().fieldOf("entries").xmap(BossRespawns::new,
            respawns -> List.copyOf(respawns.pending.values())).codec());

    private final Map<UUID, Entry> pending = new HashMap<>();

    private BossRespawns(List<Entry> saved) {
        for (var entry : saved) {
            pending.put(entry.spawnId(), entry);
        }
    }

    public static boolean isPending(ServerLevel level, EntityType<? extends BossMob> type, BlockPos pos) {
        return get(level).pending.values().stream().anyMatch(entry -> entry.type() == type
            && BlockPos.containing(entry.point().position()).equals(pos));
    }

    static void schedule(ServerLevel level, BossMob boss, Point point, int delay) {
        if (delay <= 0) {
            return;
        }
        var destination = level.getServer().getLevel(point.dimension());
        if (destination != null) {
            var respawns = get(destination);
            // Reserve the new entity's UUID so a saved queue can recognize a completed respawn.
            var entry = new Entry(UUID.randomUUID(), boss.getType(), point, destination.getGameTime() + delay);
            respawns.pending.put(entry.spawnId(), entry);
            respawns.setDirty();
        }
    }

    @SubscribeEvent
    static void rememberPoint(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof BossMob boss) {
            boss.rememberRespawnPoint(level, event.loadedFromDisk());
        }
    }

    @SubscribeEvent
    static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && level.getDifficulty() != Difficulty.PEACEFUL) {
            get(level).respawnDueBosses(level);
        }
    }

    private static BossRespawns get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    private void respawnDueBosses(ServerLevel level) {
        if (pending.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        var due = pending.values().stream().filter(entry -> entry.dueAt() <= now).toList();
        for (var entry : due) {
            if (entry.respawn(level) && pending.remove(entry.spawnId(), entry)) {
                setDirty();
            }
        }
    }

    private static boolean spawn(ServerLevel level, BossMob boss, BlockPos feet) {
        var bounds = boss.getBoundingBox();
        var min = BlockPos.containing(bounds.minX, bounds.minY, bounds.minZ);
        var max = new BlockPos(Mth.ceil(bounds.maxX) - 1, Mth.ceil(bounds.maxY) - 1, Mth.ceil(bounds.maxZ) - 1);
        int floorY = feet.getY() - 1;
        var floorMin = new BlockPos(Math.min(min.getX(), feet.getX() - 1), floorY, Math.min(min.getZ(), feet.getZ() - 1));
        var floorMax = new BlockPos(Math.max(max.getX(), feet.getX() + 1), floorY, Math.max(max.getZ(), feet.getZ() + 1));
        var border = level.getWorldBorder();
        if (!level.isInWorldBounds(floorMin) || !level.isInWorldBounds(floorMax)
            || !border.isWithinBounds(floorMin) || !border.isWithinBounds(floorMax)
            || !level.hasChunksAt(floorMin.getX() - 1, floorMin.getZ() - 1, floorMax.getX() + 1, floorMax.getZ() + 1)) {
            return false;
        }

        EventHooks.finalizeMobSpawn(boss, level, level.getCurrentDifficultyAt(feet), EntitySpawnReason.EVENT, null);
        if (boss.isSpawnCancelled() || !level.addFreshEntity(boss)) {
            return false;
        }

        for (var pos : BlockPos.betweenClosed(min, max)) {
            if (!level.isOutsideBuildHeight(pos) && !level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_FLAGS);
            }
        }
        // A fence or moving block may extend into the spawn space from an adjacent cell.
        var collisions = new BlockCollisions<>(level, boss, bounds, false, (pos, shape) -> pos.immutable());
        while (collisions.hasNext()) {
            var pos = collisions.next();
            if (border.isWithinBounds(pos)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), BLOCK_FLAGS);
            }
        }
        for (var pos : BlockPos.betweenClosed(floorMin, floorMax)) {
            var state = level.getBlockState(pos);
            if ((!state.isFaceSturdy(level, pos, Direction.UP) || !state.getFluidState().isEmpty())
                && !level.setBlock(pos, Blocks.STONE.defaultBlockState(), BLOCK_FLAGS)) {
                boss.discard();
                return false;
            }
        }
        if (!level.noBlockCollision(boss, bounds) || level.containsAnyLiquid(bounds)) {
            boss.discard();
            return false;
        }
        return true;
    }

    record Point(ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch) {
        static final Codec<Point> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Point::dimension),
            Vec3.CODEC.fieldOf("position").forGetter(Point::position),
            Codec.FLOAT.fieldOf("yaw").forGetter(Point::yaw),
            Codec.FLOAT.fieldOf("pitch").forGetter(Point::pitch)
        ).apply(instance, Point::new));
    }

    private record Entry(UUID spawnId, EntityType<?> type, Point point, long dueAt) {
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("spawn_id").forGetter(Entry::spawnId),
            EntityType.CODEC.fieldOf("type").forGetter(Entry::type),
            Point.CODEC.fieldOf("point").forGetter(Entry::point),
            Codec.LONG.fieldOf("due_at").forGetter(Entry::dueAt)
        ).apply(instance, Entry::new));

        private boolean respawn(ServerLevel level) {
            var feet = BlockPos.containing(point.position());
            if (!level.isPositionEntityTicking(feet) || !level.areEntitiesLoaded(ChunkPos.pack(feet))) {
                return false;
            }
            var existing = level.getEntity(spawnId);
            if (existing != null) {
                return existing.isAlive();
            }
            if (!(type.create(level, EntitySpawnReason.EVENT) instanceof BossMob boss)) {
                return false;
            }
            boss.setUUID(spawnId);
            boss.snapTo(point.position(), point.yaw(), point.pitch());
            boss.setPersistenceRequired();
            return spawn(level, boss, feet);
        }
    }
}
