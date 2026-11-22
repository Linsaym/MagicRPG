# Маршруты по коду и ресурсам MagicRPG

Правила проекта и порядок передачи на проверку собраны в [AGENTS.md](../AGENTS.md). Настройка JDK и зависимости описаны в [README.md](../README.md). Для сверки сигнатур Minecraft и Reforged см. [справочник API](minecraft-api.md).

## Новое измерение

1. **Готовая карта.** В Златолесье цепочка идёт от [источник карты](../tools/bake_goldenwood.py) к [ресурсу в JAR](../src/main/resources/data/magicrpg/island/goldenwood.bin.gz) и [загрузчику](../src/main/java/io/github/magicrpg/world/AuthoredIsland.java). Сохрани редактируемый источник. Сверь настройки загрузчика, высоты и биом в [описании измерения](../src/main/resources/data/magicrpg/dimension/goldenwood.json) и [типе измерения](../src/main/resources/data/magicrpg/dimension_type/goldenwood.json). Постройки Златолесья уже входят в карту. Часть идентификаторов структур нужна для поиска ориентиров.
2. **Портал и сохранение связи.** Проследи путь от `CharredFang` через [`PortalRoute`](../src/main/java/io/github/magicrpg/world/PortalRoute.java) и `LinkedPortalBlock` до [`PortalLinks`](../src/main/java/io/github/magicrpg/world/PortalLinks.java). При добавлении третьего маршрута собери настройки рамы, площадки, хранилища и сообщений рядом с его объявлением. Сейчас эти решения разбросаны по проверкам `GOLDENWOOD`. Сохрани ключи хранилищ и формат записей действующих порталов.
3. **Обитатели и ресурсы.** Посмотри регистрацию в [`Goldenwood.java`](../src/main/java/io/github/magicrpg/Goldenwood.java), [настройки естественного спавна](../src/main/resources/data/magicrpg/worldgen/biome/goldenwood_glade.json) и строки таблицы ниже. Фиксированные спавнеры и контейнеры задаются в источнике карты.
4. **Таумономикон.** Возьми за пример [ветку Златолесья](../src/main/java/io/github/magicrpg/compat/reforged/GoldenwoodMagic.java). Выбери свободные координаты узла и обнови обе локализации. Если исследование должно ограничивать физический вход, добавь проверку в серверной активации. Связь узлов Таумономикона сама по себе портал не блокирует.

Собери JAR по AGENTS.md и кратко укажи, что игрок должен увидеть. Добавь отдельный шаг, если результат проявляется только после конкретного действия, например после перезапуска мира.

### Пересборка карты Златолесья

После правок рельефа, троп, построек или встроенных спавнеров и контейнеров обнови `src/main/resources/data/magicrpg/island/goldenwood.bin.gz` из корня проекта с Python 3:

```powershell
python tools/bake_goldenwood.py
```

Затем собери JAR по [AGENTS.md](../AGENTS.md#сборка). В обычной сборке Gradle упаковывает уже подготовленный файл карты.

### Пересборка карты Лиловых лугов

После правок карты или расстановки обитателей запусти `python tools/bake_lilac_meadows.py` из корня проекта. Скрипт обновляет `src/main/resources/data/magicrpg/island/lilac_meadows.bin.gz`. Gradle упаковывает этот файл при сборке JAR.

## Другие задачи

Java-пути в таблице отсчитываются от `src/main/java/io/github/magicrpg/`. Игровые данные лежат в `src/main/resources/data/magicrpg/`, клиентские ресурсы и переводы — в `src/main/resources/assets/magicrpg/`.

| Задача | Куда идти |
| --- | --- |
| Моб или босс | `entity/` → регистрация типа, атрибутов и условий появления в `MagicRpg.java` или `Goldenwood.java` → JSON биома или спавнер на карте → `loot_table/entities/` → `client/ClientRegistration.java`, модель и текстура. |
| Предмет, блок или рецепт | `MagicRpg.java` или модуль механики → творческая вкладка в `MagicRpg.java` → `compat/reforged/` для аспектов и магических рецептов → `recipe/` для обычных рецептов → модели, текстуры, переводы и таблица добычи блока по необходимости. |
| Исследование | `compat/reforged/ReforgedIntegration.java` → класс нужной ветки → обе локализации. Сверь существующие ключи, родителей, координаты узлов и соответствие страниц зарегистрированному рецепту. |
| Призыв и расход предметов | `item/SummoningSeal.java` и `compat/reforged/ReforgedIntegration.java` для печати гончего. Начни с действия предметом и проследи серверные условия, момент успешного призыва и списание. |
| Переходы и сохранение | Для арок начни с `world/LinkedPortalBlock.java` и `world/PortalLinks.java`. Для путевых зеркал начни с `WayfarerMirrors.java`, `world/WayfarerMirrorBlock.java`, `world/WayfarerMirrorEntity.java` и `world/WayfarerMirrorLinks.java`. Проследи связь от создания до чтения после перезапуска. |
