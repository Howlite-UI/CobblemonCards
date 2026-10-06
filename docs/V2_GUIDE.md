# Cobblemon Cards V2 guide

This guide describes **2.0.0-beta.1** for **Minecraft 1.21.1**, **Java 21**, and **Cobblemon 1.8.1** on Fabric and NeoForge. See [README.md](../README.md#-required-dependencies) for loader-specific dependencies and [CHANGELOG.md](../CHANGELOG.md) for the release notes.

## Installation and upgrading

Install the JAR matching your loader: `cobblemon-cards-fabric-2.0.0-beta.1.jar` or `cobblemon-cards-neoforge-2.0.0-beta.1.jar`. Fabric bundles MidnightLib; NeoForge requires its separate dependency. Both loaders require Accessories and the matching owo-lib runtime.

When upgrading from 1.x, keep a backup of your world/configuration, replace the old addon JAR, and update Cobblemon and its other addons to compatible versions. Keep `config/cobblemon-cards.json`: existing values remain in effect, and new settings are added during startup. Existing cards keep their stored stats; loading a world does not reroll them. Older cards without additional trainer fields and binders using the legacy container component remain readable.

## Equipping cards and reading bonuses

Put cards into a binder, then equip it in the **binder accessory slot**. Carrying the binder in your normal inventory does not apply its passive bonuses. Cards inside the equipped binder contribute their primary stats and any additional trainer bonuses.

Cosmetic cards grant no passive stats. The Master Album is a collection item by default; enable `masterAlbumGivesStats` if it should also grant bonuses.

Player stats use two different units:

| Display | Meaning |
| :--- | :--- |
| `+5.0` Max Health, Armor, Luck, or Mining Speed | Add 5 to the corresponding attribute. |
| `+5.0%` Movement Speed, Attack Damage, or Attack Speed | Add 5% of the base attribute. |
| `+5.0%` spawn or trainer stat | A bonus used by the spawn-weight, experience, catch-rate, or shiny calculation. |

By default, displayed stat totals include configuration multipliers. Gameplay caps can limit the final effect even when a tooltip shows a higher total. With bonuses disabled, changing the setting affects their application; it does not rewrite the cards' stored stats.

## Grading and additional trainer bonuses

The Grading Station gives an ungraded card a grade from **1 to 10**. Each grade increases its stat values by **3% of their ungraded value**, reaching **+30% at grade 10**. The Card Restorer raises an already graded card to a higher target grade.

A card can also have one additional trainer stat: **Experience Boost**, **Catch Rate Boost**, or **Shiny Chance**. The primary stat is preserved.

| Acquisition | Default rule |
| :--- | :--- |
| Grading an ungraded non-cosmetic card | 50% chance when the resulting grade is at least 9. |
| Restoring a non-cosmetic card without a trainer bonus | 50% chance when moving from below grade 9 to grade 9 or 10. |
| Newly generated Legendary, Mythic, or Shiny card from a drop/booster | 5% chance of an additional trainer bonus. |

Restoring from grade 9 to 10 does not retry a failed threshold roll. Existing trainer bonuses are retained and scale with the grade.

Configure the threshold and acquisition chances using `enableTrainerStatGrading`, `trainerStatMinGrade`, `trainerStatGradeChance`, and `trainerStatLuckyChance`. `trainerStatLuckyValueMultiplier` scales the additional trainer value relative to the primary value; its default is **0.25**. `enableTrainerStats` controls application of trainer bonuses.

## Restoring a card

1. Insert **one already graded card** into the Card Restorer.
2. Select a target grade above its current grade, up to **10**.
3. Supply the dust cost shown by the machine and press **Restore**.
4. Close the GUI if desired. The machine continues while its chunk is loaded; reopening it shows the remaining time.

Progress is saved with the world and resumes when the chunk is loaded again. The machine does not simulate processing while its chunk is unloaded. Dust is consumed once, at completion. Removing or replacing the card cancels the operation. Removing required dust pauses progress until enough dust is available again. The target grade stays locked while an operation is active.

### Duration

The duration depends on the **target grade**, using the server's settings:

```text
duration in ticks = restorerBaseProcessTime
                  + (targetGrade - 2) × restorerProcessTimePerGrade
```

At 20 ticks per second, default settings give:

| Target grade | Ticks | Duration |
| :--- | ---: | ---: |
| 2 | 60 | 3 seconds |
| 5 | 240 | 12 seconds |
| 9 | 480 | 24 seconds |
| 10 | 540 | 27 seconds |

Set `restorerProcessTimePerGrade` to **0** for a fixed duration. Changing timing or cost settings affects newly started restorations; an active operation retains its original duration and dust cost.

### Dust cost

```text
dust cost = targetGrade × restorerBaseCost
          × 2^(targetGrade - currentGrade - 1)
```

With `restorerBaseCost = 5`, restoring grade 4 to 5 costs **25 dust**, while jumping from grade 5 to 10 costs **800 dust**. Skipping several grades increases the cost; check the amount shown before starting.

## Spawn and trainer multipliers

Spawn stats affect the **relative weights of Pokémon eligible to spawn**. They respect biome and spawn-bucket rules. A Water Spawn bonus favors eligible Water Pokémon; it does not create more spawn attempts or make ineligible species appear. Egg-group and EV-yield spawn bonuses are optional and **disabled by default**. Different enabled spawn-bonus families can stack; the stronger elemental boost is used for a dual-type species.

Trainer bonuses act on the server:

| Bonus | Effect | Default cap |
| :--- | :--- | :--- |
| Experience Boost | Multiply experience awarded to the player's Pokémon. | 5× (`maxExpBoostMultiplier`) |
| Catch Rate Boost | Multiply Cobblemon's catch-rate value; actual capture odds also depend on its other rules. | 5× (`maxCatchBoostMultiplier`) |
| Shiny Chance | Divide Cobblemon's “1 in N” rate by the bonus multiplier for new wild spawns associated with the player. | 10× (`maxShinyBoostDivisor`) |

For a displayed **+600% Shiny Chance** bonus:

```text
multiplier = min(maxShinyBoostDivisor, 1 + 600 / 100) = 7
new rate   = Cobblemon shinyRate / 7
```

With `shinyRate = 8192`, that means about **1/1170**, or **0.085% per newly spawned Pokémon**. Even after 100 new spawns, the probability of seeing no shiny is about **92%**. A bonus improves each roll; it does not guarantee a shiny within a time limit or change Pokémon that already exist. With the default 10× cap, anything above **+900%** gives the same final multiplier.

## Configuration tabs

Use MidnightLib's in-game configuration screen, or edit `config/cobblemon-cards.json` while the game/server is stopped and restart it. Mod Menu provides configuration-screen access on Fabric. On a dedicated server, change the **server's file** for gameplay settings; client-side changes do not alter server gameplay.

| Tab | Settings |
| :--- | :--- |
| General | Master stat switch, global multiplier, card/booster sources, Fakemon support. |
| Player | Attribute bonuses and their individual multipliers. |
| Spawning | Elemental, egg-group, and EV-yield spawn bonuses and weight caps. |
| Trainer | Experience/catch/shiny multipliers, caps, and acquisition rules. |
| Machines | Recycling, grading, restoration durations, and dust costs. |
| Binders | Available tiers, page counts, and Master Album bonuses. |

If cards show zero bonuses, check `enableCardStats`, the relevant stat-family toggle, and its multipliers in the configuration for the loader you are using. Fabric and NeoForge development runs use separate config folders. Re-enabling bonuses does not reroll the type already stored on an old card.

Custom species from Cobblemon addons can be authorized individually and given card sprites. This requires a server datapack plus a client resource pack; follow the [custom Fakemon sprite guide](CUSTOM_FAKEMON_SPRITES.md) or copy the [ready-made template](../templates/fakemon-card-integration/README.md).

## Beta validation

Both loader builds and packaged metadata/resources were checked during the migration. Targeted headless checks passed for restoration (38 checks) and the Cobblemon 1.8.1 shiny event (15 checks). A full multiplayer session, existing-world upgrade playtests, and large-collection performance checks remain pending before a stable V2 release.
