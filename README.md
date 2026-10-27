# Void Client

**The best client — made by bisteck**

Utility client for **Minecraft Java Edition 26.2** (Fabric, Mojang unobfuscated mappings).

| Requirement | Version |
|-------------|---------|
| Minecraft   | **26.2** |
| Java        | **25+** |
| Fabric Loader | `0.20.1+` |
| Fabric Loom | `1.19-SNAPSHOT` |
| Fabric API  | `0.161.0+26.2` |

> Target branch: `port/minecraft-26.2` (not `main` / 1.21.11 Yarn).

## Features

- Modular system (Combat, Movement, Render, Utility, HUD, Misc, Visual)
- ClickGUI dark / black & white compact — **Right Shift**
- Typed settings + JSON config
- Event bus (`Tick` / `Render2D` / `Key`)
- Smooth Freecam (accel + partialTick), Freelook pass-through
- BlockESP / PlayerESP / Tracers (origin on crosshair)
- SilentAura, AutoTool, SlowHit (foil), SusChunkFinder
- BreakWaves / Particles with world-time + partialTick
- AntVanish + staff list, ScreenFilter, CustomSky, FpsBoost

## Build

Requires **JDK 25**.

```bash
./gradlew build
```

Jar: `build/libs/void-client-1.0.0.jar`

```bash
./gradlew runClient
```

CI builds on GitHub Actions against `port/minecraft-26.2`.

## Controls

| Key | Action |
|-----|--------|
| Right Shift | Open / close ClickGUI |
| Module keybinds | Configure per module in GUI |

## License

MIT
