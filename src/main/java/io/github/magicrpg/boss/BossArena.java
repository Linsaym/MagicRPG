package io.github.magicrpg.boss;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.magicrpg.MagicRpg;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

final class BossArena extends SavedData {
    private static final int MAX_PLACEMENTS = 256;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;
    private static final SavedDataType<BossArena> TYPE = new SavedDataType<>(
        MagicRpg.id("boss_arena"), () -> new BossArena(List.of()),
        Entry.CODEC.listOf().fieldOf("blocks").xmap(BossArena::new, arena -> new ArrayList<>(arena.entries.values())).codec());

    private final HashMap<BlockPos, Entry> entries = new HashMap<>();

    private BossArena(List<Entry> saved) {
        for (var entry : saved) {
            // An encounter is never resumed from disk. Recover on the first tick with this chunk loaded.
            entries.put(entry.pos(), new Entry(entry.pos(), entry.original(), entry.replacement()));
        }
    }

    static BossArena get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    Entry place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        if (ticks < 1) {
            throw new IllegalArgumentException("Arena block duration must be positive");
        }
        if (!(state.is(Blocks.BARRIER) || state.is(Blocks.REINFORCED_DEEPSLATE) || state.is(Blocks.LIGHT))
            || !state.getFluidState().isEmpty() || entries.size() >= MAX_PLACEMENTS || entries.containsKey(pos)) {
            return null;
        }
        var original = level.getBlockState(pos);
        if (!original.isAir() || !level.getEntities(null, new AABB(pos)).isEmpty()) {
            return null;
        }
        var entry = new Entry(pos.immutable(), original, state);
        entry.expires = level.getGameTime() + ticks;
        entries.put(entry.pos(), entry);
        if (!level.setBlock(pos, state, FLAGS)) {
            entries.remove(entry.pos());
            return null;
        }
        setDirty();
        return entry;
    }

    boolean contains(Entry entry) {
        return entries.get(entry.pos()) == entry;
    }

    void expire(ServerLevel level) {
        if (entries.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        for (var entry : List.copyOf(entries.values())) {
            if (now >= entry.expires) {
                release(level, entry);
            }
        }
    }

    void release(ServerLevel level, Entry entry) {
        if (!contains(entry)) {
            return;
        }
        entry.expires = Long.MIN_VALUE;
        if (!level.isLoaded(entry.pos())) {
            return;
        }
        if (level.getBlockState(entry.pos()).equals(entry.replacement())
            && !level.setBlock(entry.pos(), entry.original(), FLAGS)) {
            return;
        }
        entries.remove(entry.pos(), entry);
        setDirty();
    }

    static final class Entry {
        static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
            BlockState.CODEC.fieldOf("original").forGetter(Entry::original),
            BlockState.CODEC.fieldOf("replacement").forGetter(Entry::replacement)
        ).apply(instance, Entry::new));

        private final BlockPos pos;
        private final BlockState original;
        private final BlockState replacement;
        private long expires = Long.MIN_VALUE;

        private Entry(BlockPos pos, BlockState original, BlockState replacement) {
            this.pos = pos;
            this.original = original;
            this.replacement = replacement;
        }

        BlockPos pos() { return pos; }
        BlockState original() { return original; }
        BlockState replacement() { return replacement; }
    }
}
