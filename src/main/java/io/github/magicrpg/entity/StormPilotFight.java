package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.boss.BossAbility;
import io.github.magicrpg.boss.BossActions;
import io.github.magicrpg.boss.BossContext;
import io.github.magicrpg.boss.BossEncounter;
import io.github.magicrpg.boss.BossZone;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class StormPilotFight {
    public static final int LINE_CLIP = 1;
    public static final int LINE_LOCK = 6;
    public static final int LINE_CONTACT = 29;
    public static final int LINE_DURATION = 48;
    public static final int SURF_CLIP = 2;
    public static final int SURF_CONTACT = 24;
    public static final int SURF_END = 52;
    public static final int SURF_DURATION = 62;
    public static final int EYE_CLIP = 3;
    public static final int EYE_CONTACT = 42;
    public static final int EYE_DURATION = 74;

    private static final BossContext.Key<BossZone.Placed> LINE_ZONE = new BossContext.Key<>(BossZone.Placed.class);
    private static final BossZone.Line LANE = new BossZone.Line(StormPilot.ARENA_RADIUS + 1, 2);
    private static final BossZone.Ring OUTER_RING = new BossZone.Ring(4.5, StormPilot.ARENA_RADIUS);
    private static final BossZone.Circle SURF_SOURCE = new BossZone.Circle(2.5);
    private static final DustParticleOptions SPRAY = new DustParticleOptions(0x6FE8EE, 1.15F);
    private static final Identifier SHELL = MagicRpg.id("storm_pilot_shell");

    private static final BossAbility<StormPilot> LINE = lightningLane();
    private static final BossAbility<StormPilot> SURF = surf();
    private static final BossAbility<StormPilot> EYE = eyeOfTheStorm();
    static final BossEncounter<StormPilot> ENCOUNTER = new BossEncounter<>(context -> context.target().isPresent(),
        StormPilot::isParticipant, List.of(
            new BossEncounter.Phase<>(MagicRpg.id("storm_pilot_storm"),
                context -> context.boss().getHealth() <= context.boss().getMaxHealth() / 2, List.of(EYE, SURF, LINE)),
            new BossEncounter.Phase<>(MagicRpg.id("storm_pilot_tide"), List.of(SURF, LINE))));

    private StormPilotFight() {}

    private static BossAbility<StormPilot> lightningLane() {
        Function<BossContext<StormPilot>, BossZone.Placed> zone = c -> c.recall(LINE_ZONE).orElseThrow();
        return BossAbility.<StormPilot>builder(MagicRpg.id("storm_pilot_lightning"), LINE_DURATION)
            .priority(10).cooldown(69).animation(LINE_CLIP)
            .at(0, BossActions.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, .85F, .7F))
            .at(LINE_LOCK, c -> {
                var target = c.target().orElseGet(c.boss()::getTarget);
                if (target == null) {
                    c.interrupt();
                } else {
                    float yaw = c.boss().aimAt(target);
                    c.remember(LINE_ZONE, LANE.at(c.boss().position(), yaw, StormPilot.ARENA_HEIGHT));
                }
            })
            .during(LINE_LOCK, LINE_CONTACT - 1, 3, BossActions.warning(zone, ParticleTypes.ELECTRIC_SPARK, 48))
            .at(LINE_CONTACT, BossActions.warning(zone, ParticleTypes.END_ROD, 64))
            .at(LINE_CONTACT, BossActions.sound(SoundEvents.LIGHTNING_BOLT_IMPACT, 1, 1.25F))
            .at(LINE_CONTACT, BossActions.hit(zone, magicDamage(14, .45)))
            .build();
    }

    private static BossAbility<StormPilot> surf() {
        return BossAbility.<StormPilot>builder(MagicRpg.id("storm_pilot_surf"), SURF_DURATION)
            .priority(20).cooldown(155).animation(SURF_CLIP)
            .at(0, BossActions.sound(SoundEvents.CONDUIT_AMBIENT, 1.4F, .8F))
            .during(0, SURF_CONTACT - 1, 4, BossActions.warning(
                c -> SURF_SOURCE.at(c.boss().position(), 0, 1), SPRAY, 24))
            .at(SURF_CONTACT, BossActions.sound(SoundEvents.GENERIC_SPLASH, 1.7F, .65F))
            .during(SURF_CONTACT, SURF_END, 2, BossActions.wave(SURF_CONTACT, SURF_END, StormPilot.ARENA_RADIUS, 1.15,
                .68, c -> c.boss().position(), magicDamage(10, .65), SPRAY))
            .build();
    }

    private static BossAbility<StormPilot> eyeOfTheStorm() {
        Function<BossContext<StormPilot>, BossZone.Placed> zone =
            c -> OUTER_RING.at(c.boss().position(), 0, StormPilot.ARENA_HEIGHT);
        return BossAbility.<StormPilot>builder(MagicRpg.id("storm_pilot_eye"), EYE_DURATION)
            .priority(30).cooldown(180).animation(EYE_CLIP)
            .at(0, c -> c.damageMultiplier(SHELL, .25F, EYE_CONTACT, BossContext.Scope.CAST))
            .at(0, BossActions.sound(SoundEvents.CONDUIT_ACTIVATE, 1.2F, .65F))
            .during(0, EYE_CONTACT - 1, 4, BossActions.warning(zone, ParticleTypes.SOUL_FIRE_FLAME, 80))
            .during(0, EYE_CONTACT - 1, 6, BossActions.particles(c -> c.boss().position().add(0, 2, 0),
                ParticleTypes.ELECTRIC_SPARK, 12, new Vec3(.85, .7, .7), .07))
            .at(EYE_CONTACT, BossActions.warning(zone, ParticleTypes.ELECTRIC_SPARK, 96))
            .at(EYE_CONTACT, BossActions.sound(SoundEvents.GUARDIAN_ATTACK, 1.5F, .75F))
            .at(EYE_CONTACT, BossActions.hit(zone, magicDamage(18, .35)))
            .at(EYE_CONTACT, c -> c.damageMultiplier(SHELL, 1.5F, EYE_DURATION - EYE_CONTACT, BossContext.Scope.CAST))
            .build();
    }

    private static BossActions.Damage<StormPilot> magicDamage(float amount, double knockback) {
        return new BossActions.Damage<>(boss -> boss.damageSources().indirectMagic(boss, boss), amount, knockback, false);
    }
}
