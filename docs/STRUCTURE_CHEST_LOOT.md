# Structure container booster compatibility

This guide describes CobblemonCards' structure container loot policy for Minecraft 1.21.1 on Fabric and NeoForge. Structure chest rewards remain enabled by default with a **2% chance**. The injected pool has one roll, with weight **10** for the classic booster and **3** for each generation 1–9 booster. The compatibility change expands the tables that pack authors can select, without changing those defaults or weights. Eligibility follows the loot table rather than a test for a chest block, so barrels and other suitable containers can use the same policy.

## Server configuration and priority

The existing MidnightLib file is `config/cobblemon-cards.json`. The new keys are flat entries in that file and appear in the **General** configuration tab:

| Key | Default | Purpose |
| --- | --- | --- |
| `additionalBoosterLootTables` | `[]` | Exact IDs of verified custom structure container tables to allow. |
| `excludedBoosterLootTables` | `[]` | Exact IDs to exclude from structure booster injection. |
| `debugBoosterLootTables` | `false` | Log inspected table IDs, eligibility reasons, exclusions and injection results. |

Merge entries into the existing file while the server is stopped, then restart. Gameplay reads the server's settings. Editing only a multiplayer client's configuration does not change server loot. Existing configuration values are retained, and the new lists default to empty.

Selection follows these rules on both loaders:

1. An explicit exclusion rejects the table, including an automatically detected or allowlisted table.
2. A path containing a complete `blocks`, `entities`, `gameplay`, `fishing` or `spawners` segment is rejected. Known Trial reward helpers, the ominous reward root, the vanilla Trial pot table and 17 verified Cobblemon injection helpers are also protected. A declared type other than the chest or generic parameter set is rejected. The allowlist cannot override these protections.
3. A valid ID in `additionalBoosterLootTables` is eligible.
4. Otherwise, a path beginning with `chests/` or containing `/chests/` is eligible. The two verified vanilla Trial dispenser tables listed below are also eligible by exact ID.
5. All other tables are left unchanged. An eligible table only receives a pool when `enableBoosterChestSpawn` is enabled and the configured chance is positive.

IDs must have an explicit namespace and path, such as `bca:general/attic`; use lowercase and omit `.json`. Malformed entries are ignored with warnings. Protected allowlist entries are also ignored with warnings. Syntactically valid IDs for absent tables are harmless: this feature modifies tables as they load and does not create or resolve a missing table. Duplicate entries and repeated injection callbacks for the same table object do not add pools.

The pool also checks the loot generation context to reject block, entity and fishing uses, and rejects reward generation located at a vanilla vault, trial spawner or decorated pot. In particular, ordinary chest, trapped chest and Ender Chest block drops do not receive boosters through this feature, even when their IDs are allowlisted. This preserves the protection against farming boosters by placing and breaking chest blocks. The origin-block check distinguishes the verified vanilla callers; it cannot establish the provenance of every modded loot call.

## Dungeons, barrels and Trial Chambers

Vanilla dungeon chests and several Trial Chamber chest and barrel tables already follow `chests/`. Finding basic resources such as sticks or honeycomb in one of these containers does not mean its table was rejected. The vanilla resources and structure NBT were inspected in the [official Minecraft 1.21.1 client artifact](https://piston-data.mojang.com/v1/objects/30c73b1c5da787909b2f73340419fdf13b9def88/client.jar), whose SHA-1 matches `30c73b1c5da787909b2f73340419fdf13b9def88`. The findings are:

| Container or source | Exact loot table ID | Treatment |
| --- | --- | --- |
| Dungeon chest | `minecraft:chests/simple_dungeon` | Automatic, standard chest convention. |
| Trial entrance chest, including sticks and honeycomb | `minecraft:chests/trial_chambers/entrance` | Automatic, standard chest convention. |
| Trial corridor barrel, including honeycomb | `minecraft:chests/trial_chambers/corridor` | Automatic, regardless of its barrel block. |
| Trial intersection barrel | `minecraft:chests/trial_chambers/intersection_barrel` | Automatic, regardless of its barrel block. |
| Trial supply chest | `minecraft:chests/trial_chambers/supply` | Automatic, standard chest convention. |
| Natural Trial reward chest; shared with vault rewards | `minecraft:chests/trial_chambers/reward` | One pool on the root table; vault use is blocked at generation time. |
| Trial corridor dispenser | `minecraft:dispensers/trial_chambers/corridor` | Automatic by this verified exact ID. |
| Trial chamber dispenser | `minecraft:dispensers/trial_chambers/chamber` | Automatic by this verified exact ID. |
| Registered water dispenser table, without a verified natural caller | `minecraft:dispensers/trial_chambers/water` | No automatic selection. |
| Trial decorated pot | `minecraft:pots/trial_chambers/corridor` | Protected even if allowlisted: its one slot cannot safely fit the existing loot plus an added booster. |
| Trial container with preset `Items` and no `LootTable` | No table to modify | Unsupported by a table-loading hook. |

Examples of direct NBT references, relative to `data/minecraft/structure/`, are `trial_chambers/corridor/entrance_1.nbt` for entrance and reward chests, `trial_chambers/decor/barrel.nbt` for the corridor barrel, `trial_chambers/intersection/intersection_2.nbt` for an intersection barrel, and `trial_chambers/chests/supply.nbt` for the supply chest. The two dispenser references occur in `trial_chambers/corridor/addon/arrow_dispenser.nbt` and `trial_chambers/dispensers/wall_dispenser.nbt`. Their corresponding JSON resources live under `data/minecraft/loot_table/` with the paths shown in the table.

Some Trial containers are deliberately filled directly in structure NBT. For example, `trial_chambers/corridor/end_2.nbt` contains a chest with an iron block, cake and diamond in `Items`, without a loot-table reference. This feature cannot append loot where no table is evaluated.

The decorated pot needs a different reward design: adding a separate booster stack to its existing single-stack loot can overflow its one slot, causing either the original item or the booster to be discarded. Its exact vanilla table is protected even if allowlisted. The runtime origin-block check also blocks a shared eligible table from adding boosters when a vanilla decorated pot uses it. There is no broad matching of `barrel`, `dispenser`, `pot`, `trial` or `dungeon`; the dispenser additions above are exact verified exceptions.

The shared Trial reward root is also used by three naturally generated chests, so rejecting every table with `reward` in its name would remove legitimate structure loot. Its helper tables run several times and must not receive separate booster pools: otherwise nested calls would unintentionally increase the chance. Vanilla vault and trial-spawner rewards are gameplay sources and do not gain structure boosters.

The exact protected reward IDs are `minecraft:chests/trial_chambers/reward_common`, `minecraft:chests/trial_chambers/reward_rare`, `minecraft:chests/trial_chambers/reward_unique`, `minecraft:chests/trial_chambers/reward_ominous`, `minecraft:chests/trial_chambers/reward_ominous_common`, `minecraft:chests/trial_chambers/reward_ominous_rare`, and `minecraft:chests/trial_chambers/reward_ominous_unique`. This is an explicit set of the verified helpers and ominous root, not a substring rule for every table named `reward`.

Cobblemon adds nested loot tables of its own to vanilla structure tables through its shared [`LootInjector`](https://gitlab.com/cable-mc/cobblemon/-/raw/main/common/src/main/kotlin/com/cobblemon/mod/common/loot/LootInjector.kt) implementation on Fabric and NeoForge. A separate booster pool in both a vanilla parent and its Cobblemon helper would give extra booster opportunities; for example, a dungeon roll could run pools in both `minecraft:chests/simple_dungeon` and `cobblemon:injection/chests/simple_dungeon`. The latter is one of 17 exact helper IDs verified in both loader artifacts for Cobblemon 1.7.3, 1.8.1 and 1.8.2, including the official [Fabric 1.8.2](https://modrinth.com/mod/cobblemon/version/GJ2a1b8O) and [NeoForge 1.8.2](https://modrinth.com/mod/cobblemon/version/9IXh9584) releases. These helpers are protected even if allowlisted. The vanilla parent keeps its single booster pool, and Cobblemon's existing item pools and nested references remain intact. The rule uses verified exact IDs rather than rejecting every third-party path containing `injection`.

## What the loading APIs can tell us

Fabric uses [`LootTableEvents.MODIFY`](https://github.com/FabricMC/fabric/blob/1.21.1/fabric-loot-api-v3/src/main/java/net/fabricmc/fabric/api/loot/v3/LootTableEvents.java); NeoForge uses [`LootTableLoadEvent`](https://github.com/neoforged/NeoForge/blob/1.21.1/src/main/java/net/neoforged/neoforge/event/LootTableLoadEvent.java). These hooks expose a loaded table and its ID, not a reference to the generated structure or container that might later request it. Fabric's [`LootTableSource`](https://github.com/FabricMC/fabric/blob/1.21.1/fabric-loot-api-v3/src/main/java/net/fabricmc/fabric/api/loot/v3/LootTableSource.java) describes the resource provider, not the gameplay origin of future loot generation. A table's [declared parameter set](https://docs.neoforged.net/docs/1.21.1/resources/server/loottables/#loot-parameter-sets) describes its accepted context; it does not establish that it belongs to a naturally generated structure. The checked NeoForge 21.1.255 source applies the event to vanilla, mod and user datapack loot tables on loading and reloading.

Consequently, CobblemonCards keeps the established `chests/` convention, adds only the two verified exact dispenser IDs, and adds no broad matching for `loot`, `treasure`, `reward`, `dungeon` or arbitrary occurrences of `chest`. Table IDs such as `examplemod:structures/ruins/treasure` or `examplemod:ancient_city/rewards` remain rejected unless explicitly allowed after their uses have been verified. These `examplemod` IDs are hypothetical examples.

Verified examples of the existing automatic conventions include [Repurposed Structures' `repurposed_structures:chests/dungeons/badlands`](https://github.com/TelepathicGrunt/RepurposedStructures/blob/1.21-Arch/common/src/main/resources/data/repurposed_structures/loot_table/chests/dungeons/badlands.json) and [YUNG's Better Dungeons' `betterdungeons:skeleton_dungeon/chests/common`](https://github.com/YUNG-GANG/YUNGs-Better-Dungeons/blob/1.21.1/Common/src/main/resources/data/betterdungeons/loot_table/skeleton_dungeon/chests/common.json). Their checked resources declare `minecraft:chest`; neither requires a new path rule.

The allowlist is a pack author's deliberate selection of verified structure container tables. A table can be reused by a mod for several purposes, so inspect its callers as well as its contents. No name or type check can certify every use made by arbitrary third-party code. Mods that directly fill containers without Minecraft loot tables cannot be supported by this hook.

## Verified Cobbleverse example

The official [Cobbleverse 1.7.42 release](https://modrinth.com/modpack/cobbleverse/version/1.7.42), published on 21 July 2026, includes custom datapacks. The [official `.mrpack` artifact](https://cdn.modrinth.com/data/Jkb29YJU/versions/4SKGla61/COBBLEVERSE%201.7.42.mrpack) contains:

- `overrides/datapacks/COBBLEVERSE-Loot-DP-v11.zip`, which defines `bca:general/...` loot tables.
- `overrides/datapacks/COBBLEVERSE-DP-v31.zip`, whose structure NBT references custom tables including `bca:general/attic` and `bca:general/kitchen` for containers.

Both example tables declare `minecraft:chest`. In the structure datapack, `data/bca/structure/dark/structures/ravenspire_manor.nbt` contains a chest at relative position `[10, 10, 6]` using `bca:general/attic`; `data/bca/structure/dark/centers/center_the_farmers_market_dark.nbt` contains a chest at `[9, 1, 30]` using `bca:general/kitchen`. Their nested table references stay under `bca:item_groups/...` and `bca:support_tables/...`; neither example calls the other or a table following the automatic `chests/` convention. The attic table is also referenced by decorated pots. The runtime decorated-pot check prevents adding boosters in those pot calls while permitting its eligible chest calls; the ID and declared type alone could not distinguish them.

Those paths do not follow the automatic `chests/` convention. Pack authors using the verified release can opt in with:

```json
{
  "additionalBoosterLootTables": [
    "bca:general/attic",
    "bca:general/kitchen"
  ],
  "excludedBoosterLootTables": [],
  "debugBoosterLootTables": false
}
```

These are version-specific examples, not built-in compatibility entries or a claim about every Cobbleverse release. The default allowlist stays empty. No blanket `bca:general/` rule or dependency on Cobbleverse or another structure mod is added.

## Finding the right table

Enable `debugBoosterLootTables`, then restart the server or reload its loot tables. The diagnostic log reports every inspected ID, whether it is eligible, whether it matched automatically, by a verified exact exception or through configuration, its `chancePercent`, and whether a pool was injected. Explicit exclusions and protected paths/types have distinct reasons. For example, `injected=true` with `chancePercent=2.0` confirms a pool was installed with a 2% chance; it does not mean a booster has already been won. Logs describe table loading and injection, not individual player rolls. Disable the flag again once diagnostics are complete to keep normal logs quiet.

A loaded table is not proof that a structure uses it. To identify an actual container's table:

1. With operator permissions, inspect an unopened generated container using `/data get block <x> <y> <z> LootTable`. Loot generation can remove this field, so use a container whose loot has not yet been generated.
2. Inspect the structure mod's JAR or datapack. Minecraft 1.21.1 table resources use `data/<namespace>/loot_table/<path>.json`, with singular `loot_table`. For example, `data/bca/loot_table/general/attic.json` has ID `bca:general/attic`.
3. Verify references in structure NBT or the mod's source, including whether other reward sources call the same table. Check nested `minecraft:loot_table` entries too.

Every distinct eligible table can make its own roll. If a parent table invokes an eligible child, configuring both can produce separate booster chances. Select the intended table and use exclusions for eligible parents or children when necessary; deduplicating a single table's pool does not merge different tables' rolls.

At the default **2%**, an eligible roll has a **98% chance of no booster**. For independent rolls with one eligible pool each, the probability of no booster after `n` rolls is `0.98^n`: approximately **36.4% after 50** and **13.3% after 100**. The average is one booster per 50 eligible rolls, with no guarantee for any particular sequence. Inspecting a container's actual table and its injection diagnostic distinguishes selection problems from ordinary unsuccessful rolls without logging player loot.

## Reloads, existing worlds and datapacks

`/reload` rebuilds server loot tables and reapplies the currently loaded settings. Injection tracking follows the loaded table object rather than suppressing its ID forever, so a reloaded table can receive its pool again. `/reload` does not reread edits made directly to the MidnightLib configuration file; restart after file edits.

Existing worlds require no conversion. Already generated container items are not changed or rerolled. An unopened loot container uses the active table when its contents are generated. A replacement loot table from a normal datapack remains subject to the same selection and protection rules.

This patch does not add a separate CobblemonCards datapack allowlist. The existing configuration provides deterministic overrides without a second resource-loading mechanism or loader-specific reload ordering. It adds no third-party dependency. Fabric's resource source and NeoForge's load event behavior differ, so support for a separate data resource should be implemented only with matching reload and datapack behavior on both loaders.

## Validation performed on 18 October 2026

Under Java 21, `:common:test :fabric:build :neoforge:build --offline --console=plain` passed on the final code revision. All **139 shared tests** passed. The suite covers dungeon and Trial chest/barrel table IDs, the two exact dispenser exceptions, exclusions, protected sources, malformed configuration, persistence and legacy defaults, duplicate injection, reloaded table targets, and block/entity/fishing context safety. All 17 Cobblemon helper IDs have regression coverage alongside their eligible vanilla parent tables.

An isolated NeoForge 21.1.255 dedicated development server reached `Done`, loaded the saved test world again, reloaded resources and stopped cleanly with exit 0. No players connected. Its isolated configuration used a **100% test chance**; the shipped default remains **2%**. Assertions verified **28 container cases**, before and after `/reload`:

- Seeded chest, barrel and dispenser fixtures used the actual saved-`LootTable` unpacking path. Dungeon, Trial entrance, corridor barrel, intersection barrel, supply, reward-root chest, both exact dispensers and an allowlisted custom barrel each generated exactly one booster alongside original loot. The defined Trial intersection table was also exercised in a seeded fixture; this does not establish a natural caller for that table.
- The dungeon check initially exposed the extra roll in Cobblemon's nested helper. The final run confirmed exactly one booster with that helper protected, while ordinary Minecraft and Cobblemon loot remained present. Diagnostics confirmed all 17 helper tables were protected on startup and reload.
- The Trial pot table produced its original item without a booster. A custom table reused by a barrel and decorated pot produced an apple plus one booster in the barrel, and retained exactly its original apple in the pot. No container-overflow warning occurred.
- Command-driven contexts located at a vault or trial spawner retained original table rewards and generated no boosters. These exercise the source-block guard; no player unlocked a vault or completed a trial-spawner challenge.
- Diagnostics confirmed the two exact dispenser defaults were injected with the configured percentage and all seven protected Trial reward IDs received no booster pool, both before and after reload.

Earlier checks during the compatibility patch also verified ordinary, trapped and Ender Chest block-drop generation through `/loot ... mine`, including test block tables that referenced an eligible chest table. They produced no boosters. Configuration rewriting retained existing values and the new lists; malformed/protected entries warned and missing table IDs were harmless. Final automated tests retain these path/type/context and persistence checks.

The earlier Fabric dedicated bootstrap initialized the mod and reached its existing `eula=false` gate. It did not start a world or exercise loot generation. Neither launch changed EULA acceptance; NeoForge's development launch bypasses that gate. Dependency mixin warnings were present, and the isolated flat-world settings emitted a generator-settings warning; neither prevented NeoForge's checks. No changed-class loading or booster condition errors occurred. Local fixtures, logs and assertion summaries are retained under ignored `build/structure-loot-smoke/` paths.

These checks do not establish player interaction, multiplayer behavior, a full Cobbleverse runtime, naturally locating every tested structure in a new world, or upgrades of existing player installations. Inspecting the official artifacts establishes resource IDs and structure references only. Fabric runtime loot checks and target-modpack gameplay remain unperformed.

## Files changed

- `common/src/main/java/com/howlite/cobblemoncards/CobblemonCardsConfig.java`
- `common/src/main/java/com/howlite/cobblemoncards/loot/BoosterLootTableMatcher.java`
- `common/src/main/java/com/howlite/cobblemoncards/loot/BoosterChestLoot.java`
- `common/src/main/java/com/howlite/cobblemoncards/loot/BoosterChestLootCondition.java`
- `common/src/main/java/com/howlite/cobblemoncards/loot/BoosterPoolInjection.java`
- `fabric/src/main/java/com/howlite/cobblemoncards/fabric/FabricCobblemonCards.java`
- `neoforge/src/main/java/com/howlite/cobblemoncards/neoforge/NeoForgeCobblemonCards.java`
- `common/src/test/java/com/howlite/cobblemoncards/loot/BoosterLootTableMatcherTest.java`
- `common/src/test/java/com/howlite/cobblemoncards/loot/BoosterLootConfigTest.java`
- `common/src/test/java/com/howlite/cobblemoncards/loot/BoosterPoolInjectionTest.java`
- `common/src/test/java/com/howlite/cobblemoncards/loot/BoosterChestLootConditionTest.java`
- `common/src/main/resources/assets/cobblemon-cards/lang/en_us.json`
- `common/src/main/resources/assets/cobblemon-cards/lang/fr_fr.json`
- `README.md`, `README_MODRINTH.md`, and this guide.
