# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-09-24

Multi-target release: the repository now carries four independent Gradle
projects, and every one of them was built and verified on a real dedicated
server.

### Added

- **Minecraft 1.20.1 — Fabric** (`fabric/1.20.1`, loader 0.15.11, JDK 17).
  Uses Mojang official mappings, so the two mixins are the *same source files*
  as the 1.20.1 Forge build.
- **Minecraft 26.3 — Fabric** (`fabric/26.3`, loader 0.19.5, loom 1.17, JDK 25).
  Re-derived from the 26.3 bytecode: the damage entry point is now
  `LivingEntity#hurtServer(ServerLevel, DamageSource, float)`, the i-frame field
  is `damageCooldownTime`, and the bypass tag is `BYPASSES_COOLDOWN` — copying
  the 1.20.1 mixin verbatim would silently do nothing.
- **Minecraft 1.12.2 — Forge** (`forge/1.12.2`, ForgeGradle 3, JDK 8).
  This target needs **no Mixin at all**: Forge fires `LivingAttackEvent` as the
  first line of `EntityLivingBase#attackEntityFrom` (before the i-frame check)
  and `AttackEntityEvent` before the cooldown value is read, which is enough to
  remove both mechanics through events alone.
- Repository layout: one directory per target
  (`forge/1.20.1`, `forge/1.12.2`, `fabric/1.20.1`, `fabric/26.3`), each with
  its own Gradle wrapper because the toolchains are mutually incompatible.
- `tools/damage_test.py` now supports a same-tick mode (datapack function),
  RCON connection settings via environment variables, and retries summoning
  until the target chunk is loaded.

### Changed

- All targets share version `0.2.0` and a uniform artifact name:
  `ctdi-<loader>-<mcversion>-<version>.jar`.
- README rewritten for the multi-target layout, including a per-version table of
  where each mechanic actually lives.

### Verified

Each target was built with `./gradlew build` and then loaded on a real
dedicated server (Forge 1.20.1, Fabric 1.20.1, Fabric 26.3 on JDK 25,
Forge 1.12.2 on JDK 8). The i-frame removal was measured with the same-tick
A/B procedure — two identical 5-damage hits inside vanilla's ~10-tick window:

| Target | with CTDI | without CTDI (control) |
|:---|:---|:---|
| 1.20.1 Forge | `19.0f -> 9.0f` (Δ10) | `20.0f -> 15.0f` (Δ5) |
| 1.20.1 Fabric | `20.0f -> 10.0f` (Δ10) | `20.0f -> 15.0f` (Δ5) |
| 26.3 Fabric | `20.0f -> 10.0f` (Δ10) | `20.0f -> 15.0f` (Δ5) |
| 1.12.2 Forge | `20.0f -> 10.0f` (Δ10) | `20.0f -> 15.0f` (Δ5) |

Not verified (unchanged from 0.1.0): the attack-cooldown **feel** in a graphical
client, and client-side visuals. Injection points, refmaps/event wiring and
runtime application are verified for all targets.

## [0.1.0] - 2026-09-24

First public release (Minecraft 1.20.1, Forge 47.2.0, Java 17).

### Added

- **Remove damage i-frames**: Mixin into `LivingEntity#hurt` that zeroes
  `invulnerableTime` before vanilla's i-frame check runs. Every hit enters full
  damage resolution; the vanilla `amount <= lastHurt` damage-drop /
  damage-diff branch is never taken. Nothing else is touched (`hurtTime` and the
  hit animation are left to vanilla).
- **Remove attack cooldown**: Mixin into `Player#getAttackStrengthScale` always
  returning `1.0f`, so every swing deals full base damage, full sharpness /
  enchantment scaling, and keeps crit / knockback / sprint-bonus eligibility.
- Mixin refmap wired through the Sponge mixin gradle plugin + annotation
  processor, so the shipped jar works on production Forge (SRG names).
- Mod metadata: `mods.toml`, `pack.mcmeta`, mod icon, MIT license header.

### Notes

- Target: Minecraft **1.20.1** + **Forge 47.2.0**, Java **17**
  (bytecode level 17 — the jar also runs under Java 21).
- Server-authoritative: mixins run on both sides; a CTDI server accepts
  rapid full-strength hits from vanilla or CTDI clients.
- Untouched by design: weapon/enchantment/potion/crit/attribute damage math,
  knockback, shields, armor, all other vanilla combat rules.
- **Side effect to know about**: the injection sits in the generic `hurt` entry
  point, so environmental damage (fall, lava, suffocation, ...) also loses the
  ~0.5s i-frame window. This follows from "remove i-frames"; a damage-type /
  entity allowlist is the planned way to opt out. `BYPASSES_INVULNERABILITY` in
  1.20.1 only contains `out_of_world` and `generic_kill`, so it is not affected.

### Verified (0.1.0)

- `./gradlew build` succeeds; refmap contains `hurt -> m_6469_` and
  `getAttackStrengthScale -> m_36403_`.
- Both mixins applied at runtime on a real Forge 1.20.1 dedicated server.
- Same-tick A/B on a live server: with CTDI `20.0f -> 10.0f`, control
  (no CTDI) `20.0f -> 15.0f`.
