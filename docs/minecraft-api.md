# API MagicRPG для Minecraft 26.3

## Источники и проверка сигнатур

Проверяй сигнатуры в JAR, с которыми собирается проект. Для поиска классов и ресурсов используй `jar tf`, для сигнатур — `javap`, для поведения — `javap -p -c`. Если локального Reforged недостаточно, см. [исходники тега `26.3`](https://github.com/notlikethiss/thaumcraft-4-reforged/tree/26.3).

Из корня проекта в PowerShell, с JDK 25 в `JAVA_HOME`:

```powershell
$tc = Join-Path $PWD 'libs/thaumcraft-4-reforged-1.1.3-26.3.jar'
if (!(Test-Path -LiteralPath $tc)) {
    $tc = Join-Path $env:APPDATA '.minecraft/mods/thaumcraft-4-reforged-1.1.3-26.3.jar'
}
# Если сборка использует -PprofileMods, укажи тот же путь к JAR в $tc.
& "$env:JAVA_HOME/bin/javap.exe" -classpath $tc thaumcraft.research.ResearchCategories thaumcraft.crafting.TcRecipes thaumcraft.aspect.AspectSync

$neoVersion = (Get-Content -Raw gradle.properties | ConvertFrom-StringData).neo_version
$mc = "build/moddev/artifacts/minecraft-patched-$neoVersion.jar"
if (!(Test-Path -LiteralPath $mc)) {
    .\gradlew.bat createMinecraftArtifacts
    if ($LASTEXITCODE -ne 0) { throw 'Не удалось подготовить JAR Minecraft' }
}
& "$env:JAVA_HOME/bin/javap.exe" -classpath $mc net.minecraft.world.level.block.Portal net.minecraft.world.level.portal.TeleportTransition net.minecraft.world.level.saveddata.SavedDataType net.minecraft.world.level.storage.SavedDataStorage
```

## Reforged 1.1.3

| Задача | API и пример |
| --- | --- |
| Вкладка и исследования | `ResearchCategories.registerCategory`, `ResearchItem.setParents`, `ResearchPage.text`. Регистрация через `FMLCommonSetupEvent.enqueueWork` в [ReforgedIntegration.java](../src/main/java/io/github/magicrpg/compat/reforged/ReforgedIntegration.java#L42). |
| Магический верстак | `ShapelessArcaneRecipe` и `ResearchPage.arcane). [Печать гончего](../src/main/java/io/github/magicrpg/compat/reforged/ReforgedIntegration.java#L60). |
| Инфузия | `InfusionRecipe` и `ResearchPage.infusion`. [Кольцо Златолесья](../src/main/java/io/github/magicrpg/compat/reforged/GoldenwoodMagic.java#L60). |
| Обычный рецепт на странице | `CraftingRecipeRef` и `ResearchPage.crafting`. [Материалы Златолесья](../src/main/java/io/github/magicrpg/compat/reforged/GoldenwoodMagic.java#L53). |
| Аспекты | `AspectSync.addRegistrar`, затем `registry.register` для предметов или `registry.registerEntity` для мобов. [Регистрация аспектов Златолесья](../src/main/java/io/github/magicrpg/compat/reforged/GoldenwoodMagic.java#L77). |

Магическое зеркало Reforged переносит выброшенные предметы. Переход игрока через путевое зеркало MagicRPG реализован в [WayfarerMirrorBlock.java](../src/main/java/io/github/magicrpg/world/WayfarerMirrorBlock.java#L161).

## Порталы и сохранение в 26.3

- `Portal.getPortalDestination(ServerLevel, Entity, BlockPos)` возвращает `TeleportTransition` для безопасной точки выхода. Вход регистрируется через `Entity.setAsInsidePortal`. [LinkedPortalBlock.java](../src/main/java/io/github/magicrpg/world/LinkedPortalBlock.java)
- `SavedDataType` связывает идентификатор, фабрику и `Codec`. Связи арок хранятся в обычном мире через `SavedDataStorage.computeIfAbsent`. После изменения данных вызывается `setDirty()`. [PortalLinks.java](../src/main/java/io/github/magicrpg/world/PortalLinks.java)
