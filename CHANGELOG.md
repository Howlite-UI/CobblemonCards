# 📝 Changelog - Cobblemon Cards

All major changes brought to **Cobblemon Cards** in this update.

---

## ⚙️ Unreleased (Spawning Overhaul, Stats Additions)

### 🌱 Spawning Logic Rework
* **Weight-based spawn boosts**: Spawn stats now influence the **weights of Pokémon that *could* spawn** (`BinderSpawnModifier` hooking into Cobblemon's `SpawningInfluence` / `affectWeight`), instead of forcefully transforming entities *after* they had already spawned.
  * Plays much nicer with other Cobblemon addons and no longer breaks game balance.
  * Respects spawn buckets and biome-specific spawn rules.
  * Spawn boosts are capped by the new `maxSpawnBoostMultiplier` config.
  * The best of the primary/secondary elemental type multipliers is applied to each species.
  * The equipped binder is re-scanned every 40 ticks instead of on every spawn attempt.
  * Even works with Pokenavs!!
  
### 🎛️ Config Additions
* **Granular multipliers**: `globalStatMultiplier`, `playerStatMultiplier`, `spawnBoostStatMultiplier`, `maxSpawnBoostMultiplier`.
* **Per-stat multipliers**: a dedicated multiplier for every player/vanilla-attribute stat — `miningSpeedStatMultiplier`, `movementSpeedStatMultiplier`, `attackDamageStatMultiplier`, `attackSpeedStatMultiplier`, `luckStatMultiplier`, `armorStatMultiplier`, `maxHealthStatMultiplier`, `cardDropChanceStatMultiplier`. These stack on top of `globalStatMultiplier` and `playerStatMultiplier`.
* **Fix**: MidnightLib no longer skips writing newly added config values when a config file already exists.

### 🧮 Accurate Stat Tooltips
* Tooltips and GUIs now reflect how each stat is applied: **flat** stats (Max Health, Armor, Luck, Mining Speed → `ADD_VALUE`) display as `+5.0`, while **percentage** stats (Movement Speed, Attack Damage, Attack Speed, Card Drop Chance, spawn boosts → `ADD_MULTIPLIED_BASE`) display as `+5.0%`.
* The application mode and modifier maths are now centralized in `CardStatUtil` for every platform.
* A config option exists to display non-formatted values, like in previous versions

### 🛠️ Critical Fix — Card Cabinet data loss
* **Card Cabinets no longer lose their contents on restart.** Introduced in 1.0.5; **1.0.4 and earlier are unaffected** and still load normally.
* Load failures are now logged with the offending NBT instead of being swallowed, and a failure to serialise a single stack can no longer abort the whole chunk save.
* Fixed `ItemStack` instances being shared between a cabinet's container and the `BINDER_CONTENTS` component when broken or placed.


---

## 🚀 Version 1.0.1 (Multiloader & Easter Eggs Update)

### ⚙️ Multiloader Architecture (Fabric & NeoForge)
* **Architectury Migration**: Complete separation of the project into `common`, `fabric`, and `neoforge` modules.
* **NeoForge Support**: The mod now runs natively on NeoForge (Minecraft 1.21.1).
* **Accessory Management**: Native support for **Trinkets** for Fabric players and **Accessories** for NeoForge players to equip card binders.
* **Asset Mutualization**: Moved all textures, models, and localizations to the common module (`common`).

### 🤫 Secret Easter Egg Cards (Mythic Cosmetics)
Added **6 new purely cosmetic mythic cards** (perfect Grade 10, no passive stats, non-recyclable) with unique acquisition conditions using the **Instant-Dex**:
* **Ghost of Lavender Town (`ghost`)**: Scan a ghost-type Pokémon (Gastly, Haunter, Gengar, Cubone, Marowak) near Midnight (Ticks 16000-20000) while standing on *Soul Sand* or *Soul Soil*.
* **Divine Bidoof (`god_bidoof`)**: Scan a wild Bidoof while holding a **Golden Apple** or an **Enchanted Golden Apple** in your off-hand.
* **Crystal Onix (`crystal_onix`)**: Scan a wild Onix while holding an **Amethyst Shard** in your off-hand.
* **Shadow Lugia (`shadow_lugia`)**: Scan a wild Lugia during a **Thunderstorm** while suffering from the **Wither** status effect.
* **Pride Sylveon (`pride_sylveon`)**: Scan a wild Sylveon while holding a color from the Trans pride flag (**Pink Dye**, **Light Blue Dye**, or **White Dye**) in your off-hand.
* **You & Mew (`you_and_mew`)**: Scan a wild Mew while carrying a custom **Player Card** (obtained by scanning another player) in your inventory.

### 💿 Card Structure Disk Improvements (`card_structure_disk`)
* **Stack Size Limit**: Reduced the maximum stack size from 64 to **1** to reflect the value of each unique disk.
* **Pouch and Sack Support**:
  * You can now charge the disk using **Card Dust Pouches** (9 dust) and **Card Dust Sacks** (81 dust).
  * **Simple Right-Click**: Consumes a single unit of the smallest available resource.
  * **Shift + Right-Click**: Smart-consumes sacks, pouches, then individual dusts to top-up the disk (capped at 1000).

### 🐛 Bug Fixes
* **Ghost of Lavender Town Fix**: Corrected block detection under the player. Sinking into Soul Sand changed the player's Y coordinate floored value; the mod now checks both current block position and below for perfect detection.
* **Nickname Fix**: Replaced nickname checks (for Bidoof & Sylveon) with off-hand item checks, as wild Pokémon cannot be nicknamed and scanning player-owned Pokémon is disabled for gameplay balancing.
