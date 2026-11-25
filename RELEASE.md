# Flower Client — Discord announcement & patch notes

---

## Discord release message (copy–paste)

> # Flower Client v1.0.0 is here
>
> A light, clean PvP client for **Minecraft 1.21.1 (Fabric)** — HUD mods you can drag anywhere, smooth zoom, and the performance mods that matter, kept up to date for you. No hacks, safe on servers that allow PvP clients.
>
> **Launcher**
> • One-click **Microsoft sign-in**, auto-downloads **Java 21** and installs **Fabric** for you
> • Built-in **Modrinth browser** — search & install mods, dependencies handled
> • **Performance mods** (Sodium, Lithium, FerriteCore, Entity Culling) kept up to date
> • Living **ASCII flower** intro that changes with the seasons
> • **Auto-update** — new versions install themselves
>
> **In-game** (press **Right Shift**)
> • HUD: FPS, CPS, Keystrokes, Coordinates, Armor, Potion effects, Ping, Direction, Clock, Speed
> • **Biome Tint** — your HUD recolors to match the biome you're in
> • **Zoom** (hold C), **Toggle Sprint**, drag-and-drop **HUD editor**
> • **Client-side day/night** switch in the pause menu
> • **Flower loading screens** + a themed title screen, in a clean ASCII/mono style
>
> **Cosmetics**
> • **Flower capes** — redeem a code to unlock. Launch code: `RELEASE2027`
> • Set your **own menu background**
> • **Discord Rich Presence** — show you're on Flower
>
> **Download:** <link to your GitHub release>
> Windows SmartScreen will warn (unsigned) — **More info → Run anyway**.
>
> Found a bug or want a feature? Drop it in the channel.

---

## Patch notes — v1.0.0 (first release)

**Launcher**
- Microsoft account sign-in; stays signed in across restarts
- Automatic Eclipse Temurin **Java 21** download, or point to your own
- Installs the latest stable **Fabric** loader for 1.21.1
- Keeps **Fabric API** + optional **Sodium / Lithium / FerriteCore / Entity Culling** current (SHA-verified); never touches mods you add yourself
- **Modrinth** tab: search, sort, install with dependencies, and remove mods
- **Seasonal ASCII bloom** intro (Rose, Sakura, Sunflower, Marigold, Pumpkin, Frost) with an Auto-by-date option and a picker
- Frosted-glass UI, live game log, memory/resolution/game-folder/JVM settings
- **Auto-update** via GitHub Releases with an in-app banner

**In-game HUD & modules**
- HUD readouts: FPS, CPS (L/R), Keystrokes with live CPS, Coordinates + biome, Armor status, Potion effects, Ping, Direction, Clock, Speed
- **Biome Tint** — HUD accent fades to the current biome's color
- **Zoom** (hold C, scroll to adjust), **Toggle Sprint**
- Drag-and-drop **HUD editor** (move, scale, snap, reset layout)
- **Right Shift** menu to toggle everything

**Look & feel**
- Themed title screen, **Flower loading screens** (replacing the Mojang/world screens)
- **Client-side sky/time** switch in the pause menu (Server / Sunrise / Day / Sunset / Night)
- Glass panels with rounded corners, gradients and soft shadows
- **ASCII / JetBrains-Mono** styling across the in-game UI to match the launcher

**Cosmetics & integrations**
- **Flower capes** — code-unlocked (`RELEASE2027`), visible to other Flower users via a shared list
- **Custom menu background** — use your own image on the title screen
- **Discord Rich Presence** with a "Get Flower" button

**Notes**
- Cosmetic/QoL only — no hacks; safe on servers that allow PvP clients
- Not code-signed yet: Windows SmartScreen shows a warning on first run
