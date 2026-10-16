# Optional booster reward integrations

This guide covers the **six available automatic booster integrations** in CobblemonCards 2.1.0 for **Minecraft 1.21.1 and Cobblemon 1.8.2**, on Fabric and NeoForge. Addons are optional dependencies: CobblemonCards continues to work when they are absent. Versions and dependencies were checked on 16 October 2026. Startup checks and automated tests are recorded below; player-driven reward mechanics and multiplayer gameplay have not been tested. Parting Gifts is unavailable on this Cobblemon version and is covered separately under known incompatibilities.

## Available integrations

| Addon | Checked version | Loaders | Mod ID | Reward mechanism | Validation status |
| --- | --- | --- | --- | --- | --- |
| Cobblemon Rustling Spots | 5.0.1 | Fabric, NeoForge | `rustlingspots` | Public server interaction callback | Implemented as an additional interaction reward; gameplay validation pending |
| Cobblemon Raid Dens | 0.13.2+1.21.1 | Fabric, NeoForge | `cobblemonraiddens` | Seven separate Raid Pouch loot tables | Implemented; gameplay validation pending |
| CobbledGacha v.3 | 3.0.3 | Fabric, NeoForge | `cobbledgacha` | Five Minecraft capsule reward loot tables | Implemented; gameplay validation pending |
| Cobblemon Fishing Loot | 1.0.0 | Fabric, NeoForge | `cobblemonfishingloot` | Root Poké Rod fishing loot table | Implemented; gameplay validation pending |
| Rad Gyms | 0.5.0 | Fabric, NeoForge | `rad_gyms` | Shared gym reward table; public active-gym lookup | Implemented in 2.1; gameplay validation pending |
| Radical Cobblemon Trainers | 0.19.2-beta | Fabric, NeoForge | `rctmod` | Trainer victory loot, with public root-table lookup to exclude nested rewards | Implemented in 2.1; gameplay validation pending |

Only the six addons listed above currently receive automatic booster rewards on the supported setup. This list describes implemented integrations, not certification of every gameplay or multiplayer scenario.

## Server configuration

Use the **Compatibility** tab in the existing MidnightLib configuration screen, or edit `config/cobblemon-cards.json` with the server stopped and restart it. Gameplay uses the server's values; a client-only change does not alter rewards. Integration settings default to enabled and automatically activate when a compatible addon is detected. Six addons can currently be used with the 1.8.2 setup; the Parting Gifts setting is retained for a future compatible release and cannot fix that addon's startup crash. The following keys are flat entries in the existing file, not a nested `compatibility` object or a separate configuration file:

```json
{
  "rustlingSpotsEnabled": true,
  "rustlingSpotsBoosterChance": 10.0,
  "rustlingSpotsFamilyBoosters": [
    "GRASS=grass", "WATER=water", "SNOW=ice", "CAVE=rock",
    "SAND=ground", "NETHERFLAMME=fire", "SOULFLAME=ghost"
  ],
  "raidDensEnabled": true,
  "raidDensTier1Chance": 5.0,
  "raidDensTier2Chance": 8.0,
  "raidDensTier3Chance": 15.0,
  "raidDensTier4Chance": 20.0,
  "raidDensTier5Chance": 30.0,
  "raidDensTier6Chance": 40.0,
  "raidDensTier7Chance": 50.0,
  "cobbledGachaEnabled": true,
  "cobbledGachaPokeChance": 1.0,
  "cobbledGachaGreatChance": 2.0,
  "cobbledGachaUltraChance": 3.0,
  "cobbledGachaMasterChance": 4.0,
  "cobbledGachaCherishChance": 5.0,
  "fishingLootEnabled": true,
  "fishingLootWaterChance": 3.0,
  "fishingLootClassicChance": 1.0,
  "radGymsEnabled": true,
  "radGymsBoosterChance": 10.0,
  "rctEnabled": true,
  "rctBoosterChance": 5.0
}
```

Merge these fields into the existing file and keep other settings. Every chance is a percentage from 0 to 100: 0 disables that roll and 100 always succeeds when an eligible booster exists. Reward calculation clamps finite out-of-range values; non-finite values give no reward. Setting an integration's enabled flag to `false` disables all of its added rewards without editing addon data.

Rustling mappings use `FAMILY=selector`, case-insensitively. Selectors are `classic` (also `base`), `generation`, `themed`, `gen1` through `gen9`, or any existing type name such as `fire`, `water` or `ghost`. For example, `"FLYING=flying"` adds a themed mapping for flying spots. Unmapped families and unrecognized selectors fall back to classic. The first matching family entry wins. No full item ID is needed in these mappings.

## Verified loot integration behavior

The loot integrations add separate booster rolls without replacing the addon's existing reward entries. Each requires its detected addon and its own enabled setting on the server. They use the existing classic, generation and type-themed booster items; there are no new booster definitions or invented rare/special booster categories.

Fabric appends pools through its loot modification API. NeoForge uses a Global Loot Modifier restricted to the verified tables below, including RCT's documented trainer table path with an actual-root check. Shared reward selection checks registered booster items. The loader hooks do not statically link optional addon classes; RCT and Rad Gyms resolve their verified public context APIs only after detection. No optional compile dependency, client reward packet or separate claims/economy system is added. Selection is uniform across eligible individual packs, not across category names: Great capsules can select one classic or nine generation packs, and Ultra capsules can select nine generation or eighteen type-themed packs. Missing/non-booster registry entries are skipped; an empty eligible selection gives no reward.

### Cobblemon Raid Dens

Open a Raid Pouch received through Raid Dens' completed-raid rewards. The extra roll belongs to the pouch's default tier table, so the addon retains ownership of completion, participant rewards, pouch consumption and item delivery. There is no extra raid-completion listener.

| Tier | Exact table ID | Default chance | Booster selection |
| --- | --- | --- | --- |
| 1 | `cobblemonraiddens:raid/tier/tier_one` | 5% | Classic |
| 2 | `cobblemonraiddens:raid/tier/tier_two` | 8% | Classic |
| 3 | `cobblemonraiddens:raid/tier/tier_three` | 15% | Generation |
| 4 | `cobblemonraiddens:raid/tier/tier_four` | 20% | Generation |
| 5 | `cobblemonraiddens:raid/tier/tier_five` | 30% | Type-themed |
| 6 | `cobblemonraiddens:raid/tier/tier_six` | 40% | Type-themed |
| 7 | `cobblemonraiddens:raid/tier/tier_seven` | 50% | Type-themed |

All seven tiers exist in the investigated release. The base addon supplies raid bosses for tiers 1-5; higher tiers depend on additional boss content. A boss-specific reward table with `replace: true` bypasses the default tier table and therefore bypasses this integration. Custom boss tables are not patched by guessing their IDs. Creative/admin-created pouches or explicit administrative loot commands remain administrative actions, not proof of a completed raid.

Evidence: [official repository and raid reward documentation](https://github.com/necro50n3/cobblemon-raiddens), [tier definitions](https://github.com/necro50n3/cobblemon-raiddens/blob/master/common/src/main/java/com/necro/raid/dens/common/data/raid/RaidTier.java), [pouch reward implementation](https://github.com/necro50n3/cobblemon-raiddens/blob/master/common/src/main/java/com/necro/raid/dens/common/items/item/RaidPouchItem.java). License: MIT.

### CobbledGacha v.3

Opening one of the five ordinary Poké Gacha capsules rolls its existing loot and a low-probability booster reward. Machine-dispensing loot, machine block drops, placeholder capsules and Pokémon-spawning balls are not targets.

| Capsule | Exact table ID | Default chance | Booster selection |
| --- | --- | --- | --- |
| Poké | `cobbledgacha:gacha_capsules/capsule_a1` | 1% | Classic |
| Great | `cobbledgacha:gacha_capsules/capsule_a2` | 2% | Classic or generation |
| Ultra | `cobbledgacha:gacha_capsules/capsule_a3` | 3% | Generation or type-themed |
| Master | `cobbledgacha:gacha_capsules/capsule_a4` | 4% | Type-themed |
| Cherish | `cobbledgacha:gacha_capsules/capsule_a5` | 5% | Type-themed |

These names and IDs were checked against both official 3.0.3 release jars. Capsule opening generates and delivers loot on the server; the addon consumes capsules in survival. Master and Cherish reward existing packs and do not guarantee rare cards, God Packs or God Pack Tickets.

Evidence: [official addon and datapacking guidance](https://modrinth.com/mod/cobbledgacha), [author's loot tables/datapacking guide](https://docs.google.com/spreadsheets/d/1AKjqLy6S8leh_qAg_ATlUcj3T7uENHeOPuBYw7NitX8/edit?usp=sharing), [Fabric 3.0.3](https://www.curseforge.com/minecraft/mc-mods/cobbledgacha/files/7787788), [NeoForge 3.0.3](https://www.curseforge.com/minecraft/mc-mods/cobbledgacha/files/7787787). License: All Rights Reserved. The jar's source link, `hisRoyalty/gachamachine`, was unavailable; the public author guide and actual release artifacts establish the integration. No addon files or sprites are redistributed.

Known upstream startup limitation: the NeoForge smoke run without PokeBlocks logged eleven parse errors for CobbledGacha's Plush-O-Matic `cobbledgacha:gacha_machine_12_*` yarn reward tables, which reference absent PokeBlocks registry items. These are separate from the five capsule tables targeted here. The server still reached `Done`; no capsule, raid, fishing or compatibility modifier parsing errors were observed. This integration does not alter those machine tables or add PokeBlocks as a dependency.

### Cobblemon Fishing Loot

The exact root table is `cobblemon:fishing/pokerod`. With Fishing Loot installed, a Poké Rod item catch gets independent chances of **3% for a Water booster** and **1% for a classic booster**, while keeping the normal catch. Both packs can occur together: the default combined chance is 0.03%, and the chance of at least one pack is 3.97% per eligible item-catch roll.

The integration does not patch vanilla rod fishing, treasure sub-tables, block loot or chest loot. Cobblemon owns the server-side catch validation; its Poké Rod item branch rolls this root table. Pokémon catches, misses and attempts that never reach the legitimate item-catch branch do not get this reward. Fishing Loot's existing open-water restriction on rare treasure remains attached to rare treasure; the added boosters are independent of that restriction.

Evidence: [official Fishing Loot release](https://www.curseforge.com/minecraft/mc-mods/cobblemon-fishing-loot), [author documentation](https://docs.notsupport.com/cobblemonfishingloot/), [Cobblemon Poké Rod catch implementation](https://gitlab.com/cable-mc/cobblemon/-/blob/main/common/src/main/kotlin/com/cobblemon/mod/common/entity/fishing/PokeRodFishingBobberEntity.kt). Published license metadata is inconsistent: CurseForge lists Apache 2.0, Fabric metadata lists MIT, and the bundled license is CC0. This integration redistributes none of the addon's resources or code.

### Cobblemon Rustling Spots

The investigated 5.0.1 release exposes `RustlingSpotAddonApi.registerInteractionCallback`. CobblemonCards registers one callback only after detecting the addon, using its verified public API through a checked reflective adapter without loading missing addon classes or adding a compile dependency. Earlier releases without that API, or incompatible signatures, log that this integration is unavailable and preserve normal gameplay. The default chance is **10% per valid consumed server-side interaction**. The callback returns `PASS`, keeping the addon's normal reward processing.

| Actual family key | Default booster |
| --- | --- |
| `grass` | Grass |
| `water` | Water |
| `snow` | Ice |
| `cave` | Rock |
| `sand` | Ground |
| `netherflamme` | Fire |
| `soulflame` | Ghost |
| `leaves`, `flying`, unmapped families | Classic |

`netherflamme` is the addon's actual spelling. The callback runs before normal reward resolution, so an originally empty spot can still give a booster. A later addon callback can handle or empty the spot; if an earlier callback already handled it, CobblemonCards is not called. Every validated outcome consumes the spot. A weak identity guard avoids repeating the integration roll for the same retained live spot if normal resolution throws. Spots are temporary in-memory objects rather than persistent claim records. This is an additional spot-interaction reward, not a post-resolution successful-loot event.

Evidence: [official source](https://github.com/LevelsFR/RustlingSpots-MultiLoader-1.21.1), [official project](https://www.curseforge.com/minecraft/mc-mods/rustling-spots). License: MPL 2.0. This release requires Cobblemon 1.8.0 or later.

## Additions in 2.1.0

### Dependencies of optional addons

Installing an addon also requires its own dependencies. These do not become hard dependencies of CobblemonCards.

- CobbledGacha 3.0.3 and Raid Dens require GeckoLib. The 1.8.2 test setup uses GeckoLib **4.9.3 for Minecraft 1.21.1**: [Fabric](https://modrinth.com/mod/geckolib/version/tmxM7U4W), [NeoForge](https://modrinth.com/mod/geckolib/version/Grwn5rUB). Missing `software/bernie/geckolib/animatable/GeoItem` or `GeoBlockEntity` prevents the addon itself from starting, even if booster integration is disabled. GeckoLib 4.9.3 declares `geckoanimfix` incompatible.
- Rad Gyms 0.5.0 was released for Cobblemon **1.8.1 or later**. It needs RCT API, Architectury, and Fabric Language Kotlin on Fabric. It does not require RCT Mod.
- RCT Mod 0.19.2-beta requires RCT API and, on Fabric, **Forge Config API Port**. The current test setup uses [21.1.6 Fabric](https://modrinth.com/mod/forge-config-api-port/version/N5qzq0XV); NeoForge supplies the configuration API natively.
- For Cobblemon **1.8.2**, use **RCT API 0.16.2-beta**: [Fabric](https://modrinth.com/mod/rctapi/version/DoQNDXAU), [NeoForge](https://modrinth.com/mod/rctapi/version/mYGAO3MT). Its changelog fixes crashes involving items changed in 1.8.2, including Iron Ball. Replace the older 0.16.1-beta used by the earlier 1.8.1 verification setup. Do not keep both API jars installed. CobblemonCards' dependency metadata rejects older RCT API versions when present; it does not require the API when no addon needs it.
- Main dependencies remain at the versions in `gradle.properties` and the README. Their official metadata accepts the current Minecraft/loaders; the GraalJS 22.3.0, ICU 71.1 and MongoDB 4.10.2 versions bundled by Cobblemon did not change. Fabric Loader **0.19.5** and NeoForge **21.1.255** also satisfy Fishing Loot's newer minimum loader requirements.

The versions in the available integrations table passed the startup checks described below. A permissive Cobblemon dependency range alone does not prove binary compatibility, as the Parting Gifts incompatibility demonstrates.

### Rad Gyms 0.5.0

The exact patched table is **`rad_gyms:gyms/default/shared_loot_table`**. The shipped gym definitions call it once alongside one level-dependent table after the leader is beaten. A **10% booster** roll is added to that shared table, so common/uncommon/rare/epic tables cannot each grant another booster. Entrance archaeology and block-drop tables are untouched.

The verified public `RadGymsState.getGymForPlayer(ServerPlayer)` and `Gym.getType()` methods select an existing booster matching the active gym type. Unknown types or a reward generated without an active gym use classic boosters. If the API becomes incompatible, the integration logs the problem and skips its booster roll. Rewards enter the native gym reward bag; opening that already-filled bag does not generate loot again. Datapacks redirecting rewards away from the shared table get no automatic booster; native `maxItems` caps and bundle capacity still apply.

Configuration: `radGymsEnabled` and `radGymsBoosterChance`. The public lookup is resolved only when `rad_gyms` is detected. No direct NPC battle-completion hook is added.

Evidence: [official source](https://github.com/gitoido-mc/rad-gyms), [reward handler](https://github.com/gitoido-mc/rad-gyms/blob/main/x-common/src/main/kotlin/lol/gito/radgyms/common/event/gyms/GenerateRewardHandler.kt), [gym reward definitions](https://github.com/gitoido-mc/rad-gyms/tree/main/x-common/src/generated/data/rad_gyms/gyms), [Fabric 0.5.0](https://modrinth.com/mod/rad-gyms/version/jNLB4nS8), [NeoForge 0.5.0](https://modrinth.com/mod/rad-gyms/version/GczZp4sE). License: GPL-3.0-only. No addon files are copied or redistributed.

### Radical Cobblemon Trainers 0.19.2-beta

RCT derives victory reward IDs under **`rctmod:trainers/`** from trainer definitions (default, group or single-trainer tables). These tables receive a **5% classic-or-generation booster** roll. Each individual eligible pack has equal selection weight. The actual public `RCTMod.getInstance().getTrainerManager().getData(TrainerMob).getLootTableResource()` lookup identifies the current trainer's root table. Only an exact match can grant a booster. This matters because the shipped single-trainer tables often nest group tables: simply patching every trainer table would otherwise duplicate rewards on Fabric.

The loot context must contain an actual RCT `TrainerMob` and a server player in `LAST_DAMAGE_PLAYER`. Generic item-category tables and ordinary entity death tables are excluded. RCT keeps control of victory eligibility, trainer cooldowns, multiplayer reward choice and ground-drop delivery. There is no per-player extra victory listener or cross-battle reward suppression. Two later legitimate victories still get independent rolls. Custom trainer tables under the verified namespace/path are supported; redirected tables in other namespaces are left to the datapack.

Configuration: `rctEnabled` and `rctBoosterChance`. The reflection adapter resolves only verified public API methods after detecting `rctmod`; missing/incompatible APIs skip booster rewards without preventing startup. Rad Gyms uses Cobblemon NPCs and its own reward table, so RCT API being present does not activate RCT Mod rewards or duplicate gym rewards.

Evidence: [official source, Minecraft 1.21.1](https://gitlab.com/srcmc/rct/mod/-/tree/1.21.1), [native trainer victory loot](https://gitlab.com/srcmc/rct/mod/-/blob/1.21.1/common/src/main/java/com/gitlab/srcmc/rctmod/world/entities/TrainerMob.java), [loot table documentation](https://srcmc.gitlab.io/rct/docs/latest/configuration/data_pack/loot_tables/), [Fabric 0.19.2-beta](https://modrinth.com/mod/rctmod/version/rgfBfnW9), [NeoForge 0.19.2-beta](https://modrinth.com/mod/rctmod/version/ITPewVPq). Metadata declares GNU-LGPL-3, while the inspected source headers describe the Mod Community License; no RCT source or assets are copied into CobblemonCards.

## Known incompatibility: Parting Gifts

**Parting Gifts 3.0.2 and 3.0.3 cannot be used with Cobblemon 1.8.2.** Both loaders fail during the addon's `ReleaseUtils` initialization with `NoSuchMethodError: ElementalType.<init>` because those releases call removed constructors. Remove the addon JAR until an upstream fix is available. CobblemonCards' metadata rejects these known broken versions before initialization.

The integration code and the `partingGiftsEnabled` / `partingGiftsBoosterChance` settings are retained, but they are not an available reward source on the current setup. Disabling the integration cannot fix the addon's own startup failure. No compatible official release or fix was found during the 16 October verification.

Evidence: [official source](https://github.com/anabsolutesloth/cobblemonreleaserewards), [release utility implementation](https://github.com/anabsolutesloth/cobblemonreleaserewards/blob/main/common/src/main/java/com/emperdog/releaserewards/loot/ReleaseUtils.java), [Fabric 3.0.3](https://modrinth.com/mod/cobblemon-release-rewards/version/LhIHf2Xu), [NeoForge 3.0.3](https://modrinth.com/mod/cobblemon-release-rewards/version/PXsx5x0U).

## Datapacks, authority and balance

Existing entries and reward delivery stay with the addon. Additive hooks target only the exact IDs listed above. A server datapack can replace a targeted table's original contents; the extra roll still applies when that exact table is queried as the reward root and the integration is enabled. A replacing raid boss table, a redirected reward ID or a custom capsule outside the verified five IDs is respected and receives no guessed integration. Disable the relevant integration when a pack requires complete control of booster rewards.

There is a loader difference for custom wrapper tables: Fabric's appended pool also runs when the targeted table is referenced as a child of another table. NeoForge's Global Loot Modifier runs for the queried root table, so a custom wrapper using a different root ID receives no automatic booster even if it references a targeted child. Both implementations cover the verified addons' normal reward roots. Prefer those roots when authoring overrides intended to behave the same on both loaders.

Every legitimate targeted loot roll is independent. Reopening a GUI is not a reward trigger, and rewards are not suppressed merely because the same item was rolled before. Repeated administrative loot generation, creative access and deliberately repeated table references remain controlled by the administrator/datapack. This integration does not certify upstream addon claim persistence or third-party callback behavior without runtime testing.

Raid tiers 5-7 offer the largest booster rates. Frequent raids, cheap capsules and repeated item fishing can therefore increase the supply of cards and recyclable Cobblecard Dust. Modpack authors should tune server percentages alongside existing chest spawns, recycling costs and card bonuses. Probabilities describe receiving packs, not the rarity of cards inside them.

## Validation and runtime checklist

Source, official release metadata and actual reward resources were inspected before implementation. These checks establish verified IDs and mechanisms; they do not replace playing with the addons.

### Current Cobblemon 1.8.2 validation

Both builds and the 47 shared tests pass under Java 21. Isolated launches with the six compatible addons and RCT API 0.16.2-beta, GeckoLib 4.9.3 and Forge Config API Port 21.1.6 reached Fabric's `eula=false` gate and NeoForge's `Done`, respectively. No players joined these servers. The attempt including Parting Gifts 3.0.3 failed on both loaders with the removed `ElementalType` constructor, before the incompatibility metadata was added. This is recorded as an addon incompatibility, not a successful seven-addon test.

| Case | Fabric | NeoForge |
| --- | --- | --- |
| No optional addons | PASS: EULA gate, exit 0 | PASS: `Done`, clean console stop, exit 0 |
| Six addons, integrations enabled | PASS: six detected, context APIs verified, EULA gate | PASS: six detected, context APIs verified, `Done`, clean stop |
| Six addons, all integration settings disabled | PASS: six flags false, EULA gate | PASS: six flags false, `Done`, clean stop |
| Parting Gifts 3.0.3 installed | Expected rejection: explicit loader incompatibility before initialization | Expected rejection: explicit pre-loading incompatibility with explanatory reason |
| Old RCT API 0.16.1-beta installed | Expected rejection: minimum API version shown | Expected rejection: minimum API version shown |

These checks verify startup and configuration loading, not actual booster rewards. The isolated cases are `v2182-absent`, `v2182-six-enabled`, `v2182-six-disabled`, `v2182-all-enabled` (Parting Gifts rejection), and `v2182-rct-old`. Packaged Fabric/NeoForge metadata was also inspected to confirm the 1.8.2 minimum, optional API constraints and exact Parting Gifts exclusions. The current NeoForge six-addon run still reports the existing Gacha machine-table/PokeBlocks warnings described above; it reaches `Done` despite those unrelated table errors.

REI/EMI NeoForge discovery was also corrected: REI has a native annotated client adapter and EMI's shared plugin declares its verified entrypoint annotation. The versions in the README still support Minecraft 1.21.1 on both loaders. Client recipe registration and rendering have not been tested in-game.

### Historical Cobblemon 1.8.1 checks

The following runs predate the 1.8.2 target. Their Parting Gifts success and older RCT API versions apply to that earlier setup only.

| Check | Result |
| --- | --- |
| Fabric build | PASS: `:fabric:build`, Java 21 |
| NeoForge build | PASS: `:neoforge:build`, Java 21 |
| Automated common tests | PASS for 2.1.0: `:common:test`; 47 tests, 0 failures, including independent new toggles and RCT nested-root regression |
| Fabric 2.1.0 bootstrap, optional addons absent | PASS: isolated development launch reached `eula=false` gate; exit 0 |
| Fabric 2.1.0 bootstrap, three new addons present | PASS: three detected enabled; RCT/Rad Gyms context APIs verified; EULA gate; exit 0 |
| Fabric 2.1.0 bootstrap, all seven addons enabled | PASS: seven detected, public APIs verified; RCT API 0.16.1-beta; EULA gate; exit 0 |
| Fabric bootstrap, seven addons with three new integrations disabled | PASS: new enabled flags read false; EULA gate; exit 0 (RCT API 0.16.0-beta is accepted on Fabric) |
| NeoForge 2.1.0 dedicated startup, optional addons absent | PASS: isolated server reached `Done`, zero players, clean console stop; exit 0 |
| NeoForge 2.1.0 dedicated startup, three new addons present | PASS: three detected, context APIs verified, `Done`, clean stop; exit 0 |
| NeoForge 2.1.0 dedicated startup, seven addons, three new integrations disabled | PASS: flags read false, RCT API 0.16.1-beta, `Done`, clean stop; exit 0 |
| NeoForge 2.1.0 dedicated startup, all seven addons enabled | PASS: seven detected, public APIs verified, `Done`, clean stop; exit 0; existing Gacha machine warnings above |
| Each addon tested individually in gameplay | NOT RUN |
| Enabled and disabled reward behavior | NOT RUN in-game |
| Actual pouch/capsule/fishing/release/gym/trainer rewards | NOT RUN in-game |
| Dedicated-server reward gameplay and multiplayer | NOT RUN |
| Non-default config, 0% and 100% | NOT RUN in-game |
| Datapack replacements and reloads | NOT RUN in-game |
| Reconnect, restart and repeated interaction | NOT RUN in-game |

The successful build/test command was `:common:test :fabric:build :neoforge:build --console=plain` under Java 21. Historical 1.8.1 bootstrap runs used isolated directories under `build/compat-smoke/<loader>/v21-{absent,present,all-disabled,all-enabled}/`; current 1.8.2 runs use `v2182-*`. Their `bootstrap.log` files are local build artifacts. No EULA acceptance file was changed: Fabric stopped at its false setting, while NeoForge development launches bypassed that gate and started the server. These runs had zero players and did not exercise reward interactions. NeoForge's three-addon run also logged non-fatal upstream mixin warnings about client classes on a dedicated server; it still reached `Done` and stopped cleanly.

Before a release, run the remaining gameplay checklist separately on Fabric and NeoForge. Verify that normal rewards survive, disabled integrations give no packs, independent fishing rewards can both occur without accidental duplicate callbacks, completed multiplayer raids retain the addon's participant handling, and custom replacing raid rewards bypass the tier integration. Test full inventories and survival item consumption through the addons' native mechanics.

## Implementation files

Created files (paths are relative to the repository):

| Directory | Files |
| --- | --- |
| `common/src/main/java/com/howlite/cobblemoncards/compat/` | `CompatManager.java`, `CompatConfig.java`, `CompatReward.java`, `BoosterRewardHelper.java`, `RewardProbability.java`, `CompatBoosterLootEntry.java`, `RustlingSpotsCompat.java`, `RctCompat.java`, `RadGymsCompat.java` |
| `fabric/src/main/java/com/howlite/cobblemoncards/fabric/compat/` | `FabricLootCompat.java` |
| `neoforge/src/main/java/com/howlite/cobblemoncards/neoforge/compat/` | `NeoForgeLootCompat.java`, `CompatBoosterModifier.java` |
| `neoforge/src/main/resources/data/` | `neoforge/loot_modifiers/global_loot_modifiers.json`, `cobblemon-cards/loot_modifiers/optional_boosters.json` |
| `common/src/test/java/com/howlite/cobblemoncards/compat/` | `RewardProbabilityTest.java`, `BoosterRewardHelperTest.java`, `CompatConfigTest.java`, `CompatManagerTest.java` |
| `docs/` | `OPTIONAL_INTEGRATIONS.md` |

Modified files across the compatibility feature: `common/build.gradle`; `common/src/main/java/com/howlite/cobblemoncards/CobblemonCardsConfig.java`; the shared compatibility classes above; `common/src/main/resources/assets/cobblemon-cards/lang/en_us.json` and `fr_fr.json`; `fabric/src/main/java/com/howlite/cobblemoncards/fabric/FabricCobblemonCards.java`; `neoforge/src/main/java/com/howlite/cobblemoncards/neoforge/NeoForgeCobblemonCards.java`; the NeoForge modifier above; the configuration/manager tests above; `gradle.properties`; `README.md`; `README_MODRINTH.md`; `CHANGELOG.md`; `docs/V2_GUIDE.md`.

The 1.8.2 migration also updates `fabric/build.gradle`, `fabric/src/main/resources/fabric.mod.json`, `neoforge/src/main/resources/META-INF/neoforge.mods.toml`, the shared client `compat/emi/CardRecyclerEmiPlugin.java` and `compat/rei/CardRecyclerReiPlugin.java`, and adds `neoforge/src/main/java/com/howlite/cobblemoncards/neoforge/compat/rei/NeoForgeCardRecyclerReiPlugin.java`.

Downloaded research artifacts and isolated test-server files remain under ignored `build/` directories and are not distributed in either mod jar.
