# Flower Client — v1.1.0 Discord announcement & patch notes

---

## Discord update message (copy–paste)

> # Flower Client v1.1.0 — Version Selector
>
> You can now pick your **Minecraft version** right from the launcher. Choose a version, press Play, and Flower sets up the matching **Fabric** loader and performance mods for you — no manual installs.
>
> **New**
> • **Minecraft version selector** on the home screen — **1.16.5 through 1.21.4**
> • Per-version **Fabric** install plus the **Sodium / Lithium / FerriteCore** performance stack, handled automatically
> • The built-in **Modrinth** browser now follows your selected version, so every mod you install matches
> • Your version is remembered between launches
>
> **Good to know**
> • The full **Flower HUD** runs on **1.21.x** right now. On other versions Flower still launches with Fabric and the performance mods, and more HUD versions are on the way.
>
> **Download:** <link to your GitHub release>
> Already on Flower? The launcher **auto-updates** — you'll see a banner.
>
> Bugs or requests? Drop them in the channel.

---

## Patch notes — v1.1.0

**Added**
- **Minecraft version selector** in the launcher home screen — a dropdown of supported versions, newest first
- Supported versions: 1.21.4, 1.21.3, 1.21.1, 1.21, 1.20.6, 1.20.4, 1.20.1, 1.19.4, 1.19.2, 1.18.2, 1.17.1, 1.16.5
- Each version installs its own **Fabric** loader plus **Sodium**, **Lithium** and **FerriteCore**
- Per-version tags in the menu (Recommended / Flower HUD / Perf only) and an inline note when a performance-only version is selected
- Selected version is saved to settings and validated on load

**Changed**
- **Modrinth** browser now searches and installs for the selected version
- The whole launch pipeline (Fabric, mods, game) resolves the chosen version end to end

**Safety**
- The Flower HUD jar is only installed on versions it is built for (1.21.x), so choosing another version can never crash the game; a HUD jar left over from a previous version is removed automatically on the next launch
