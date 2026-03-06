# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**LeNeMon** is a custom Fabric mod (mod ID: `lenemon`) for a Cobblemon/Pokémon Minecraft server running on MC 1.21.1. It is a server-specific mod with many gameplay systems built on top of Fabric API, Cobblemon, GeckoLib, and Impactor Economy.

Key dependencies (from `build.gradle`):
- Minecraft 1.21.1 with Fabric Loader 0.18.4
- Fabric API 0.116.8+1.21.1
- GeckoLib 4.8.3 (for animated armor/item rendering)
- Cobblemon 1.7.3+1.21.1 (from local `libs/` folder)
- Impactor 5.3.5+1.21.1 (economy, from local `libs/` folder)
- fabric-permissions-api 0.3.1 (via LuckPerms)
- HoloDisplays 0.4.8 (holograms)

## Build Commands

```bash
# Build the mod (outputs lenemon-1.0.0.jar + SHA256 in build/libs/)
./gradlew build

# Generate SHA256 only (requires prior build)
./gradlew generateSha256

# Run Minecraft client for testing
./gradlew runClient

# Run Minecraft server for testing
./gradlew runServer
```

The `build` task automatically produces `build/libs/lenemon-1.0.0.jar` (renamed from the standard remapped jar via the `fixedJar` task).

## Source Structure

The project uses Fabric Loom's split environment source sets:

- `src/main/java/com/lenemon/` — server-side and common code
- `src/client/java/com/lenemon/` — client-only code
- `src/main/resources/` — mod resources (fabric.mod.json, mixins, assets, data)

### Key Package Map

| Package | Purpose |
|---|---|
| `com.lenemon` | Entry points: `Lenemon` (server) and `LenemonClient` (client) |
| `com.lenemon.registry` | `ModItems`, `ModBlocks`, `ModBlockEntities` — all item/block registration |
| `com.lenemon.command` | All server commands (`LenemonCommand`, `HunterCommand`, `VoteCommand`, etc.) |
| `com.lenemon.network` | Fabric networking payloads (`PacketHudBalance`, `PacketHudHunter`, `LenemonNetwork`) |
| `com.lenemon.casino` | Casino slot machine system (world data, spin handler, holograms, network payloads) |
| `com.lenemon.casino.screen` | Casino screen handler and opener |
| `com.lenemon.armor` | Armor effect system — config loader, effect registry, set bonuses (XP, shiny, particles) |
| `com.lenemon.armor.sets` | Per-armor-set definitions (`DevArmorSet`, `RayArmorSet`) |
| `com.lenemon.hunter` | Hunter quest system (quests, levels, rewards, world data) |
| `com.lenemon.shop` | Shop system (categories, items, config, screen, sell service) |
| `com.lenemon.gift` | Gift chest system (config, data, item helper, lottery, sessions) |
| `com.lenemon.economy` | `EconomyService` interface + `ImpactorEconomyService` implementation |
| `com.lenemon.discord` | Discord webhook integration for chat relay |
| `com.lenemon.vote` | Vote reward system |
| `com.lenemon.fly` | Fly timer/session management |
| `com.lenemon.heal` | Heal paper item helper |
| `com.lenemon.pickaxe` | Excaveon pickaxe (GeckoLib animated, config-driven mining modes) |
| `com.lenemon.enchantment` | Custom enchantments (`AutoSmeltEnchantment`) |
| `com.lenemon.block` | Custom blocks (`ElevatorBlock`, `CasinoBlock`, event handlers) |
| `com.lenemon.mixin` | Mixins: `ItemStackMixin`, `PCBlockMixin`, `PlayerInteractBlockMixin`, `GiftChestBlockMixin`, `GiftChestPlaceMixin` |
| `com.lenemon.client.*` | Client renderers (GeckoLib armor/item), HUD renderer, network client, custom title screen |
| `com.lenemon.client.guieditor` | In-dev GUI editor (disabled by default in `LenemonClient`) |
| `com.lenemon.compat` | `LuckPermsCompat` for permission checks |

## Architecture Patterns

### Config System
Most systems load JSON configs at server start via `FabricLoader.getInstance().getConfigDir().resolve("lenemon/...")`. Config classes (e.g., `ArmorSetConfig`, `ShopConfig`, `ExcaveonConfig`) are loaded with GSON and auto-migrate missing fields. Configs live server-side under `config/lenemon/`.

### Economy
All monetary operations go through the `EconomyService` interface (`com.lenemon.economy`). The live implementation is `ImpactorEconomyService` backed by Impactor. Use `EconomyHelper` utility where possible.

### Armor Effects
Armor effects are data-driven: `ArmorConfigLoader` reads per-armor JSON configs. `ArmorEffectRegistry` maps armor piece → effects. `ArmorEffectHandler` applies server-side effects (potion effects, XP boosts, shiny rate); `ParticleArmorEffect` applies client-side particle effects via `LenemonNetworkClient`.

### Casino
`CasinoWorldData` (persistent world state) stores machine configs per block position. `CasinoSpinHandler` processes spin outcomes using Cobblemon Pokémon as prizes. Network payloads under `com.lenemon.casino.network` sync spin state between server and client.

### Hunter / Quest System
`QuestConfigLoader` reads quest configs from disk. `HunterWorldData` persists per-player hunter progress. `HunterManager` registers the server-tick events that drive quest tracking.

### Networking
Custom payloads implement `CustomPayload` and are registered in `LenemonNetwork.register()`. Client-side receivers are registered in `LenemonNetworkClient.register()`.

### Permissions
Use `Permissions.check(source, "lenemon.<node>", fallbackLevel)` from `fabric-permissions-api` / LuckPerms. Permission nodes follow the `lenemon.*` namespace.

### GeckoLib Integration
Animated items and armor use GeckoLib 4. Renderers (`DevArmorRenderer`, `RayArmorRenderer`, `ExcaveonRenderer`) are registered in `LenemonClient`. Geo model JSON files live in `src/main/resources/assets/lenemon/geo/`. Animation JSON files live in `src/main/resources/assets/lenemon/animations/`.

## Local Library JARs
Cobblemon and Impactor are not on a public maven; they are provided as local JARs in `libs/`. If these are missing, the project will not compile.
