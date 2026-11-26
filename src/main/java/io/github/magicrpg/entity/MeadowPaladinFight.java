package io.github.magicrpg.entity;

import io.github.magicrpg.MagicRpg;
import io.github.magicrpg.boss.BossAbility;
import io.github.magicrpg.boss.BossAction;
import io.github.magicrpg.boss.BossActions;
import io.github.magicrpg.boss.BossContext;
import io.github.magicrpg.boss.BossEncounter;
import io.github.magicrpg.boss.BossZone;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

public final class MeadowPaladinFight {
    public static final int SWEEP = 1, SWEEP_CONTACT = 21, SWEEP_END = 39;
    public static final int HEW = 2, HEW_CONTACT = 28, HEW_END = 55;
    public static final int RESONANCE = 3, RESONANCE_CONTACT = 33, RESONANCE_END = 67;

    private static final double SWEEP_REACH = 4.15;
    private static final double HEW_REACH = 3.65;
    private static final double WAVE_REACH = 7;
    private static final int WAVE_END = RESONANCE_CONTACT + 14;
    private static final Identifier OPENING = MagicRpg.id("paladin_opening");
    private static final Identifier CRACKED = MagicRpg.id("paladin_cracked");
    private static final BossContext.Key<BossZone.Placed> STRIKE = new BossContext.Key<>(BossZone.Placed.class);
    private static final Function<BossContext<MeadowPaladin>, BossZone.Placed> ZONE =
        cast -> cast.recall(STRIKE).orElseThrow();
    private static final DustParticleOptions GOLD_DUST = new DustParticleOptions(0xFFCD76, 0.9F);
    private static final DustParticleOptions LILAC_DUST = new DustParticleOptions(0xE4A6FF, 1.15F);
    private static final BossAbility<MeadowPaladin> SWEEP_ATTACK = sweep();
    private static final BossAbility<MeadowPaladin> HEW_ATTACK = hew();

    static final BossEncounter<MeadowPaladin> ENCOUNTER = new BossEncounter<>(
        context -> context.target().isPresent(),
        (boss, entity) -> entity instanceof ServerPlayer player && boss.inGuardZone(player, false),
        List.of(
            new BossEncounter.Phase<>(CRACKED, context ->
                context.boss().getHealth() <= context.boss().getMaxHealth() / 2
                    && (context.phase().filter(CRACKED::equals).isPresent()
                        || context.boss().bossFight().activeAbility().isEmpty()),
                List.of(resonance(), HEW_ATTACK, SWEEP_ATTACK)),
            new BossEncounter.Phase<>(MagicRpg.id("paladin_guard"), List.of(HEW_ATTACK, SWEEP_ATTACK))
        ));

    private MeadowPaladinFight() {}

    private static BossAbility<MeadowPaladin> sweep() {
        return attack("paladin_sweep", SWEEP, SWEEP_END, SWEEP_REACH, new BossZone.Cone(SWEEP_REACH, 96), 0)
            .cooldown(47).priority(10)
            .at(0, BossActions.sound(SoundEvents.IRON_GOLEM_STEP, 1.2F, .72F))
            .at(14, BossActions.sound(SoundEvents.IRON_GOLEM_REPAIR, .9F, .83F))
            .during(3, SWEEP_CONTACT - 1, 4, BossActions.warning(ZONE, GOLD_DUST, 32))
            .at(SWEEP_CONTACT, BossAction.all(
                BossActions.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1.45F, .69F),
                BossActions.particles(cast -> offset(ZONE.apply(cast), 2.9).add(0, 2.28, 0),
                    ParticleTypes.ELECTRIC_SPARK, 13, new Vec3(.19, .10, .19), .17),
                BossActions.warning(ZONE, GOLD_DUST, 32),
                BossActions.hit(ZONE, steel(14, .85))))
            .at(SWEEP_CONTACT + 4, exposure(SWEEP_END - 6))
            .build();
    }

    private static BossAbility<MeadowPaladin> hew() {
        // Start the strip inside the keeper's collision box so it covers point-blank targets.
        return attack("paladin_hew", HEW, HEW_END, HEW_REACH, new BossZone.Line(HEW_REACH + .7, 1.10), -.7)
            .cooldown(71).priority(20)
            .requires(cast -> cast.lastCompleted().filter(SWEEP_ATTACK.id()::equals).isPresent()
                || cast.target().orElseThrow().distanceToSqr(cast.boss()) >= 2.6 * 2.6
                || cast.lastCompleted().isEmpty())
            .at(0, BossActions.sound(SoundEvents.IRON_GOLEM_REPAIR, 1.1F, .64F))
            .at(HEW_CONTACT - 7, BossActions.sound(SoundEvents.IRON_GOLEM_STEP, 1.4F, .67F))
            .during(4, HEW_CONTACT - 1, 4, BossActions.warning(ZONE, GOLD_DUST, 34))
            .at(HEW_CONTACT, BossAction.all(
                BossActions.sound(SoundEvents.DEEPSLATE_BREAK, 1.55F, .61F),
                BossActions.sound(SoundEvents.ANVIL_LAND, .5F, .85F),
                floorBurst(cast -> offset(ZONE.apply(cast), 3.6), 34),
                BossActions.warning(ZONE, ParticleTypes.ELECTRIC_SPARK, 36),
                BossActions.hit(ZONE, steel(19, .45))))
            .at(HEW_CONTACT + 3, exposure(HEW_END - 5))
            .at(HEW_END - 9, BossActions.sound(SoundEvents.IRON_GOLEM_REPAIR, .75F, .72F))
            .build();
    }

    private static BossAbility<MeadowPaladin> resonance() {
        Function<BossContext<MeadowPaladin>, Vec3> center = cast -> ZONE.apply(cast).origin();
        return attack("paladin_resonance", RESONANCE, RESONANCE_END, 5.75, new BossZone.Circle(WAVE_REACH), .95)
            .cooldown(155).priority(30)
            .at(0, BossActions.sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6F, .63F))
            .at(19, BossActions.sound(SoundEvents.WARDEN_SONIC_CHARGE, .85F, 1.25F))
            .during(3, RESONANCE_CONTACT - 1, 4, BossActions.warning(ZONE, LILAC_DUST, 48))
            .during(10, RESONANCE_CONTACT - 2, 4,
                BossActions.particles(cast -> cast.boss().position().add(0, 3.35, 0), LILAC_DUST, 10,
                    new Vec3(.45, .28, .45), .02))
            .at(RESONANCE_CONTACT, BossAction.all(
                BossActions.sound(SoundEvents.WARDEN_SONIC_BOOM, 1.3F, .88F),
                BossActions.sound(SoundEvents.DEEPSLATE_BREAK, 1.2F, .69F),
                floorBurst(center, 26)))
            .during(RESONANCE_CONTACT, WAVE_END, 1, BossActions.wave(RESONANCE_CONTACT, WAVE_END,
                WAVE_REACH, .85, 2, center,
                new BossActions.Damage<>(boss -> boss.damageSources().indirectMagic(boss, boss), 15, .8, true),
                LILAC_DUST))
            .at(WAVE_END + 1, exposure(RESONANCE_END - 3))
            .at(RESONANCE_END - 7, BossActions.sound(SoundEvents.AMETHYST_BLOCK_CHIME, .95F, .65F))
            .build();
    }

    private static BossAbility.Builder<MeadowPaladin> attack(String name, int clip, int end, double range,
                                                             BossZone shape, double forwardOffset) {
        return BossAbility.<MeadowPaladin>builder(MagicRpg.id(name), end).animation(clip)
            .requires(cast -> cast.boss().onGround() && cast.target().filter(target ->
                cast.boss().distanceToSqr(target) <= range * range && cast.boss().hasLineOfSight(target)).isPresent())
            .at(0, cast -> {
                var boss = cast.boss();
                var target = cast.target().orElseThrow();
                float yaw = boss.yawTo(target);
                Vec3 origin = boss.position().add(Vec3.directionFromRotation(0, yaw).scale(forwardOffset));
                cast.remember(STRIKE, shape.at(origin, yaw, 2));
                boss.brace();
            })
            .during(0, end, 1, cast -> cast.boss().face(ZONE.apply(cast).yaw()));
    }

    private static Vec3 offset(BossZone.Placed zone, double distance) {
        return zone.origin().add(Vec3.directionFromRotation(0, zone.yaw()).scale(distance));
    }

    private static BossAction<MeadowPaladin> floorBurst(Function<BossContext<MeadowPaladin>, Vec3> center, int count) {
        return cast -> BossActions.particles(center.andThen(pos -> pos.add(0, .14, 0)),
            new BlockParticleOption(ParticleTypes.BLOCK, MagicRpg.WHITE_STONE.get().defaultBlockState()),
            count, new Vec3(.45, .13, .42), .16).run(cast);
    }

    private static BossActions.Damage<MeadowPaladin> steel(float damage, double knockback) {
        return new BossActions.Damage<>(boss -> boss.damageSources().mobAttack(boss), damage, knockback, true);
    }

    private static BossAction<MeadowPaladin> exposure(int end) {
        return cast -> cast.damageMultiplier(OPENING, 1.35F, end - cast.elapsed(), BossContext.Scope.CAST);
    }
}
