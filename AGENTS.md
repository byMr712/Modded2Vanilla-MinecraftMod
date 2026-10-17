# Developer & Agent Guidelines — MrModded2Vanilla (26.1.2)

## 1. Project Overview & Identity
- **Mod Name:** MrModded2Vanilla
- **Display Name in Mod Menu:** `[MR] Modded2Vanilla`
- **Target Minecraft Version:** 26.1.2
- **Supported Loaders:** Fabric Loader (`0.19.5+` / `>=0.19.5`) and NeoForge (`>=1.0.0`)
- **Mapping Stack:** Mojang Mappings (Official) via local empty mappings
- **Fabric API:** `0.155.3+26.1.2`
- **Mod Menu:** `18.0.2`
- **Java Requirement:** Java 25 LTS
- **Build Output:**
  - Fabric: `MrModded2Vanilla-Fabric-26.1.2-byMr712-v1.0.jar`
  - NeoForge: `MrModded2Vanilla-NeoForge-26.1.2-byMr712-v1.0.jar`
- **Repository:** https://github.com/byMr712/Modded2Vanilla-MinecraftMod
- **Author:** [Mr712](https://github.com/byMr712)
- **License:** Apache-2.0

---

## 2. Core Architecture & Modules

### 2.1 Universal Registry Buffer (`UniversalRegistryBuffer` + `RegistryMixin`)
- Intercepts registration attempts during `Bootstrap.bootStrap()` when namespace != `"minecraft"`.
- Modded entries are temporarily held in `UniversalRegistryBuffer` and flushed cleanly at `Bootstrap.bootStrap() -> TAIL`.
- Ensures canonical vanilla block/item states occupy deterministic initial palette indices `0..N`.

### 2.2 Canonical Block State Snapshot (`VanillaBlockStateSnapshot` + `BlockMixin`)
- Captures an immutable snapshot of pure vanilla block states right after bootstrap completion via `Block.BLOCK_STATE_REGISTRY`.
- When isolating in multiplayer (`IsolatorState.isIsolating()`), `Block.stateById(id)` resolves against the snapshot, preventing ID shifts and mismatch.

### 2.3 Entity Metadata Compensation (`DataTrackerMixin`)
- Resolves discrepancies in entity metadata entries (`SynchedEntityData`) between client and server.
- Automatically handles index shifts (`+1, -1, +2, -2...`) and matches serializers safely, preventing `Invalid entity data item type` crashes.
- Shadows `entity` (`SyncedDataHolder`) and `itemsById` (`DataItem<?>[]`).

### 2.4 Mechanics & Interaction Isolation (`VanillaMechanicIsolator`, `LivingEntityMixin`, `PlayerEntityMixin`)
- Clamps player step height via `LivingEntity.maxUpStep` to vanilla `0.6F`.
- Normalizes climbing speeds on ladders, vines, and scaffolding via `LivingEntity.handleOnClimbable(Vec3)`.
- Restricts mid-air jumps without physical support via `LivingEntity.jumpFromGround()`.
- Enforces strict vanilla reach distance (survival: 4.5 blocks, creative: 5.0 blocks, entity reach: 3.0 blocks) via `Player.blockInteractionRange()` and `Player.entityInteractionRange()`.
- Guards ledge-clipping invariance via `Player.isStayingOnGroundSurface()`.
- Sanitizes creative item components (`ComponentSanitizer`) in `MultiPlayerGameMode.handleCreativeModeItemAdd` before dispatch to vanilla servers.

### 2.5 Safe Feature Rendering (`SafeRenderHelper` + `LivingEntityRendererMixin`)
- Intercepts `RenderLayer.submit(PoseStack, SubmitNodeCollector, int, S, float, float)` in `LivingEntityRenderer.submit` with try-catch handlers to prevent client crashes from malformed third-party cosmetics or animations.

### 2.6 Adjustment Tracker (`AdjustmentTracker`)
- Detects and records any non-system classes and third-party mod JARs that triggered runtime isolation.
- Emits exactly one formatted notice to the client log upon connecting to a multiplayer server.

### 2.7 Configuration & Packet Pipeline Isolation (`ClientConnectionMixin` + `ClientPlayNetworkHandlerMixin`)
- Filters configuration-phase `ServerboundSelectKnownPacks` packets to pure vanilla known packs (`isVanilla()`), eliminating disconnects caused by client-side modpack declarations.
- Safely validates incoming `ClientboundBlockEntityDataPacket` packets to prevent crashes from null or unsupported block entity types.
- Provides direct in-place component sanitization (`ComponentSanitizer.sanitizeInPlace`) to strip non-minecraft Data Components.

---

## 3. Version Nuances (Minecraft 26.1.2)
- **Official Mojang Mappings:** Uses official Mojang namespace directly without Yarn intermediaries (`net.minecraft.world.level.block.Block`, `net.minecraft.network.syncher.SynchedEntityData`, `net.minecraft.client.player.LocalPlayer`).
- **Java 25 Requirement:** Minecraft 26.x requires Java 25 LTS compiler and runtime.
- **Identifier Naming:** Mojang mappings in 26.x use `net.minecraft.resources.Identifier` directly.
- **Bootstrap Initialization:** Uses `Bootstrap.bootStrap()` with a capital S.
- **SynchedEntityData Storage:** Entity tracker uses `itemsById` array and `DataValue<?>` records instead of legacy entries.
- **Network Pipeline:** Uses `Connection.send(Packet, ChannelFutureListener, boolean)` and `PacketDecoder.decode`. Custom payload uses `ServerboundCustomPayloadPacket` with `payload().type().id()`.
- **Render State Pipeline:** Uses `LivingEntityRenderer.submit(S, PoseStack, SubmitNodeCollector, CameraRenderState)` redirecting `RenderLayer.submit`.
- **Known Packs Pipeline:** Employs `ServerboundSelectKnownPacks` filtering during the network configuration phase.

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
- Prerequisites: Java 25 JDK installed.
- Compile and build:
  ```bash
  ./gradlew build
  ```
- Resulting jars:
  - `build/libs/MrModded2Vanilla-Fabric-26.1.2-byMr712-v1.0.jar`
  - `build/libs/MrModded2Vanilla-NeoForge-26.1.2-byMr712-v1.0.jar`.
---

## 6. Automated Testing (Multiplayer Verification)
Each branch includes automated client tests verifying handshake, palette snapshot, DataTracker isolation, and mixin stability against vanilla servers:
- **Fabric test:**
  ```bash
  ./gradlew testFabric
  # Target a custom server:
  ./gradlew testFabric -Pserver=example-server.com
  ```
- **NeoForge test:**
  ```bash
  ./gradlew testNeoForge
  # Target a custom server:
  ./gradlew testNeoForge -Pserver=example-server.com
  ```
  *Note:* Requires ATLauncher with a `Minecraft2612withNeoForge` instance, or `ATLAUNCHER_DIR` environment variable.
- **Full 10-round progressive test suites:**
  - Fabric: `pwsh -File scripts/test_all_fabric_rounds.ps1`
  - NeoForge: `pwsh -File scripts/test_all_neoforge_rounds.ps1`
- **Custom Mod Testing:**
  - Place third-party mod `.jar` files in `test_mods/fabric/` or `test_mods/neoforge/`.
  - Jars are automatically copied to test instances and tested against vanilla servers.
  - All `.jar` files in `test_mods/` are git-ignored.

