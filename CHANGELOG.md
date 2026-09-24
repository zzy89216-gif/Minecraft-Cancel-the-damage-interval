# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-09-24

First public release.

### Added

- **Remove damage i-frames** (Minecraft 1.20.1, Forge):
  Mixin into `LivingEntity#hurt` that zeroes `invulnerableTime` (and the
  client-side `hurtTime` flash timer) before vanilla's i-frame check runs.
  Every hit now enters full damage resolution; the vanilla
  `amount <= lastHurt` damage-diff branch is never taken.
- **Remove attack cooldown**:
  Mixin into `Player#getAttackStrengthScale` always returning `1.0f`, so
  every swing deals full base damage, full sharpness/enchantment scaling,
  and keeps crit / knockback / sprint-bonus eligibility.
- Mixin refmap wired through the Sponge mixin gradle plugin + annotation
  processor, so the shipped jar works on production Forge (SRG names).
- Mod metadata: `mods.toml`, pack.mcmeta, mod icon, MIT license header.
- README with verified implementation notes, CHANGELOG, and HANDOVER.

### Notes

- Target: Minecraft **1.20.1** + **Forge 47.2.0**, Java **17**
  (bytecode level 17 — the jar also runs under Java 21).
- Server-authoritative: mixins run on both sides; a CTDI server accepts
  rapid full-strength hits from vanilla or CTDI clients.
- Untouched by design: weapon/enchantment/potion/crit/attribute damage
  math, knockback, shields, armor, all other vanilla combat rules.

### Verified

- `./gradlew build` succeeds; the shipped jar contains a correct refmap
  (`hurt -> m_6469_`, `getAttackStrengthScale -> m_36403_`).
- Both mixins are applied at runtime on a real Forge 1.20.1 dedicated
  server (`Mixing InvulnerabilityMixin ... into LivingEntity`,
  `Mixing AttackCooldownMixin ... into Player`).
- **i-frames removal measured on a live server** (RCON, two identical
  5-damage hits ~1 tick apart on the same zombie, inside vanilla's 10-tick
  window):
  - with CTDI: `20.0f -> 10.0f` (both hits land)
  - without CTDI: `20.0f -> 15.0f` (vanilla drops the second hit)
  - reproducible with `tools/damage_test.py`
- Attack-cooldown behavior is **not** gameplay-tested yet: it needs a
  player swinging in a graphical client. Its injection point, refmap entry
  and runtime application are verified, but the "hand speed = DPS" feel
  still has to be confirmed in-game.

