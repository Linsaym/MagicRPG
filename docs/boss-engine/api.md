# Справочник Java API

Основной пакет — `io.github.magicrpg.boss`. Сигнатуры ниже приведены для `B extends BossMob`.

## Описание боя

| Java API | Поведение |
| --- | --- |
| `new BossFight<B>(B boss, BossEncounter<B> encounter)` | Контроллер. Верни его из `protected BossFight<?> bossFight()` своей сущности. |
| `new BossEncounter<B>(Predicate<BossContext<B>> activeWhen, BiPredicate<B, LivingEntity> canHit, List<Phase<B>> phases)` | Условия активности, дополнительный фильтр целей и упорядоченные фазы. Перегрузка с одним списком использует текущую цель для активации. |
| `new BossEncounter.Phase<B>(Identifier id, Predicate<BossContext<B>> condition, List<BossAbility<B>> abilities)` | Условная фаза. Перегрузка `(Identifier, List<BossAbility<B>>)` всегда доступна. |
| `BossAbility.<B>builder(Identifier id, int duration)` | Положительная длительность в тиках. Один ID может использоваться несколькими фазами, если они ссылаются на одно определение. |
| `Builder<B> cooldown(int ticks)`, `priority(int value)`, `animation(int clipId)` | Настройки выбора и воспроизведения. Положительный ID клипа связывает способность с клиентским клипом. ID 0 зарезервирован для обычной позы. |
| `Builder<B> requires(Predicate<BossContext<B>> predicate)` | Дополнительное условие старта. Последовательные вызовы объединяются через И. |
| `Builder<B> interruptWhen(InterruptRule<B> rule)` | Правило принимает контекст, `DamageSource` и величину после временных множителей, до брони. Вызывается после успешного получения урона выжившим боссом. |
| `Builder<B> at(int tick, BossAction<B> action)`, `during(int first, int last, int interval, BossAction<B> action)`, `BossAbility<B> build()` | Расписание и создание способности. Допускается до 128 записей. |
| `BossAction.all(BossAction<B>... actions)`, `BossAction.when(Predicate<BossContext<B>> condition, BossAction<B> action)` | Последовательное исполнение и условное действие. Прерывание останавливает оставшиеся действия. |
| `BossFight<B>.interrupt()` | Явно прерывает текущую способность. Возвращает `true`, если она была активна. |

## Контекст исполнения

- `B boss()`, `ServerLevel level()`, `Optional<LivingEntity> target()`, `boolean isHostile(LivingEntity entity)`.
- `Optional<Identifier> phase()`, `long phaseTicks()`, `Optional<Identifier> lastCompleted()`, `Optional<BossAbility<B>> ability()`, `int elapsed()`, `boolean isCasting()`, `void interrupt()`.
- `new BossContext.Key<T>(Class<T> type)`, `<T> void remember(Key<T> key, T value)`, `<T> Optional<T> recall(Key<T> key)`. Объявляй ключ один раз, храни значения в контексте конкретного исполнения.
- `void damageMultiplier(Identifier key, float factor, int ticks, Scope scope)`. `0` создаёт щит, значения выше `1` усиливают входящий урон. Повтор ключа заменяет эффект, разные ключи перемножаются. Множители обходятся источниками с тегом `BYPASSES_INVULNERABILITY`.
- `<M extends Mob> Optional<M> summon(EntityType<M> type, Vec3 at, int ticks, Scope scope)`, `void retargetSummons()`. Призыв назначает цель способности. Повторное наведение использует текущую цель босса или сбрасывает её у призванных мобов.
- `boolean temporaryBlock(BlockPos pos, BlockState state, int ticks, Scope scope)`. Возвращает `false`, если клетка занята, тип блока не поддерживается или исчерпан лимит. См. [ограничения арены](architecture.md#временные-ресурсы).

Методы, создающие временные ресурсы или меняющие память, вызывай только из действий активной способности. Предикаты выбора используют контекст для чтения.

## Геометрия, эффекты и движение

| Java API | Поведение |
| --- | --- |
| `new BossZone.Circle(double radius)`, `new BossZone.Cone(double radius, double halfAngleDegrees)`, `new BossZone.Line(double length, double halfWidth)`, `new BossZone.Ring(double innerRadius, double outerRadius)` | Геометрия в горизонтальной плоскости. Полуугол конуса измеряется в градусах. Линия — прямоугольная полоса вперёд от точки установки. |
| `BossZone.Placed at(Vec3 origin, float yaw, double halfHeight)` | Устанавливает фигуру. Minecraft yaw 0 направлен вдоль +Z. Установленная зона даёт `bounds()`, `contains(Vec3 feet)` и `outline(int points, Consumer<Vec3> visitor)`. |
| `BossActions.warning(Function<BossContext<B>, BossZone.Placed> zone, ParticleOptions particle, int points)` | Рисует контур из 8–96 точек. Зафиксируй одну зону для предупреждения и удара. |
| `BossActions.hit(Function<BossContext<B>, BossZone.Placed> zone, Damage<B> damage)` | Применяет `new Damage<B>(Function<B, DamageSource> source, float amount, double knockback, boolean requireSight)`. Проверка видимости идёт от босса. |
| `BossActions.wave(int first, int last, double radius, double thickness, double halfHeight, Function<BossContext<B>, Vec3> origin, Damage<B> damage, ParticleOptions particle)` | Расширяющийся фронт. Вызывай одним экземпляром действия через `during` от `first` до `last`, включая финальный тик. Волна запоминает уже затронутые цели и покрывает радиальный промежуток между вызовами. |
| `BossActions.dash(Function<BossContext<B>, Vec3> direction, double speed)`, `leap(Function<BossContext<B>, Vec3> direction, double speed, double rise)` | Устанавливают импульс, дальше действуют обычные столкновения Minecraft. Прыжок начинается на земле. Скорости в блоках за тик. |
| `BossActions.teleport(Function<BossContext<B>, Vec3> destination)`, `boolean tryTeleport(BossContext<?> context, Vec3 at)` | Телепортирует при наличии свободного места в загруженной области. Выбор поверхности и проверку пола при необходимости добавляет способность. |
| `BossActions.particles(Function<BossContext<B>, Vec3> position, ParticleOptions particle, int count, Vec3 spread, double speed)`, `sound(SoundEvent sound, float volume, float pitch)` | Отправляют эффекты с сервера с учётом общего бюджета босса. |

См. [API клипов и дорожек](animation.md#java-api).
