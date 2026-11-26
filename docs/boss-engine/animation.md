# Анимация Boss Engine

Ключевые кадры задают **смещения относительно исходной позы**. Перемещения измеряются в единицах модели, вращения — в радианах, масштаб умножается на исходный. Дорожки для отдельных костей можно хранить в статическом клипе. Для всей модели удобно задать кости перечислением и связать их с `ModelPart`.

## Java API

| Сигнатура | Назначение |
| --- | --- |
| `new KeyframeTrack<T>(List<KeyframeTrack.Key<T>> keys, KeyframeTrack.Interpolator<T> interpolator)` | Дорожка. Первый кадр в тике 0, следующие строго возрастают. |
| `new KeyframeTrack.Key<T>(float tick, T value, Curve incoming)` | Ключевой кадр. Кривая относится к движению от предыдущего кадра к этому. |
| `new BoneTransform(float x, float y, float z, float pitch, float yaw, float roll)` | Смещения и углы. Перегрузка добавляет `scaleX`, `scaleY`, `scaleZ`. `BoneTransform.IDENTITY` соответствует базовой позе. |
| `new AnimationClip<K>(int duration, Map<K, KeyframeTrack<BoneTransform>> tracks)` | Клиентский клип. ID при регистрации в аниматоре совпадает с `BossAbility.Builder.animation(int clipId)`. |
| `new BossModelAnimator<K>(Map<Integer, AnimationClip<K>> clips, float blendTicks)` | Клиентский адаптер. Храни один экземпляр в рендерере. Он поддерживает отдельное состояние воспроизведения для каждой сущности. |
| `Map<K, BoneTransform> sample(BossMob entity, float partialTick)` | Сними позу в `extractRenderState`. Клиент плавно переходит от последней видимой позы к новому клипу или базовой позе. |
| `static <K> void apply(Map<K, BoneTransform> pose, Map<K, ModelPart> parts)` | Примени сохранённую позу в `setupAnim` после `super.setupAnim(state)` и базовых движений модели. |

Кривые `LINEAR`, `STEP`, `SMOOTHSTEP`, `EASE_IN` и `EASE_OUT` задают характер отдельного интервала. Углы интерполируются по заданным значениям. Используй непрерывный ряд углов через границу ±π, если нужно сохранить направление поворота. Клип удерживает последний ключ дорожки до окончания. Для прерывания используй обычно 2–4 тика смешивания и проверяй, что переход не скрывает начало следующего предупреждения.

Этот клип соответствует [рассечению](examples.md). В Java-файле рендерера импортируй классы из `io.github.magicrpg.boss.animation` и `io.github.magicrpg.client.animation.BossModelAnimator`. Времена и ID возьми из общих констант способности.

```java
enum Bone { TORSO }

var torsoTrack = new KeyframeTrack<BoneTransform>(List.of(
    new KeyframeTrack.Key<>(0, BoneTransform.IDENTITY, Curve.LINEAR),
    new KeyframeTrack.Key<>(12, new BoneTransform(0, 1, 0, -.1F, -.85F, 0), Curve.SMOOTHSTEP),
    new KeyframeTrack.Key<>(17, new BoneTransform(0, 1, 0, -.12F, -.95F, 0), Curve.EASE_OUT),
    new KeyframeTrack.Key<>(CONTACT, new BoneTransform(0, 0, 1, .22F, .7F, 0), Curve.EASE_IN),
    new KeyframeTrack.Key<>(CONTACT + 4, new BoneTransform(0, 0, 1, .27F, .9F, 0), Curve.EASE_OUT),
    new KeyframeTrack.Key<>(CLEAVE_DURATION, BoneTransform.IDENTITY, Curve.SMOOTHSTEP)
), BoneTransform::blend);
var clip = new AnimationClip<>(CLEAVE_DURATION, Map.of(Bone.TORSO, torsoTrack));
var animator = new BossModelAnimator<>(Map.of(CLEAVE_CLIP, clip), 3);
```

## Как поставить выразительную атаку

1. Начни с читаемого силуэта. Сначала разметь нейтральную позу, подготовку, контакт, крайнюю позу после удара и готовность к следующему движению. Проверь их с дистанции и ракурса игрока. Разные угрозы должны различаться по движению и контуру зоны ещё до контакта.
2. Перенеси вес до разгона. Зафиксируй опорную ногу, смести таз, затем подключи грудь и плечо. Задержи оружие на несколько кадров относительно корпуса. При контакте стойка и траектория должны объяснять направление силы. Следи за проскальзыванием стоп и хватом обеих рук, если оружие двуручное.
3. Дай разную скорость стадиям. В тяжёлой атаке подготовка обычно длиннее разгона. Ускоряй движение к удару, используй короткое продолжение за точку контакта, затем дай корпусу погасить импульс. Плащ, подвески и свободная рука могут догонять основное движение с небольшой задержкой.
4. Синхронизируй контакт с сервером. Ключ контакта, урон, вспышка и звук используют общий тик. Предупреждение фиксируется достаточно рано, чтобы игрок успел отреагировать. Для прерывания, смены фазы и первого появления в дальности видимости проверь переходы отдельно.
5. Покажи восстановление. Для тяжёлого оружия перенос ноги или возвращение клинка выражает вес и даёт противнику окно для ответного действия. Проверь, что финальный переход к ходьбе сохраняет положение корпуса и не создаёт скачка.
