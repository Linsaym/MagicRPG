package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.boss.BossAbility;
import io.github.magicrpg.boss.BossAction;
import io.github.magicrpg.boss.BossActions;
import io.github.magicrpg.boss.BossContext;
import io.github.magicrpg.boss.BossEncounter;
import io.github.magicrpg.boss.BossZone;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class AmberArcherFight {
    public static final int BIND_CLIP = 1, RAIN_CLIP = 2, FAN_CLIP = 3;
    public static final int FIRST_SHOT = 14, SHOT_INTERVAL = 13, LAST_SHOT = FIRST_SHOT + 2 * SHOT_INTERVAL;
    public static final int RELEASE = 46, BIND_END = 74;
    public static final int RAIN_LAUNCH = 12, RAIN_FALL = 36, RAIN_END = 54;
    public static final int FAN_SHOT = 14, FAN_END = 36;
    private static final double RELEASE_HEIGHT = AmberArcher.HEIGHT * .64;
    private static final double FAN_RELEASE_HEIGHT = AmberArcher.HEIGHT * .58;

    static final Identifier FAN_ID = MagicRpg.id("amber_archer_fan");
    private static final DustParticleOptions ROOT = new DustParticleOptions(0xe88016, 1.25F);
    private static final DustParticleOptions GOLD = new DustParticleOptions(0xffcf49, 1.1F);
    private static final BossZone.Circle RAIN_RING = new BossZone.Circle(1.05);
    private static final List<Vec3> MARK_OFFSETS = List.of(Vec3.ZERO,
        new Vec3(3.4, 0, 0), new Vec3(-3.4, 0, 0), new Vec3(0, 0, 3.4), new Vec3(0, 0, -3.4));
    private static final List<Vec3> SHAFT_OFFSETS = List.of(Vec3.ZERO,
        new Vec3(.53, 0, 0), new Vec3(-.53, 0, 0), new Vec3(0, 0, .53), new Vec3(0, 0, -.53),
        new Vec3(.38, 0, .38), new Vec3(-.38, 0, .38), new Vec3(.38, 0, -.38), new Vec3(-.38, 0, -.38));
    private static final BossContext.Key<RainMarks> RAIN_MARKS = new BossContext.Key<>(RainMarks.class);
    private static final BossContext.Key<Arrow> SKY_ARROW = new BossContext.Key<>(Arrow.class);
    private static final BossContext.Key<Vec3> FAN_DIRECTION = new BossContext.Key<>(Vec3.class);
    private static final List<BossAction<AmberArcher>> ROOTS = rootParticles();

    private static final BossAbility<AmberArcher> BIND = BossAbility.<AmberArcher>builder(
            MagicRpg.id("amber_archer_bind"), BIND_END)
        .priority(30).cooldown(180).animation(BIND_CLIP)
        .requires(c -> c.target().filter(target -> c.boss().hasLineOfSight(target)).isPresent())
        .during(0, RELEASE - 1, 1, BossActions.pinPlayer())
        .at(0, BossActions.sound(SoundEvents.ROOTED_DIRT_BREAK, 1.65F, .65F))
        .during(0, RELEASE - 4, 4, c -> ROOTS.forEach(root -> root.run(c)))
        .at(0, BossActions.particles(c -> target(c).position().add(0, .23, 0),
            ParticleTypes.WAX_ON, 14, new Vec3(.32, .19, .32), .015))
        .during(FIRST_SHOT, LAST_SHOT, SHOT_INTERVAL, BossAction.all(
            BossActions.sound(SoundEvents.ARROW_SHOOT, 1.5F, .68F), AmberArcherFight::shootBoundTarget))
        .at(RELEASE, BossActions.sound(SoundEvents.AMETHYST_BLOCK_BREAK, .9F, .85F))
        .build();

    private static final BossAbility<AmberArcher> FAN = BossAbility.<AmberArcher>builder(FAN_ID, FAN_END)
        .priority(20).cooldown(88).animation(FAN_CLIP)
        .requires(c -> c.boss().onGround() && c.target().filter(target ->
            c.boss().distanceToSqr(target) <= 3.2 * 3.2 && c.boss().hasLineOfSight(target)).isPresent())
        .at(0, BossAction.all(AmberArcherFight::aimFan, BossActions.sound(SoundEvents.BAMBOO_WOOD_HIT, 1.3F, .65F)))
        .at(FAN_SHOT, BossAction.all(AmberArcherFight::shootFan,
            BossActions.sound(SoundEvents.CROSSBOW_SHOOT, 1.3F, 1.35F)))
        .build();

    private static final BossAbility<AmberArcher> RAIN = BossAbility.<AmberArcher>builder(
            MagicRpg.id("amber_archer_rain"), RAIN_END)
        .priority(10).cooldown(105).animation(RAIN_CLIP)
        .at(0, BossActions.sound(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 1.3F))
        .at(RAIN_LAUNCH, BossAction.all(AmberArcherFight::launchRain,
            BossActions.sound(SoundEvents.ARROW_SHOOT, 1.1F, 1.45F)))
        .during(RAIN_LAUNCH, RAIN_FALL + 2, 4, AmberArcherFight::warnRain)
        .at(RAIN_FALL, AmberArcherFight::dropRain)
        .build();

    static final BossEncounter<AmberArcher> ENCOUNTER = new BossEncounter<>(
        c -> c.target().isPresent(),
        (boss, victim) -> victim instanceof ServerPlayer player && boss.canFight(player),
        List.of(new BossEncounter.Phase<>(MagicRpg.id("amber_archer_courtyard"), List.of(BIND, FAN, RAIN))));

    private AmberArcherFight() {}

    private static LivingEntity target(BossContext<AmberArcher> context) {
        return context.target().orElseThrow();
    }

    private static List<BossAction<AmberArcher>> rootParticles() {
        var roots = new ArrayList<BossAction<AmberArcher>>();
        for (int root = 0; root < 5; ++root) {
            for (int point = 0; point < 6; ++point) {
                double t = point / 5.0;
                double angle = root * Mth.TWO_PI / 5 + t * .8;
                double radius = .88 - .55 * t;
                var offset = new Vec3(Math.cos(angle) * radius, .07 + .49 * t * t, Math.sin(angle) * radius);
                roots.add(BossActions.particles(c -> target(c).position().add(offset), ROOT, 1, Vec3.ZERO, 0));
            }
        }
        return List.copyOf(roots);
    }

    private static void shootBoundTarget(BossContext<AmberArcher> context) {
        Vec3 from = releaseOrigin(context.boss(), RELEASE_HEIGHT);
        LivingEntity victim = target(context);
        Vec3 aim = victim.position().add(0, victim.getBbHeight() * .58, 0).subtract(from);
        float speed = 3.6F;
        double travelTicks = aim.length() / speed;
        shoot(context, from, aim.add(0, .025 * travelTicks * travelTicks, 0), speed, 3.3);
    }

    private static void aimFan(BossContext<AmberArcher> context) {
        var boss = context.boss();
        var victim = target(context);
        Vec3 bearing = Vec3.directionFromRotation(0, boss.getYRot());
        var from = releaseOrigin(boss, FAN_RELEASE_HEIGHT);
        double rise = victim.getY(.65) - from.y;
        double run = Math.max(.35, victim.position().subtract(from).horizontalDistance());
        context.remember(FAN_DIRECTION, bearing.add(0, rise / run, 0));
    }

    private static void shootFan(BossContext<AmberArcher> context) {
        var boss = context.boss();
        Vec3 aim = context.recall(FAN_DIRECTION).orElseThrow();
        Vec3 back = aim.multiply(-1, 0, -1);
        Vec3 landing = boss.position().add(back.scale(4));
        BlockPos floor = BlockPos.containing(landing.add(0, -.3, 0));
        if (boss.inCourtyard(landing) && context.level().isLoaded(floor)
            && context.level().getBlockState(floor).isFaceSturdy(context.level(), floor, Direction.UP)) {
            BossActions.<AmberArcher>leap(c -> back, .59, .31).run(context);
        }
        Vec3 from = releaseOrigin(boss, FAN_RELEASE_HEIGHT);
        for (int degrees = -26; degrees <= 26; degrees += 13) {
            shoot(context, from, aim.yRot(degrees * Mth.DEG_TO_RAD), 2.2F, 1.55);
        }
    }

    private static void launchRain(BossContext<AmberArcher> context) {
        var boss = context.boss();
        Vec3 from = releaseOrigin(boss, boss.getEyeHeight() - .12);
        context.remember(SKY_ARROW, shoot(context, from, new Vec3(0, 1, 0), 1.35F, 0));
        var centers = new ArrayList<Vec3>();
        for (Vec3 offset : MARK_OFFSETS) {
            Vec3 candidate = target(context).position().add(offset);
            Vec3 top = candidate.add(0, 2.5, 0);
            if (!boss.inCourtyard(candidate) || !context.level().isLoaded(BlockPos.containing(top))) {
                continue;
            }
            var floor = context.level().clip(new ClipContext(top, candidate.add(0, -2.5, 0),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, boss));
            Vec3 center = floor.getLocation();
            if (floor.getType() == HitResult.Type.BLOCK && boss.inCourtyard(center)
                && clearSky(context, center)) {
                centers.add(center);
            }
        }
        context.remember(RAIN_MARKS, new RainMarks(List.copyOf(centers)));
    }

    private static boolean clearSky(BossContext<AmberArcher> context, Vec3 center) {
        // Only warn at a spot where arrows can descend to the marked floor.
        for (Vec3 offset : SHAFT_OFFSETS) {
            Vec3 foot = center.add(offset).add(0, .12, 0);
            Vec3 top = foot.add(0, 4.8, 0);
            if (!context.level().isLoaded(BlockPos.containing(top)) || !context.level().isLoaded(BlockPos.containing(foot))
                || context.level().clip(new ClipContext(top, foot, ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE, context.boss())).getType() != HitResult.Type.MISS) {
                return false;
            }
        }
        return true;
    }

    private static void warnRain(BossContext<AmberArcher> context) {
        for (Vec3 center : context.recall(RAIN_MARKS).orElseThrow().centers()) {
            BossActions.<AmberArcher>warning(c -> RAIN_RING.at(center, 0, 1), GOLD, 16).run(context);
        }
    }

    private static void dropRain(BossContext<AmberArcher> context) {
        context.recall(SKY_ARROW).ifPresent(Arrow::discard);
        for (Vec3 center : context.recall(RAIN_MARKS).orElseThrow().centers()) {
            if (!clearSky(context, center)) {
                continue;
            }
            for (Vec3 offset : SHAFT_OFFSETS) {
                shoot(context, center.add(offset).add(0, 4.8, 0), new Vec3(0, -1, 0), 1.55F, 2.8);
            }
        }
    }

    private static Vec3 releaseOrigin(AmberArcher boss, double height) {
        Vec3 forward = Vec3.directionFromRotation(0, boss.getYRot());
        return boss.position().add(forward.scale(.34)).add(forward.z * .16, height, -forward.x * .16);
    }

    private static Arrow shoot(BossContext<AmberArcher> context, Vec3 from, Vec3 direction, float speed, double damage) {
        var arrow = new Arrow(context.level(), from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.setOwner(context.boss());
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setBaseDamage(damage);
        arrow.shoot(direction.x, direction.y, direction.z, speed, 0);
        context.level().addFreshEntity(arrow);
        return arrow;
    }

    private record RainMarks(List<Vec3> centers) {}
}
