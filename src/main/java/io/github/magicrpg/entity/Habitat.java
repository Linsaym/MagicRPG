package io.github.magicrpg.entity;

import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;

final class Habitat {
    private Habitat() {}

    static Optional<BlockPos> find(BlockPos center, int horizontal, int vertical, Predicate<BlockPos> matches) {
        for (var pos : BlockPos.withinBoxByManhattanDistance(center, horizontal, vertical, horizontal)) {
            if (matches.test(pos)) {
                return Optional.of(pos.immutable());
            }
        }
        return Optional.empty();
    }
}
