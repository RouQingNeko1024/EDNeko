# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

edneko is a Minecraft Forge 1.8.9 client based on the LiquidBounce legacy codebase. It is a mixed Kotlin/Java Gradle project using ForgeGradle, Sponge Mixin, ShadowJar, and Java 8 bytecode.

## Common commands

Use the Gradle wrapper from the repository root.

- Initial Forge workspace setup plus build: `./gradlew setupDevWorkspace build` (Windows PowerShell: `./gradlew.bat setupDevWorkspace build`)
- CI-equivalent build: `./gradlew build --no-daemon` (Windows PowerShell: `./gradlew.bat build --no-daemon`)
- Build the shaded/reobfuscated mod jar: `./gradlew build`; output is under `build/libs/`
- Prepare IDE metadata: `./gradlew idea` (Windows PowerShell: `./gradlew.bat idea`)
- List available Gradle tasks: `./gradlew tasks`

There is no dedicated test source set in this repository currently. If tests are added later, use Gradle's standard test task, e.g. `./gradlew test` or `./gradlew test --tests fully.qualified.TestName`.

## Build details

- Java source/target compatibility is 1.8; CI uses Temurin JDK 8.
- Kotlin JVM target is 1.8; Kotlin version is configured in `gradle.properties`.
- `build` depends on `reobfShadowJar`; the plain `jar` task is disabled.
- Forge runtime config is in the `minecraft` block of `build.gradle`: Minecraft/Forge `1.8.9-11.15.1.2318-1.8.9`, run directory `run`, mappings `stable_22`, and the Mixin coremod JVM arg.
- Resources are processed with version expansion for `mcmod.info`, then copied into `build/classes/java` by the custom `moveResources` task.

## Architecture

- Main client lifecycle is centered on `net.ccbluex.liquidbounce.LiquidBounce`. `preload()` loads languages, alt generators, and SRG mappings asynchronously. `startClient()` loads fonts/settings, registers listeners, commands, modules, scripts, configs, tabs, input fixes, and optional Discord RPC. Shutdown saves configs and tears down runtime services.
- Forge/Mixin injection enters through `net.ccbluex.liquidbounce.injection.forge.MixinLoader`, which bootstraps Sponge Mixin, loads `liquidbounce.forge.mixins.json`, and registers ASM transformers. Minecraft hooks live under `net.ccbluex.liquidbounce.injection.forge.mixins` grouped by target area such as `client`, `entity`, `gui`, `render`, `network`, `world`, and `packets`.
- Modules are singleton-style objects extending `features.module.Module`, grouped under `features.module.modules.<category>`. `ModuleManager.registerModules()` contains the central registry list; add new built-in modules there so they are toggleable and receive generated setting commands.
- Module settings come from the `Configurable`/`Value` system. Module state, keybinds, and values are saved via `FileManager` configs in the Minecraft data directory under `edneko-1.8.9`.
- Events are dispatched by `event.EventManager`. Handlers are priority-ordered `EventHook`s; normal listeners should be initialized during startup. `Module.handleEvents()` gates module event handling by enabled state and `GameDetector` activity.
- Commands are registered in `features.command.CommandManager.registerCommands()`. The default prefix is `.`. Commands live under `features.command.commands`; module setting commands are generated automatically for modules with values.
- Scripts are managed by `script.ScriptManager`, with SRG remapping support loaded during preload/startup.
- UI/HUD code is under `ui.client`, including click GUIs, HUD elements, font management, alt manager, and visual styles. Static assets, fonts, language JSON, shaders, images, and Mixin config are under `src/main/resources/assets/minecraft/edneko` and root resource files.
- Utility code is organized under `utils` by concern: client, movement, render, rotation, inventory, timing, packet/network, IO, Kotlin helpers, and related systems.

## Conventions from repository docs

- Prefer Kotlin for new code when practical; Java is still present for mixins and legacy code.
- Keep package names under `net.ccbluex.liquidbounce` unless the code is genuinely self-contained and intentionally external.
- New `.kt` and `.java` files should include the repository's short edneko header from `.github/CODING_STANDARDS.md`.
- Follow Kotlin official conventions and Java conventions used by the surrounding code.

## Notes for changes

- When adding or changing mixins, update `src/main/resources/liquidbounce.forge.mixins.json` and preserve Java 8/Mixin 0.7 compatibility.
- When adding modules, wire the object into `ModuleManager.registerModules()` and choose the correct `Category`; if it exposes values, a module command will be generated automatically.
- When adding commands, register them in `CommandManager.registerCommands()` unless they are script/shortcut-generated.
- For resource paths, existing assets are usually addressed below `assets/minecraft/edneko`.
