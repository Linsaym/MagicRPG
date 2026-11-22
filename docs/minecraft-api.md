# API MagicRPG для Minecraft 26.3

## Источники и проверка сигнатур

- Проверяй `thaumcraft.*` на локальном JAR, который использует сборка. Для поиска классов и ресурсов используй `jar tf`, для просмотра сигнатур — `javap`, для просмотра поведения — `javap -p -c`. Если этих данных мало, сверься с [исходниками тега `26.3`](https://github.com/notlikethiss/thaumcraft-4-reforged/tree/26.3). У старых Thaumcraft и других портов отличаются пакеты и методы. [Релиз Reforged 1.1.3](https://github.com/notlikethiss/thaumcraft-4-reforged/releases/tag/26.3)
- Для Minecraft и NeoForge проверяй сигнатуры в зависимостях ModDevGradle из текущего проекта. Публичные примеры для другой версии годятся для понимания механизма, их импорты и сигнатуры нужно сверять с 26.3.

Из корня проекта в PowerShell, с JDK 25 в `JAVA_HOME`:

```powershell
$tc = Join-Path $PWD 'libs/thaumcraft-4-reforged-1.1.3-26.3.jar'
if (!(Test-Path -LiteralPath $tc)) {
    $tc = Join-Path $env:APPDATA '.minecraft/mods/thaumcraft-4-reforged-1.1.3-26.3.jar'
}
# Если сборка использует -PprofileMods, укажи тот же путь к JAR в $tc.
& "$env:JAVA_HOME/bin/javap.exe" -classpath $tc thaumcraft.research.ResearchCategories thaumcraft.crafting.TcRecipes thaumcraft.aspect.AspectSync

.\gradlew.bat createMinecraftArtifacts
$mc = 'build/moddev/artifacts/minecraft-patched-26.3.0.115.jar'
# Если изменился neo_version, укажи соответствующий JAR в $mc.
& "$env:JAVA_HOME/bin/javap.exe" -classpath $mc net.minecraft.world.level.block.Portal net.minecraft.world.level.portal.TeleportTransition net.minecraft.world.level.saveddata.SavedDataType net.minecraft.world.level.storage.SavedDataStorage
```

## Reforged 1.1.3

Рабочий пример регистрации исследования, рецепта и аспектов находится в [ReforgedIntegration.java](../src/main/java/io/github/magicrpg/compat/reforged/ReforgedIntegration.java). Остальной код MagicRPG обращается к этому пакету за интеграцией с Reforged.

| Задача | Точка входа |
| --- | --- |
| Вкладка Таумономикона | `thaumcraft.research.ResearchCategories.registerCategory(key, icon, background)` принимает ключ вкладки и два `Identifier` с путями к текстурам. Имя задаёт перевод `tc.research_category.<key>`. |
| Исследование | `ResearchItem` с ключом вкладки, `setPages(...)`, `registerResearchItem()`. Имя и подпись читаются из `tc.research_name.<key>` и `tc.research_text.<key>`. `ResearchPage.text(...)` принимает ключ перевода. `setSecondary()` позволяет купить исследование за очки аспектов при стандартной сложности исследований. |
| Магический верстак | `thaumcraft.crafting.TcRecipes.register(new ShapelessArcaneRecipe(key, research, result, aspects, ingredients))`. `research` совпадает с ключом исследования, а `ResearchPage.arcane(key)` ссылается на ключ рецепта. Результат и ингредиенты задаются через `TcStack` и `TcIngredient`. |
| Аспекты для сканирования | `thaumcraft.aspect.AspectSync.addRegistrar(registry -> ...)`. Используй `registry.register(item, aspects)` и `registry.registerEntity(type, aspects)`. Reforged вызывает обработчик при перестроении таблиц аспектов и синхронизирует их с клиентом. |
| Магические каменные кирпичи | `thaumcraft.registry.ModBlocks.ARCANE_STONE_BRICKS.get()` возвращает блок `thaumcraft:arcane_stone_bricks`. Для проверки рамы используй блок через интеграцию Reforged. |

Исследования и рецепты регистрируются в очереди `FMLCommonSetupEvent.enqueueWork(...)`. Цена рецепта задаётся в целых единицах вис: `Aspect.FIRE` — Ignis, `Aspect.ENTROPY` — Perditio. Верстак проверяет исследование и вис, применяет модификаторы жезла и списывает вис при взятии результата. Для проверки точной цены используй жезл с нейтральным модификатором. Для проверки сканирования убедись, что игрок уже знает составные части сложных аспектов.

В Reforged магическое зеркало `thaumcraft:magic_mirror` переносит выброшенные предметы. `thaumcraft.device.MirrorBlock#entityInside` передаёт `ItemEntity` в `MirrorBlockEntity#transport`. Зеркало эссенции `thaumcraft:essentia_mirror` работает с эссенцией через `EssentiaMirrorBlockEntity`. Переход игроков в MagicRPG реализован отдельно в [LinkedPortalBlock.java](../src/main/java/io/github/magicrpg/world/LinkedPortalBlock.java).

## Порталы и сохранение в 26.3

- `net.minecraft.world.level.block.Portal` определяет `getPortalDestination(ServerLevel, Entity, BlockPos)`. В блоке регистрируй вход через `Entity.setAsInsidePortal(...)`, проверяй возможность прохода на сервере и возвращай `TeleportTransition` только при безопасной точке назначения.
- `ServerLevel.getDataStorage()` возвращает `net.minecraft.world.level.storage.SavedDataStorage`. Связи порталов между измерениями можно хранить в одном `SavedData` через хранилище обычного мира. `SavedDataType` использует `Identifier`, фабрику и `Codec`. Храни ключи измерений и координаты, помечай изменённые данные через `setDirty()` и проверяй загрузку после перезапуска. [Справочник NeoForge по SavedData](https://docs.neoforged.net/docs/datastorage/saveddata/)
