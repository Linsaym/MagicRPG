# Примеры способностей

Фрагменты рассчитаны на тело класса `ExampleBoss extends BossMob` из [руководства](guide.md). Используй классы Boss Engine из `io.github.magicrpg.boss`, Minecraft `ParticleTypes`, `SoundEvents`, `Vec3` и стандартный `java.util.function.Function`.

## Телеграф и удар по одному контуру

```java
private static final BossContext.Key<BossZone.Placed> CLEAVE_ZONE =
    new BossContext.Key<>(BossZone.Placed.class);
private static final int CONTACT = 20;
private static final int CLEAVE_DURATION = 33;
private static final int CLEAVE_CLIP = 1;
private static final BossAbility<ExampleBoss> CLEAVE = cleave();

private static BossAbility<ExampleBoss> cleave() {
    Function<BossContext<ExampleBoss>, BossZone.Placed> zone = c -> c.recall(CLEAVE_ZONE).orElseThrow();
    var damage = new BossActions.Damage<ExampleBoss>(
        boss -> boss.damageSources().mobAttack(boss), 8, .6, true);
    return BossAbility.<ExampleBoss>builder(MagicRpg.id("example_cleave"), CLEAVE_DURATION)
        .cooldown(48).priority(10).animation(CLEAVE_CLIP)
        .requires(c -> c.target().map(t -> c.boss().distanceToSqr(t) <= 36).orElse(false))
        .interruptWhen((c, source, amount) -> c.elapsed() < CONTACT && amount >= 6)
        .at(0, c -> {
            var boss = c.boss();
            boss.getNavigation().stop();
            c.remember(CLEAVE_ZONE, new BossZone.Cone(6, 55).at(boss.position(), boss.getYRot(), 2));
        })
        .during(0, CONTACT - 1, 4, BossActions.warning(zone, ParticleTypes.WITCH, 24))
        .at(CONTACT, BossAction.all(
            BossActions.sound(SoundEvents.PLAYER_ATTACK_SWEEP, 1, .8F),
            BossActions.hit(zone, damage)))
        .build();
}
```

По окончании удара способность остаётся активной до тика 33, чтобы дать игроку время ответить. ИИ босса может проверять `activeAbility()`, чтобы остановить преследование на время исполнения. Общие константы `CONTACT`, `CLEAVE_DURATION` и `CLEAVE_CLIP` связывают расписание с ключевыми кадрами.

## Волна как продолжение комбинации

Объяви эту способность в той же фазе. Она получает приоритет над рассечением, если последнее полностью исполнено.

```java
private static final BossAbility<ExampleBoss> WAVE =
    BossAbility.<ExampleBoss>builder(MagicRpg.id("example_wave"), 34)
        .priority(20).cooldown(100)
        .requires(c -> c.lastCompleted().filter(CLEAVE.id()::equals).isPresent())
        .at(0, c -> c.damageMultiplier(MagicRpg.id("example_opening"), 1.5F, 10, BossContext.Scope.CAST))
        .during(10, 26, 2, BossActions.wave(10, 26, 9, 1.5, 2,
            c -> c.boss().position(),
            new BossActions.Damage<ExampleBoss>(
                boss -> boss.damageSources().indirectMagic(boss, boss), 5, .4, true),
            ParticleTypes.REVERSE_PORTAL))
        .build();
```

Волна фиксирует центр при первом исполнении. Для движения между атаками можно добавить `at(tick, BossActions.dash(...))` либо отдельную способность. Для призыва и щита в контексте используются те же шаги. Рабочая комбинация призыва, щита и временной стены находится в сценарии `phaseAndCleanup` в [GameTest](../../src/gameTest/java/io/github/magicrpg/boss/test/BossEngineGameTests.java).
