package io.github.magicrpg.boss.test;

import com.mojang.authlib.GameProfile;
import io.github.magicrpg.boss.BossAbility;
import io.github.magicrpg.boss.BossAction;
import io.github.magicrpg.boss.BossActions;
import io.github.magicrpg.boss.BossContext.Scope;
import io.github.magicrpg.boss.BossEncounter;
import io.github.magicrpg.boss.BossFight;
import io.github.magicrpg.boss.BossMob;
import io.github.magicrpg.boss.BossZone;
import io.github.magicrpg.boss.animation.AnimationClip;
import io.github.magicrpg.boss.animation.AnimationCue;
import io.github.magicrpg.boss.animation.AnimationPlayer;
import io.github.magicrpg.boss.animation.BoneTransform;
import io.github.magicrpg.boss.animation.Curve;
import io.github.magicrpg.boss.animation.KeyframeTrack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(BossEngineGameTests.ID)
public final class BossEngineGameTests {
    public static final String ID = "magicrpg_boss_tests";
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, ID);
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS =
        DeferredRegister.create(Registries.TEST_FUNCTION, ID);
    private static final DeferredHolder<EntityType<?>, EntityType<DummyBoss>> DUMMY =
        ENTITIES.register("dummy", key -> EntityType.Builder.of(DummyBoss::new, MobCategory.MISC)
            .sized(.8F, 2).build(ResourceKey.create(Registries.ENTITY_TYPE, key)));
    private static final Map<String, Consumer<GameTestHelper>> TESTS = Map.of(
        "geometry_and_animation", BossEngineGameTests::geometryAndAnimation,
        "scheduling_and_combos", BossEngineGameTests::schedulingAndCombos,
        "phase_and_cleanup", BossEngineGameTests::phaseAndCleanup,
        "movement_and_wave", BossEngineGameTests::movementAndWave,
        "expiry_and_reset", BossEngineGameTests::expiryAndReset,
        "player_targeting", BossEngineGameTests::playerTargeting
    );

    static {
        TESTS.forEach((name, test) -> FUNCTIONS.register(name, () -> test));
    }

    public BossEngineGameTests(IEventBus bus) {
        ENTITIES.register(bus);
        FUNCTIONS.register(bus);
        bus.addListener((EntityAttributeCreationEvent event) ->
            event.put(DUMMY.get(), DummyBoss.createMobAttributes().add(Attributes.MAX_HEALTH, 40).build()));
        bus.addListener((RegisterGameTestsEvent event) -> {
            var environment = event.registerEnvironment(id("normal"),
                new TestEnvironmentDefinition.SetDifficulty(Difficulty.NORMAL));
            TESTS.keySet().forEach(name -> {
                var function = ResourceKey.create(Registries.TEST_FUNCTION, id(name));
                event.registerTest(id(name), new FunctionGameTestInstance(function,
                    new TestData<>(environment, id("arena"), 100, 0, true)));
            });
        });
    }

    private static void geometryAndAnimation(GameTestHelper test) {
        var origin = new Vec3(0, 10, 0);
        var circle = new BossZone.Circle(5).at(origin, 0, 2);
        test.assertTrue(circle.contains(origin.add(3, 2, 4)), "Circle includes its edge and vertical boundary");
        test.assertTrue(!circle.contains(origin.add(3, 2.1, 4)), "Circle rejects feet above the warning volume");
        var cone = new BossZone.Cone(5, 60).at(origin, 90, 1);
        test.assertTrue(cone.contains(origin.add(-4, 0, 0)), "Yaw 90 faces west");
        test.assertTrue(!cone.contains(origin.add(4, 0, 0)), "Cone excludes its rear");
        var line = new BossZone.Line(5, 1).at(origin, 0, 1);
        test.assertTrue(line.contains(origin.add(.95, 0, 4.9)), "Line includes a far corner");
        test.assertTrue(!line.contains(origin.add(1.1, 0, 4)), "Line rejects points outside its width");
        var ring = new BossZone.Ring(2, 5).at(origin, 0, 1);
        test.assertTrue(!ring.contains(origin) && ring.contains(origin.add(2, 0, 0)), "Ring preserves its safe interior");
        for (var zone : List.of(circle, cone, line, ring)) {
            var samples = new ArrayList<Vec3>();
            zone.outline(32, samples::add);
            test.assertValueEqual(samples.size(), 32, "Warning point budget");
            test.assertTrue(samples.stream().allMatch(zone.bounds().inflate(.001)::contains), "Warning stays in bounds");
        }

        var contact = new BoneTransform(0, 0, 10, 0, 0, 0);
        var track = new KeyframeTrack<>(List.of(new KeyframeTrack.Key<>(0, BoneTransform.IDENTITY, Curve.LINEAR),
            new KeyframeTrack.Key<>(4, contact, Curve.EASE_IN),
            new KeyframeTrack.Key<>(6, BoneTransform.IDENTITY, Curve.STEP)), BoneTransform::blend);
        test.assertValueEqual(track.sample(2).z(), 1.25F, "Cubic anticipation");
        test.assertValueEqual(track.sample(4), contact, "Exact contact key");
        test.assertValueEqual(track.sample(5.9F), contact, "Stepped hold");
        test.assertValueEqual(track.sample(12), BoneTransform.IDENTITY, "Clip holds final key");
        var cue = new AnimationCue(1, Integer.MAX_VALUE);
        test.assertValueEqual(AnimationCue.decode(cue.encode()), cue, "Atomic network cue");
        var player = new AnimationPlayer<>(Map.of(1, new AnimationClip<>(6, Map.of("body", track))), 2);
        long impact = (long) Integer.MAX_VALUE + 4;
        test.assertValueEqual(player.sample(cue, impact, 0).get("body"), contact, "Late tracking across clock wrap");
        var idle = new AnimationCue(0, (int) impact);
        test.assertValueEqual(player.sample(idle, impact, 0).get("body"), contact, "No jump on interruption");
        test.assertValueEqual(player.sample(idle, impact + 1, 0).get("body").z(), 5F, "Crossfade midpoint");
        test.assertTrue(player.sample(idle, impact + 2, 0).isEmpty(), "Transition releases animated bones");
        test.succeed();
    }

    private static void schedulingAndCombos(GameTestHelper test) {
        var trace = new ArrayList<String>();
        var high = BossAbility.<DummyBoss>builder(id("high"), 3).priority(10).cooldown(20).animation(4)
            .at(0, c -> {
                test.assertValueEqual(c.boss().animationCue().elapsed(c.level().getGameTime(), 0), 0F, "Start cue");
                trace.add("prepare");
            })
            .at(2, BossAction.all(c -> trace.add("hit"), c -> trace.add("sound")))
            .at(3, c -> trace.add("recover")).build();
        var followUp = BossAbility.<DummyBoss>builder(id("follow_up"), 2).priority(5).cooldown(20)
            .requires(c -> c.lastCompleted().filter(high.id()::equals).isPresent())
            .during(0, 2, 2, c -> trace.add("combo" + c.elapsed())).build();
        var fallback = BossAbility.<DummyBoss>builder(id("fallback"), 1).cooldown(30)
            .at(0, c -> trace.add("fallback")).build();
        spawn(test, encounter(new BossEncounter.Phase<>(id("opening"), List.of(fallback, followUp, high))));
        test.runAfterDelay(10, () -> {
            test.assertValueEqual(trace, List.of("prepare", "hit", "sound", "recover", "combo0", "combo2",
                "fallback"), "Priority, ordering, cooldown and combo sequence");
            test.succeed();
        });
    }

    private static void phaseAndCleanup(GameTestHelper test) {
        var arenaPos = test.absolutePos(new BlockPos(4, 1, 4));
        var persistentPos = test.absolutePos(new BlockPos(5, 1, 4));
        var minions = new ArrayList<Mob>();
        var first = BossAbility.<DummyBoss>builder(id("guard"), 40).cooldown(80).animation(2)
            .interruptWhen((c, source, amount) -> amount >= 2)
            .at(0, c -> {
                c.damageMultiplier(id("shield"), 0, 2, Scope.CAST);
                test.assertTrue(c.temporaryBlock(arenaPos, Blocks.BARRIER.defaultBlockState(), 60, Scope.CAST),
                    "Place cast wall");
                test.assertTrue(c.temporaryBlock(persistentPos, Blocks.LIGHT.defaultBlockState(), 60, Scope.ENCOUNTER),
                    "Place encounter light");
                minions.add(c.summon(EntityTypes.PIG, test.absoluteVec(new Vec3(7.5, 1, 6.5)), 60, Scope.CAST).orElseThrow());
            }).at(29, c -> test.fail("Interrupted ability must not reach impact")).build();
        var second = BossAbility.<DummyBoss>builder(id("frenzy"), 35).animation(3)
            .at(0, c -> c.damageMultiplier(id("vulnerable"), 2, 35, Scope.CAST)).build();
        var boss = spawn(test, encounter(
            new BossEncounter.Phase<>(id("enraged"), c -> c.boss().getHealth() <= 20, List.of(second)),
            new BossEncounter.Phase<>(id("opening"), List.of(first))));
        test.runAfterDelay(2, () -> {
            test.assertValueEqual(boss.animationCue().clip(), 2, "Guard cue is synchronized");
            test.assertTrue(!boss.hurtServer(test.getLevel(), boss.damageSources().magic(), 4), "Shield blocks damage");
            test.assertTrue(minions.getFirst().isAlive(), "Summon spawned");
            test.assertBlockPresent(Blocks.BARRIER, 4, 1, 4);
        });
        test.runAfterDelay(5, () -> {
            test.assertTrue(boss.hurtServer(test.getLevel(), boss.damageSources().magic(), 4), "Shield expired");
            test.assertValueEqual(boss.animationCue().clip(), 0, "Damage interrupts the cast");
            test.assertTrue(minions.getFirst().isRemoved(), "Interruption removes cast summon");
            test.assertBlockPresent(Blocks.AIR, 4, 1, 4);
            test.assertBlockPresent(Blocks.LIGHT, 5, 1, 4);
            boss.setHealth(20);
        });
        test.runAfterDelay(23, () -> {
            test.assertValueEqual(boss.bossFight().phase().orElseThrow(), id("enraged"), "Health changes combat phase");
            test.assertValueEqual(boss.animationCue().clip(), 3, "New phase starts a new ability");
            test.assertTrue(boss.hurtServer(test.getLevel(), boss.damageSources().magic(), 3), "Vulnerability hit");
            test.assertValueEqual(boss.getHealth(), 14F, "Vulnerability doubles incoming damage");
            boss.kill(test.getLevel());
            test.assertBlockPresent(Blocks.AIR, 5, 1, 4);
            test.succeed();
        });
    }

    private static void movementAndWave(GameTestHelper test) {
        var origin = test.absoluteVec(new Vec3(4.5, 1, 4.5));
        var wall = new BlockPos(3, 1, 4);
        test.setBlock(wall, Blocks.STONE);
        var damage = new BossActions.Damage<DummyBoss>(boss -> boss.damageSources().mobAttack(boss), 3, 0, false);
        var wave = BossActions.wave(4, 26, 5, 1, 2, c -> origin, damage, ParticleTypes.WITCH);
        var pulse = BossAbility.<DummyBoss>builder(id("pulse"), 27).cooldown(100)
            .at(0, c -> {
                test.assertTrue(!BossActions.tryTeleport(c, test.absoluteVec(new Vec3(3.5, 1, 4.5))),
                    "Reject teleport into solid block");
                test.assertTrue(BossActions.tryTeleport(c, origin), "Teleport to a free position");
                test.assertValueEqual(c.boss().position(), origin, "Server position updated");
            })
            .at(2, BossActions.leap(c -> new Vec3(-1, 0, 0), .3, .6))
            .at(3, BossActions.dash(c -> new Vec3(0, 0, -1), .7))
            .at(3, c -> test.assertTrue(c.boss().getDeltaMovement().z < -.6, "Dash sets horizontal velocity"))
            .during(4, 26, 2, wave)
            .build();
        var near = test.spawn(EntityTypes.PIG, new Vec3(5.5, 1, 4.5));
        var far = test.spawn(EntityTypes.PIG, new Vec3(8.8, 1, 4.5));
        var outside = test.spawn(EntityTypes.PIG, new Vec3(10.5, 1, 4.5));
        for (var pig : List.of(near, far, outside)) {
            pig.setNoAi(true);
        }
        var boss = spawn(test, encounter(new BossEncounter.Phase<>(id("opening"), List.of(pulse))));
        test.runAfterDelay(31, () -> {
            test.assertValueEqual(near.getHealth(), 7F, "Near target hit once");
            test.assertValueEqual(far.getHealth(), 7F, "Far target hit once");
            test.assertValueEqual(outside.getHealth(), 10F, "Target past radius avoids damage");
            test.assertTrue(boss.getZ() < origin.z, "Movement is simulated by the server");
            test.succeed();
        });
    }

    private static void expiryAndReset(GameTestHelper test) {
        var summons = new ArrayList<Mob>();
        var timed = test.absolutePos(new BlockPos(3, 1, 3));
        var replaced = test.absolutePos(new BlockPos(4, 1, 3));
        var hardStop = test.absolutePos(new BlockPos(5, 1, 3));
        var ability = BossAbility.<DummyBoss>builder(id("field"), 50)
            .at(0, c -> {
                summons.add(c.summon(EntityTypes.PIG, test.absoluteVec(new Vec3(7.5, 1, 7.5)), 4, Scope.ENCOUNTER)
                    .orElseThrow());
                for (var pos : List.of(timed, replaced, hardStop)) {
                    test.assertTrue(c.temporaryBlock(pos, Blocks.LIGHT.defaultBlockState(),
                        pos.equals(hardStop) ? 100 : 4, Scope.ENCOUNTER), "Temporary light placed");
                }
                test.setBlock(4, 1, 3, Blocks.STONE);
            }).build();
        var boss = spawn(test, encounter(new BossEncounter.Phase<>(id("opening"), List.of(ability))));
        test.runAfterDelay(12, () -> {
            test.assertTrue(summons.getFirst().isRemoved(), "Summon expires during active ability");
            test.assertBlockPresent(Blocks.AIR, 3, 1, 3);
            test.assertBlockPresent(Blocks.STONE, 4, 1, 3);
            test.assertBlockPresent(Blocks.LIGHT, 5, 1, 3);
            boss.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            test.assertBlockPresent(Blocks.AIR, 5, 1, 3);
            test.succeed();
        });
    }

    private static void playerTargeting(GameTestHelper test) {
        var origin = test.absoluteVec(new Vec3(6.5, 1, 6.5));
        var hit = BossActions.hit(c -> new BossZone.Circle(3).at(origin, 0, 1),
            new BossActions.Damage<DummyBoss>(boss -> boss.damageSources().mobAttack(boss), 4, 0, false));
        var pulse = BossAbility.<DummyBoss>builder(id("players"), 5).cooldown(100).at(2, hit).build();
        var boss = spawn(test, new BossEncounter<>(List.of(new BossEncounter.Phase<>(id("opening"), List.of(pulse)))));

        var primary = addPlayer(test, "primary", origin.add(1, 0, 0), GameType.SURVIVAL);
        var secondary = addPlayer(test, "secondary", origin.add(0, 0, 1), GameType.SURVIVAL);
        var observer = addPlayer(test, "observer", origin.add(-1, 0, 0), GameType.CREATIVE);
        boss.setTarget(primary);
        test.runAfterDelay(8, () -> {
            test.assertValueEqual(primary.getHealth(), 16F, "Primary player hit");
            test.assertValueEqual(secondary.getHealth(), 16F, "Second player hit by the same cast");
            test.assertValueEqual(observer.getHealth(), 20F, "Creative observer excluded");
            for (var player : List.of(primary, secondary, observer)) {
                test.getLevel().removePlayerImmediately(player, Entity.RemovalReason.DISCARDED);
            }
            test.succeed();
        });
    }

    private static FakePlayer addPlayer(GameTestHelper test, String name, Vec3 at, GameType mode) {
        var player = new FakePlayer(test.getLevel(), new GameProfile(UUID.randomUUID(), name));
        player.setPos(at);
        player.setGameMode(mode);
        player.setPermanentlyInvulnerable(false);
        player.connection.markClientLoaded();
        test.getLevel().addNewPlayer(player);
        return player;
    }

    @SafeVarargs
    private static BossEncounter<DummyBoss> encounter(BossEncounter.Phase<DummyBoss>... phases) {
        return new BossEncounter<>(c -> true, (boss, victim) -> testTarget(victim), List.of(phases));
    }

    private static boolean testTarget(LivingEntity victim) {
        return victim.getType() == EntityTypes.PIG;
    }

    private static DummyBoss spawn(GameTestHelper test, BossEncounter<DummyBoss> definition) {
        var dummy = test.spawn(DUMMY.get(), new Vec3(2.5, 1, 2.5));
        dummy.fight = new BossFight<>(dummy, definition);
        return dummy;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ID, path);
    }

    private static final class DummyBoss extends BossMob {
        private BossFight<DummyBoss> fight = new BossFight<>(this,
            new BossEncounter<>(List.of(new BossEncounter.Phase<>(id("idle"), List.of()))));

        private DummyBoss(EntityType<? extends BossMob> type, Level level) { super(type, level); }

        @Override
        protected BossFight<DummyBoss> bossFight() { return fight; }
    }
}
