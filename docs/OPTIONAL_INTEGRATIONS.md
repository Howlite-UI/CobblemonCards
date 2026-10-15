# Optional booster reward integrations

All eight addons were investigated for Minecraft 1.21.1 on 14 October 2026. Addons are optional dependencies: CobblemonCards continues to work when they are absent. An investigated release is not an in-game compatibility certification. NeoForge dedicated-server startup was tested with zero players; Fabric initialization was tested up to its EULA gate. Actual reward mechanics and multiplayer gameplay have not been tested.

## Feasibility matrix

| Addon | Investigated release | Published loaders | Verified mod ID | Reward mechanism | Implementation status |
| --- | --- | --- | --- | --- | --- |
| Cobblemon Rustling Spots | 5.0.1 | Fabric, NeoForge | `rustlingspots` | Public server interaction callback | Implemented as an additional interaction reward; gameplay validation pending |
| Cobblemon Raid Dens | 0.13.2+1.21.1 | Fabric, NeoForge | `cobblemonraiddens` | Seven separate Raid Pouch loot tables | Implemented; gameplay validation pending |
| CobbledGacha v.3 | 3.0.3 | Fabric, NeoForge | `cobbledgacha` | Five Minecraft capsule reward loot tables | Implemented; gameplay validation pending |
| Cobblemon Battle Tower | 1.11.23 | Fabric, NeoForge | `cobblemon_battle_tower` | Configured Battle Points shop | Deferred: no verified additive shop registration API |
| Cobblemon Expeditions | 1.8.1 | Fabric, NeoForge | `cobblemon_expeditions` | Custom weighted expedition loot and persistent reward inbox | Deferred: no additive reward-bundle callback |
| CobbleDollars | 2.0.0+Beta-6.1 | Fabric, NeoForge | `cobbledollars` | Datapack merchant shops and addon currency | Deferred: no safe additive shop registration and transaction API verified |
| Cobblemon PokeLoot | Published 1.0.2; jar metadata 1.0.0 | NeoForge | `pokeloot` | Direct private reward generation; block loot tables do not generate rewards | Deferred: no safe public reward hook |
| Cobblemon Fishing Loot | 1.0.0 | Fabric, NeoForge | `cobblemonfishingloot` | Root Poké Rod fishing loot table | Implemented; gameplay validation pending |

The feasibility research preceded implementation. No configuration or shop prices are exposed for deferred integrations.

## Server configuration

Use the **Compatibility** tab in the existing MidnightLib configuration screen, or edit `config/cobblemon-cards.json` with the server stopped and restart it. Gameplay uses the server's values; a client-only change does not alter rewards. All four implemented integrations default to enabled and automatically activate when their addon is detected. The following keys are flat entries in the existing file, not a nested `compatibility` object or a separate configuration file:

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
  "fishingLootClassicChance": 1.0
}
```

Merge these fields into the existing file and keep other settings. Every chance is a percentage from 0 to 100: 0 disables that roll and 100 always succeeds when an eligible booster exists. Reward calculation clamps finite out-of-range values; non-finite values give no reward. Setting an integration's enabled flag to `false` disables all of its added rewards without editing addon data.

Rustling mappings use `FAMILY=selector`, case-insensitively. Selectors are `classic` (also `base`), `generation`, `themed`, `gen1` through `gen9`, or any existing type name such as `fire`, `water` or `ghost`. For example, `"FLYING=flying"` adds a themed mapping for flying spots. Unmapped families and unrecognized selectors fall back to classic. The first matching family entry wins. No full item ID is needed in these mappings.

## Verified loot integration behavior

The loot integrations add separate booster rolls without replacing the addon's existing reward entries. Each requires its detected addon and its own enabled setting on the server. They use the existing classic, generation and type-themed booster items; there are no new booster definitions or invented rare/special booster categories.

Fabric appends pools through its loot modification API. NeoForge uses a Global Loot Modifier restricted to the exact table IDs below. Shared reward selection checks registered booster items. Neither loot implementation needs addon classes, compile dependencies, a client reward packet, or a new claims/economy system. Selection is uniform across eligible individual packs, not across category names: Great capsules can select one classic or nine generation packs, and Ultra capsules can select nine generation or eighteen type-themed packs. Missing/non-booster registry entries are skipped; an empty eligible selection gives no reward.

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

## Addons requiring further work

### Cobblemon Expeditions

Expeditions uses custom `data/cobblemon_expeditions/expedition_loot/*.json` weighted pools, not Minecraft loot tables. Adding an item to those pools competes with normal rewards; it cannot provide an independent additive 10% roll. No supported reward-bundle extension callback was verified. Its documented public storage API handles persistent state and atomic reward claims, and must not be replaced to inject booster rewards. Request an upstream callback before the completed reward bundle is persisted.

Evidence: [official addon](https://www.curseforge.com/minecraft/mc-mods/cobblemon-expeditions), [official documentation repository](https://github.com/ZIPender/cobblemon_expeditions_doc), [atomic storage/claim contract](https://github.com/ZIPender/cobblemon_expeditions_doc/blob/main/storage-api.md). License: All Rights Reserved. The repository contains documentation, not the addon source.

### Cobblemon Battle Tower

The native Battle Points shop supports configurable items and commands in `config/cobblemon_battle_tower/bp_shop_items.json`, reloaded by `/bt reload`. No supported additive shop contribution or milestone callback was verified. Replacing a shop list at startup would lose ownership across reloads and server/client synchronization. A safe future integration should extend this shop and use its existing BP transaction handler. The suggested 100/200/250 BP prices are not active CobblemonCards settings.

Evidence: [official documentation](https://modrinth.com/mod/cobblemon-battle-tower), [Fabric 1.11.23](https://modrinth.com/mod/cobblemon-battle-tower/version/Srt2YL8E), [NeoForge 1.11.23](https://modrinth.com/mod/cobblemon-battle-tower/version/hzDUcNDj). License: All Rights Reserved. No official source repository or API documentation was linked.

### CobbleDollars

Beta-6 documentation defines `config/cobbledollars/default_shop.json` and custom shops in `data/<namespace>/cobbledollars/shop/<id>.json`. Custom shops can be opened through the native merchant command, but registering a new shop does not add its offers to naturally spawned merchants' default shop. No supported additive default-shop reload hook or durable purchase extension API was verified. CobblemonCards does not rewrite that configuration, create another balance, expose a purchase GUI or perform currency deductions. The suggested 2,500/5,000/7,500 prices are not active settings.

Evidence: [official wiki](https://harmex.gitbook.io/cobbledollars), [current default shop format](https://harmex.gitbook.io/cobbledollars/configuration/default-shop), [datapack shop format](https://harmex.gitbook.io/cobbledollars/datapack/shops), [native merchant command](https://harmex.gitbook.io/cobbledollars/commands/cobblemerchant), [Fabric Beta-6.1](https://modrinth.com/mod/cobbledollars/version/NQdxKsW7), [NeoForge Beta-6.1](https://modrinth.com/mod/cobbledollars/version/Iqiqjfsi). License: All Rights Reserved. No official source repository was linked.

### Cobblemon PokeLoot

The investigated Munkey PokeLoot mod is NeoForge-only. Its published 1.0.2 jar declares 1.0.0 internally. Its six ball block loot tables are empty; reward generation happens through a private method when the block is destroyed. Patching those tables would not extend actual rewards, and no safe public hook distinguishes naturally generated loot objects from placed/admin objects. A similarly named datapack project is a different addon and must not be treated as this mod.

Evidence: [official CurseForge project](https://www.curseforge.com/minecraft/mc-mods/cobblemon-pokeloot), [official Modrinth project](https://modrinth.com/mod/munkeys-cobblemon-pokeloot). License: All Rights Reserved. No source/API documentation was linked.

## Datapacks, authority and balance

Existing entries and reward delivery stay with the addon. Additive hooks target only the exact IDs listed above. A server datapack can replace a targeted table's original contents; the extra roll still applies when that exact table is queried as the reward root and the integration is enabled. A replacing raid boss table, a redirected reward ID or a custom capsule outside the verified five IDs is respected and receives no guessed integration. Disable the relevant integration when a pack requires complete control of booster rewards.

There is a loader difference for custom wrapper tables: Fabric's appended pool also runs when the targeted table is referenced as a child of another table. NeoForge's Global Loot Modifier runs for the queried root table, so a custom wrapper using a different root ID receives no automatic booster even if it references a targeted child. Both implementations cover the verified addons' normal reward roots. Prefer those roots when authoring overrides intended to behave the same on both loaders.

Every legitimate targeted loot roll is independent. Reopening a GUI is not a reward trigger, and rewards are not suppressed merely because the same item was rolled before. Repeated administrative loot generation, creative access and deliberately repeated table references remain controlled by the administrator/datapack. This integration does not certify upstream addon claim persistence or third-party callback behavior without runtime testing.

Raid tiers 5-7 offer the largest booster rates. Frequent raids, cheap capsules and repeated item fishing can therefore increase the supply of cards and recyclable Cobblecard Dust. Modpack authors should tune server percentages alongside existing chest spawns, recycling costs and card bonuses. Probabilities describe receiving packs, not the rarity of cards inside them.

## Validation and runtime checklist

Source, official release metadata and actual reward resources were inspected before implementation. These checks establish verified IDs and mechanisms; they do not replace playing with the addons.

| Check | Result |
| --- | --- |
| Fabric build | PASS: `:fabric:build`, Java 21 |
| NeoForge build | PASS: `:neoforge:build`, Java 21 |
| Automated common tests | PASS: `:common:test`; 44 tests, 0 failures |
| Fabric bootstrap, optional addons absent | PASS: isolated development launch reached `eula=false` gate; exit 0 |
| Fabric bootstrap, all four integrated addons present | PASS: all four detected enabled; Rustling API registered; reached `eula=false` gate; exit 0 |
| NeoForge dedicated startup, optional addons absent | PASS: isolated development server reached `Done`, zero players, clean console stop; exit 0 |
| NeoForge dedicated startup, all four integrated addons present | PASS: detected integrations, reached `Done`, zero players, clean console stop; exit 0; Gacha machine warning above |
| Each addon tested individually in gameplay | NOT RUN |
| Enabled and disabled reward behavior | NOT RUN in-game |
| Actual pouch/capsule/fishing rewards | NOT RUN in-game |
| Dedicated-server reward gameplay and multiplayer | NOT RUN |
| Non-default config, 0% and 100% | NOT RUN in-game |
| Datapack replacements and reloads | NOT RUN in-game |
| Reconnect, restart and repeated interaction | NOT RUN in-game |

The successful build/test command was `:common:test :fabric:build :neoforge:build --console=plain` under Java 21. Bootstrap runs used isolated directories under `build/compat-smoke/<loader>/<absent-or-present>/`; their `bootstrap.log` files are local build artifacts. No EULA acceptance file was changed: Fabric stopped at its false setting, while the NeoForge development launch bypassed that gate and started the server. Neither run had players or exercised reward interactions.

Before a release, run the remaining gameplay checklist separately on Fabric and NeoForge. Verify that normal rewards survive, disabled integrations give no packs, independent fishing rewards can both occur without accidental duplicate callbacks, completed multiplayer raids retain the addon's participant handling, and custom replacing raid rewards bypass the tier integration. Test full inventories and survival item consumption through the addons' native mechanics.

## Implementation files

Created files (paths are relative to the repository):

| Directory | Files |
| --- | --- |
| `common/src/main/java/com/howlite/cobblemoncards/compat/` | `CompatManager.java`, `CompatConfig.java`, `CompatReward.java`, `BoosterRewardHelper.java`, `RewardProbability.java`, `CompatBoosterLootEntry.java`, `RustlingSpotsCompat.java` |
| `fabric/src/main/java/com/howlite/cobblemoncards/fabric/compat/` | `FabricLootCompat.java` |
| `neoforge/src/main/java/com/howlite/cobblemoncards/neoforge/compat/` | `NeoForgeLootCompat.java`, `CompatBoosterModifier.java` |
| `neoforge/src/main/resources/data/` | `neoforge/loot_modifiers/global_loot_modifiers.json`, `cobblemon-cards/loot_modifiers/optional_boosters.json` |
| `common/src/test/java/com/howlite/cobblemoncards/compat/` | `RewardProbabilityTest.java`, `BoosterRewardHelperTest.java`, `CompatConfigTest.java`, `CompatManagerTest.java` |
| `docs/` | `OPTIONAL_INTEGRATIONS.md` |

Modified files: `common/build.gradle`; `common/src/main/java/com/howlite/cobblemoncards/CobblemonCardsConfig.java`; `common/src/main/resources/assets/cobblemon-cards/lang/en_us.json` and `fr_fr.json`; `fabric/src/main/java/com/howlite/cobblemoncards/fabric/FabricCobblemonCards.java`; `neoforge/src/main/java/com/howlite/cobblemoncards/neoforge/NeoForgeCobblemonCards.java`; `README.md`; `README_MODRINTH.md`; `CHANGELOG.md`; `docs/V2_GUIDE.md`.

Downloaded research artifacts and isolated test-server files remain under ignored `build/` directories and are not distributed in either mod jar.
