# Flower Client

A PvP client for Minecraft **1.21.1** in the spirit of Feather and Dawn. It has two parts:

| Part | What it is | Folder |
|---|---|---|
| **Launcher** | Electron app: Microsoft sign-in, automatic Java 21, Fabric install, mod syncing, the ASCII bloom intro | `launcher/` |
| **Mod** | Fabric client mod: HUD mods, Right Shift menu, drag-and-drop HUD editor, zoom, toggle sprint | `mod/` |

## Features

**In game (press `Right Shift`)**
- HUD mods: FPS, CPS (left | right), Keystrokes with CPS, Coordinates + biome, Armor Status, Potion Effects, Ping, Direction, Clock, Speed
- **Edit HUD**: drag elements anywhere, scroll over one to resize it (50–300%), right-click to toggle its background, and it snaps to the screen centre
- Zoom: hold `C`, scroll to zoom further. It's smooth and leaves your hand alone, like OptiFine.
- Toggle Sprint
- A "✿ Flower" button on the title screen, and the window title becomes "Flower Client 1.21.1"
- Settings are saved to `config/flowerclient.json` (the launcher reads and writes the same file)

**Launcher**
- Microsoft account sign-in (via `msmc`). The token is encrypted with your OS keychain.
- Downloads Eclipse Temurin Java 21 automatically, or you can point it at your own Java
- Installs the latest stable Fabric loader for 1.21.1
- Keeps Fabric API up to date from Modrinth, plus optional Sodium, Lithium, FerriteCore and Entity Culling (each download is checked against its SHA-1)
- Never touches mods you add to the `mods` folder yourself
- Turn in-game modules on/off before you launch; settings for memory, resolution, game folder, Java and JVM args; a live game log
- A 3D ASCII flower blooms on start, scatters into particles when you press Play, and grows back when the game closes

## Getting an installer (easiest)

1. Push this folder to a GitHub repo.
2. GitHub Actions (`.github/workflows/main.yml`) builds the mod jar, puts it inside the launcher and makes a Windows installer.
3. Open the **Actions** tab → latest run → download **FlowerClient-Setup**.
   If you push a tag like `v1.0.0`, the installer and mod jar are also attached to a GitHub Release.

## Building locally

Requirements: **JDK 21**, **Gradle 8.12+**, **Node 20+**.

```bash
# 1. Build the mod
cd mod
gradle build                      # → mod/build/libs/flowerclient-1.0.0+1.21.1.jar

# 2. Run the launcher (it finds the jar in ../mod/build/libs automatically)
cd ../launcher
npm install
npm start

# 3. Make an installer (copy the jar in first so it gets bundled)
cp ../mod/build/libs/flowerclient-*.jar resources/mods/   # skip the -sources jar
npm run dist                      # → launcher/dist/Flower Client Setup 1.0.0.exe
```

To test only the mod, drop the jar and Fabric API into any Fabric 1.21.1 instance's `mods` folder.

## Project layout

```
mod/
  src/main/java/dev/flowerclient/
    FlowerClient.java          entrypoint: keybinds, HUD render, title-screen button
    module/                    Module, HudModule, ModuleManager + impl/ (all mods)
    gui/                       FlowerScreen (menu), HudEditorScreen, Theme
    config/ConfigManager.java  JSON config shared with the launcher
    mixin/                     zoom FOV, zoom scroll, window title
launcher/
  main.js / preload.js         window + secure IPC bridge
  src/                         auth, java, fabric, mods, game (launch), store, net
  renderer/                    index.html, styles.css, app.js, bloom.js (ASCII flower)
```

## Adding a module

1. Make a class in `mod/.../module/impl/`. For a one-line HUD readout, extend `TextHudModule`; otherwise extend `HudModule` or `Module`.
2. Register it in `ModuleManager.init()`.
3. Add the same name to `MODULES` in `launcher/src/mods.js` so the launcher can toggle it.

## Notes

- The mod is a cosmetic/QoL client like Feather and Lunar. It doesn't have any hacks, so it's safe on servers that allow PvP clients.
- Targets Minecraft 1.21.1 only. A different version means changing `mod/gradle.properties`, checking the mixin method signatures still match, and updating `MC_VERSION` in `launcher/src/game.js`.
- Not code-signed, so Windows SmartScreen will warn the first time ("More info" → "Run anyway").
