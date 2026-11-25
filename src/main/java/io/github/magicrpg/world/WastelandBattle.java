package io.github.magicrpg.world;

import io.github.magicrpg.IronWastelands;
import io.github.magicrpg.entity.WastelandZombie;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

public final class WastelandBattle {
    private static final BlockPos FOUNDRY = new BlockPos(5, 105, 0);
    private static final int HOME_RADIUS = 36;
    private static final int RESUPPLY_INTERVAL = 600;
    private static final float CONSTRUCT_DAMAGE_AGAINST_ZOMBIES = 25;
    private static final AABB BATTLEFIELD = new AABB(-34, 87, -39, 45, 145, 40);

    private WastelandBattle() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(WastelandBattle::resupply);
        NeoForge.EVENT_BUS.addListener(WastelandBattle::preventFriendlyTargeting);
        NeoForge.EVENT_BUS.addListener(WastelandBattle::adjustConstructDamage);
    }

    private static void preventFriendlyTargeting(LivingChangeTargetEvent event) {
        var target = event.getNewAboutToBeSetTarget();
        if (target != null && areFriendlyConstructs(event.getEntity(), target)) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    private static void adjustConstructDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof LivingEntity attacker)) {
            return;
        }
        var target = event.getEntity();
        if (areFriendlyConstructs(attacker, target)) {
            event.setCanceled(true);
        } else if (target instanceof WastelandZombie && IronWastelands.isConstruct(attacker)
            && event.getSource().getDirectEntity() == attacker && event.getAmount() > 0) {
            event.setAmount(Math.max(event.getAmount(), CONSTRUCT_DAMAGE_AGAINST_ZOMBIES));
        }
    }

    private static boolean areFriendlyConstructs(LivingEntity first, LivingEntity second) {
        return (IronWastelands.isConstruct(first) && (IronWastelands.isConstruct(second) || second instanceof IronGolem))
            || (first instanceof IronGolem && IronWastelands.isConstruct(second));
    }

    private static void resupply(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)
            || !level.dimension().equals(PortalRoute.IRON_WASTELANDS.dimension())
            || !(level.getChunkSource().getGenerator() instanceof AuthoredIsland island)
            || level.getDifficulty() == Difficulty.PEACEFUL || !level.isSpawningMonsters()
            || level.getGameTime() % RESUPPLY_INTERVAL != 0 || !level.isPositionEntityTicking(FOUNDRY)) {
            return;
        }
        var starts = island.layout().tiles().values().stream().flatMap(tile -> tile.fixtures().stream())
            .filter(fixture -> inBattlefield(fixture.pos())).toList();
        for (var type : List.of(IronWastelands.IRON_COLOSSUS.get(), IronWastelands.FLESH_TITAN.get(),
                IronWastelands.HOLLOWED.get(), IronWastelands.ZOMBIE_THAUMATURGE.get(), IronWastelands.LARGE_IRON_GOLEM.get())) {
            replenish(level, starts, type);
        }
    }

    private static void replenish(ServerLevel level, List<AuthoredIsland.Fixture> starts, EntityType<? extends Mob> type) {
        var id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        var positions = starts.stream().filter(start -> start.id().equals(id)).map(AuthoredIsland.Fixture::pos).toList();
        int remaining = positions.size() - level.getEntities(type, BATTLEFIELD, Mob::isAlive).size();
        for (var pos : positions) {
            if (remaining <= 0) {
                break;
            }
            var center = Vec3.atBottomCenterOf(pos);
            var floor = pos.below();
            if (!level.isPositionEntityTicking(pos)
                || !level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                continue;
            }
            var mob = type.create(level, EntitySpawnReason.REINFORCEMENT);
            if (mob != null) {
                mob.snapTo(center, 0, 0);
                var bounds = mob.getBoundingBox();
                if (!level.noCollision(bounds) || !level.getEntities(null, bounds).isEmpty()) {
                    continue;
                }
                prepare(mob, level, EntitySpawnReason.REINFORCEMENT);
                if (level.addFreshEntity(mob)) {
                    remaining--;
                }
            }
        }
    }

    static void prepare(Mob mob, ServerLevelAccessor level, EntitySpawnReason reason) {
        var pos = mob.blockPosition();
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), reason, null);
        mob.setPersistenceRequired();
        if (inBattlefield(pos)) {
            mob.setHomeTo(FOUNDRY, HOME_RADIUS);
        } else {
            mob.setHomeTo(pos, 16);
        }
    }

    private static boolean inBattlefield(BlockPos pos) {
        return BATTLEFIELD.contains(Vec3.atCenterOf(pos));
    }
}
