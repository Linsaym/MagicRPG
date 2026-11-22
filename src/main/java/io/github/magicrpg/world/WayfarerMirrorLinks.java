package io.github.magicrpg.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.magicrpg.MagicRpg;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

final class WayfarerMirrorLinks extends SavedData {
    private static final SavedDataType<WayfarerMirrorLinks> TYPE = new SavedDataType<>(
        MagicRpg.id("wayfarer_mirrors"), () -> new WayfarerMirrorLinks(Map.of()),
        Codec.unboundedMap(UUIDUtil.STRING_CODEC, Endpoint.CODEC.listOf()).fieldOf("pairs")
            .xmap(WayfarerMirrorLinks::new, links -> links.pairs).codec());

    private final Map<UUID, List<Endpoint>> pairs;

    private WayfarerMirrorLinks(Map<UUID, List<Endpoint>> pairs) {
        this.pairs = new HashMap<>(pairs);
    }

    static WayfarerMirrorLinks get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    boolean canPlace(ServerLevel level, UUID pair, Endpoint endpoint) {
        var ends = pairs.getOrDefault(pair, List.of());
        if (ends.size() >= 2) {
            // A command or a mod can suppress block entity removal callbacks.
            var server = level.getServer();
            for (var end : ends) {
                var target = server.getLevel(end.dimension());
                if (target != null) {
                    target.getChunkAt(end.pos());
                    if (!(target.getBlockEntity(end.pos()) instanceof WayfarerMirrorEntity mirror)
                        || !pair.equals(mirror.pair())) {
                        remove(pair, end);
                    }
                }
            }
            ends = pairs.getOrDefault(pair, List.of());
        }
        return ends.contains(endpoint) || ends.size() < 2;
    }

    void add(UUID pair, Endpoint endpoint) {
        var ends = pairs.getOrDefault(pair, List.of());
        if (ends.isEmpty()) {
            pairs.put(pair, List.of(endpoint));
        } else if (ends.size() == 1 && !ends.contains(endpoint)) {
            pairs.put(pair, List.of(ends.getFirst(), endpoint));
        } else {
            return;
        }
        setDirty();
    }

    void remove(UUID pair, Endpoint endpoint) {
        var ends = pairs.get(pair);
        if (ends == null || !ends.contains(endpoint)) {
            return;
        }
        var remaining = ends.stream().filter(end -> !end.equals(endpoint)).toList();
        if (remaining.isEmpty()) {
            pairs.remove(pair);
        } else {
            pairs.put(pair, remaining);
        }
        setDirty();
    }

    @Nullable Endpoint other(UUID pair, Endpoint source) {
        var ends = pairs.get(pair);
        if (ends == null || ends.size() != 2 || !ends.contains(source)) {
            return null;
        }
        return ends.getFirst().equals(source) ? ends.getLast() : ends.getFirst();
    }

    record Endpoint(ResourceKey<Level> dimension, BlockPos pos) {
        private static final Codec<Endpoint> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(Endpoint::dimension),
            BlockPos.CODEC.fieldOf("pos").forGetter(Endpoint::pos)
        ).apply(instance, Endpoint::new));

        static Endpoint at(ServerLevel level, BlockPos pos) {
            return new Endpoint(level.dimension(), pos.immutable());
        }
    }
}
