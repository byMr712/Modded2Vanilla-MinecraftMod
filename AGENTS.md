# Developer & Agent Guidelines — MrModded2Vanilla (1.21.5)

## 1. Project Overview & Identity
- **Mod Name:** MrModded2Vanilla
- **Display Name in Mod Menu:** `[MR] Modded2Vanilla`
- **Target Minecraft Version:** 1.21.5
- **Supported Loaders:** Fabric Loader (`0.19.5+` / `>=0.19.5`) and NeoForge (`>=1.0.0`)
- **Mapping Stack:** Yarn `1.21.5+build.1:v2`
- **Fabric API:** `0.128.2+1.21.5`
- **Java Requirement:** Java 21 LTS
- **Build Output:**
  - Fabric: `MrModded2Vanilla-Fabric-1.21.5-byMr712-v1.0.jar`
  - NeoForge: `MrModded2Vanilla-NeoForge-1.21.5-byMr712-v1.0.jar`
- **Repository:** https://github.com/byMr712/Modded2Vanilla-MinecraftMod
- **Author:** [Mr712](https://github.com/byMr712)
- **License:** Apache-2.0

---

## 2. Core Architecture & Modules

### 2.1 Universal Registry Buffer (`UniversalRegistryBuffer` + `RegistryMixin`)
- Intercepts registration attempts during `Bootstrap.initialize()` when namespace != `"minecraft"`.
- Modded entries are temporarily held in `UniversalRegistryBuffer` and flushed cleanly at `Bootstrap.initialize() -> TAIL`.
- Ensures canonical vanilla block/item states occupy deterministic initial palette indices `0..N`.

### 2.2 Canonical Block State Snapshot (`VanillaBlockStateSnapshot` + `BlockMixin`)
- Captures an immutable snapshot of pure vanilla block states right after bootstrap completion.
- When isolating in multiplayer (`IsolatorState.isIsolating()`), `Block.getStateFromRawId(id)` resolves against the snapshot, preventing ID shifts and mismatch.

### 2.3 Entity Metadata Compensation (`DataTrackerMixin`)
- Resolves discrepancies in entity metadata entries (`DataTracker`) between client and server.
- Automatically handles index shifts (`+1, -1, +2, -2...`) and matches handlers safely, preventing `Invalid entity data item type` crashes.

### 2.4 Mechanics & Interaction Isolation (`VanillaMechanicIsolator`, `LivingEntityMixin`, `PlayerEntityMixin`)
- Clamps player step height to vanilla `0.6F`.
- Normalizes climbing speeds on ladders, vines, and scaffolding.
- Restricts mid-air jumps without physical support.
- Enforces strict vanilla reach distance (survival: 4.5 blocks, creative: 5.0 blocks, entity reach: 3.0 blocks).
- Sanitizes creative item components (`ComponentSanitizer`) before dispatch to vanilla servers.

### 2.5 Safe Feature Rendering (`SafeRenderHelper` + `LivingEntityRendererMixin`)
- Wraps `FeatureRenderer.render()` in try-catch handlers to prevent client crashes from malformed third-party cosmetics or animations.

### 2.6 Adjustment Tracker (`AdjustmentTracker`)
- Detects and records any non-system classes and third-party mod JARs that triggered runtime isolation.
- Emits exactly one formatted notice to the client log upon connecting to a multiplayer server.

### 2.7 Configuration & Packet Pipeline Isolation (ClientConnectionMixin + ClientPlayNetworkHandlerMixin)
- Filters configuration-phase SelectKnownPacksC2SPacket packets to pure vanilla known packs (isVanilla()), eliminating disconnects caused by client-side modpack declarations.
- Safely validates incoming BlockEntityUpdateS2CPacket packets to prevent crashes from null or unsupported block entity types.
- Provides direct in-place component sanitization (ComponentSanitizer.sanitizeInPlace) to strip non-minecraft Data Components.
---

## 3. Version Nuances (Minecraft 1.21.5)
- **Known Packs Pipeline:** Employs `SelectKnownPacksC2SPacket` filtering during the network configuration phase.
- **NetworkState Architecture:** `NetworkState` moved to package `net.minecraft.network.state.NetworkState`.
- **Render State Architecture:** Minecraft 1.21.5 utilizes `LivingEntityRenderState` in `LivingEntityRenderer.render(S, MatrixStack, VertexConsumerProvider, int)`.
- **Movement Flags:** Movement packets (`PlayerMoveC2SPacket`) include the `horizontalCollision` boolean flag.
- **Data Components:** Uses the standard 1.20.5+ Data Component system via `Registries.DATA_COMPONENT_TYPE` and `ItemStack.getComponents()`.
- **DataTracker Architecture:** `DataTracker` uses array-based storage `DataTracker.Entry<?>[] entries` and `DataTracked` interface.

---

## 4. Multi-Loader Porting Architecture: Fabric & NeoForge
Supporting both Fabric and NeoForge loaders from a single codebase without external multi-loader frameworks:
- `src/main/` contains universal mechanics isolator, registry buffer, snapshot, and Mixins.
- `src/neoforge/` contains:
  - `com.mr712.modded2vanilla.MrModded2VanillaNeoForge` entrypoint annotated with `@Mod("modded2vanilla")`.
  - Compile-time `@Mod` annotation stub (`net.neoforged.fml.common.Mod`).
  - Lightweight `net.fabricmc.api.*` and `net.fabricmc.loader.api.*` stubs providing safe fallbacks and querying NeoForge's `ModList` via reflection.
  - `META-INF/neoforge.mods.toml` mod descriptor.
- `jarNeoForge` task:
  - Bundles compiled classes from `sourceSets.main` and `sourceSets.neoforge`.
  - Excludes Fabric-only descriptors (`fabric.mod.json`) and the `@Mod` compile stub (`net/neoforged/**`).
  - Emits `MrModded2Vanilla-NeoForge-<mc_version>-byMr712-v<mod_version>.jar`.
- `remapJar` produces the pure Fabric JAR `MrModded2Vanilla-Fabric-<mc_version>-byMr712-v<mod_version>.jar`.
- Post-build tasks automatically sync both artifacts to `C:\Users\Mr712\Documents\MrModded2Vanilla_all_builds\`.

---

## 5. Build Instructions
- Prerequisites: Java 21 or Java 25 JDK installed.
- Compile and build:
  ```bash
  ./gradlew build
  ```
- Resulting jars:
  - `build/libs/MrModded2Vanilla-Fabric-1.21.5-byMr712-v1.0.jar`
  - `build/libs/MrModded2Vanilla-NeoForge-1.21.5-byMr712-v1.0.jar`.