> **Language:** [Русский](README.md) · English

# MrModded2Vanilla

A client-side Minecraft mod designed to deliver a smooth and stable multiplayer experience when playing with client content modpacks on vanilla and third-party servers.

## About the Mod

MrModded2Vanilla allows you to play with your favorite content mods (custom blocks, items, mobs, armor, and decorations) in singleplayer while seamlessly joining standard multiplayer servers (including Vanilla, Paper, Purpur, Spigot, Realms, and servers joined via ViaFabricPlus) without having to disable mods or change launcher profiles.

## The Problem Solved

When you install client-side content mods, they register extra attributes, entities, and blocks. When connecting to standard servers, this could previously lead to issues:
- Game crashes when spawning mobs or other players due to unexpected attribute differences.
- Unexpected disconnections caused by unhandled signals or unrecognized item properties.
- Visual glitches and crashes caused by failing cosmetic models or custom animations.

MrModded2Vanilla automatically protects your multiplayer connection and prevents these issues seamlessly.

## Features

- Entity and player sync protection: prevents crashes when loading mobs and players on multiplayer servers.
- Safe model and animation rendering: keeps the game running smoothly even if custom cosmetic models or 3D animations encounter an issue.
- Smart connection protection: stops redundant custom communication that the server does not expect, preventing accidental anti-cheat kicks.
- Safe item interactions: cleans up incompatible creative-mode item properties before sending them across the network.
- Sound and particle safety: filters malformed sound or particle triggers to prevent unexpected connection drops.
- Clean vanilla blocks: ensures standard blocks display reliably and accurately on multiplayer servers.
- Full ViaFabricPlus compatibility: works stably when playing on servers running different Minecraft versions.
- Clean log notice: prints a single clear notice listing any incompatible mod files that received automatic adjustments.

## Automatic Modes

The mod requires zero manual setup and adjusts its behavior automatically:

- Singleplayer: isolation is disabled, all your mods, blocks, items, and custom features work completely normally.
- Standard multiplayer: isolation activates to provide a clean, crash-free vanilla connection.
- Modded multiplayer: mods supported by the server synchronize naturally.

## Installation

1. Download the version you need (Fabric or NeoForge) from [GitHub Releases](https://github.com/byMr712/Modded2Vanilla-MinecraftMod/releases).
2. Requirements depending on your loader:
   - For Fabric: Fabric Loader and Fabric API.
   - For NeoForge: NeoForge Loader.
3. Place the downloaded `.jar` file into your `mods`.
4. Launch the game.

## Building from Source

To compile the mod, run the following command in your terminal:
```bash
./gradlew build
```
The compiled files for both loaders (Fabric and NeoForge) will be saved in the `build/libs` directory (and automatically synced to `Documents/MrModded2Vanilla_all_builds` if present).

## Automated Compatibility Testing

To verify mod isolation and multiplayer server connectivity without manual launcher steps:
- **Fabric:**
  ```bash
  ./gradlew testFabric
  ```
- **NeoForge:**
  ```bash
  ./gradlew testNeoForge
  ```
- Target a custom server address:
  ```bash
  ./gradlew testFabric -Pserver=example-server.com
  ./gradlew testNeoForge -Pserver=example-server.com
  ```
- **Note for NeoForge:**
  Running `./gradlew testNeoForge` on a third-party machine requires an installed ATLauncher with an instance for the target Minecraft version (named `Minecraft2612withNeoForge` by default), or overriding the ATLauncher path via the `ATLAUNCHER_DIR` environment variable.
- **Testing Custom Mods:**
  Place any third-party mod `.jar` files into the designated folders:
  - For Fabric: in `test_mods/fabric/`
  - For NeoForge: in `test_mods/neoforge/`
  When running `./gradlew testFabric` or `./gradlew testNeoForge`, all `.jar` files in these folders will automatically be loaded and tested against the multiplayer server. All `.jar` files inside `test_mods/` are ignored by Git.
- Run full 10-round progressive test suites:
  - Fabric: `pwsh -File scripts/test_all_fabric_rounds.ps1`
  - NeoForge: `pwsh -File scripts/test_all_neoforge_rounds.ps1`


## Authors and License

- Author: [Mr712](https://github.com/byMr712)
- Source code: [GitHub](https://github.com/byMr712/Modded2Vanilla-MinecraftMod)
- Licensed under [Apache License 2.0](LICENSE)
