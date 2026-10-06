# Changelog — Cobblemon Cards

Changes by version. The V2 entry describes the prepared beta; it is not a stable-release announcement.

## 2.0.0-beta.1 — V2 beta

**Target:** Minecraft 1.21.1, Java 21, Cobblemon 1.8.1; Fabric and NeoForge.

### Added

- Additional **Experience**, **Catch Rate**, and **Shiny Chance** trainer bonuses without replacing a card's primary stat.
- Trainer-bonus acquisition through high-grade grading/restoration and a configurable lucky chance on newly generated Legendary, Mythic, or Shiny cards.
- Grade-dependent restoration durations: 3 seconds at target grade 2, 12 seconds at grade 5, and 27 seconds at grade 10 with the default settings.
- Restoration settings in MidnightLib: `restorerBaseProcessTime` and `restorerProcessTimePerGrade`.
- Egg-group and EV-yield spawn bonuses, each disabled by default and configurable separately.
- A V2 gameplay/configuration guide and upgrade instructions in both READMEs.
- A documented datapack/resource-pack template for custom Fakemon card sprites.

### Changed

- Migrated Fabric and NeoForge builds to **Cobblemon 1.8.1**, with compatible loader, Kotlin, Accessories, owo-lib, and supporting library versions centralized in `gradle.properties`.
- Both loaders use **Accessories** to equip binders; Trinkets and Cardinal Components are not required by this addon.
- Restoration is processed on the server while its chunk is loaded, continues after the GUI closes, and saves its progress. An active operation keeps its initial duration and cost; dust is spent once at completion.
- Removing/replacing the card cancels restoration. Removing required dust pauses it until the machine is refilled.
- MidnightLib configuration is organized into **General**, **Player**, **Spawning**, **Trainer**, **Machines**, and **Binders**, with colored headings and English/French labels and tooltips. Existing setting keys and values are preserved.
- Global, stat-family, and individual stat multipliers allow separate balancing of player, spawning, and trainer bonuses. Spawn, experience, catch, and shiny multipliers have configurable caps.
- Spawn bonuses modify eligible Pokémon's **spawn weights** before entity creation, preserving biome rules and spawn buckets. The stronger of a species' two elemental-type boosts is used; equipped binders are scanned every 40 ticks.
- Flat player stats display without a percentage sign; percentage bonuses retain it. Formatting and application calculations share `CardStatUtil`.
- Grading boosts card stat values by **3% per grade**, up to **30% at grade 10**.
- Release JARs identify their loader: `cobblemon-cards-fabric-2.0.0-beta.1.jar` and `cobblemon-cards-neoforge-2.0.0-beta.1.jar`.

### Fixed

- Extended the Card Restorer's card hitbox **2 pixels downward**.
- Added missing configuration translations and ensured newly introduced configuration entries are written alongside existing settings.
- Fixed Card Cabinet contents disappearing after restart in the affected 1.0.5 code. Previously lost cards cannot be recovered by this change; 1.0.4 and earlier saves remain readable.
- Cabinet serialization failures are logged; one invalid stack no longer aborts saving the entire cabinet. Breaking/placing a cabinet no longer shares mutable stack instances with its stored contents component.
- Corrected experience-bonus multiplication so fractional boosts are applied before rounding.
- Fakemon datapack reloads now refresh the booster species pool even when one entry is replaced by another and the whitelist size stays unchanged.

### Upgrade notes

- Update Cobblemon and the dependencies listed in [README.md](README.md) for the chosen loader. Other Cobblemon addons need their own 1.8.1-compatible versions.
- Existing configuration is retained: `enableCardStats = false` still disables passive bonuses. Existing cards keep their stored stats.
- Existing cards without additional trainer fields and binders using the legacy container component remain readable.
- Shiny bonuses multiply the base probability: **+600% = 7×**. With `shinyRate = 8192`, this gives approximately **1/1170 per new Pokémon**. The default 10× cap means bonuses beyond +900% do not further increase the chance.
- See [the V2 guide](docs/V2_GUIDE.md) for restoration timing, trainer-bonus acquisition, and configuration.

### Validation and beta scope

- Both loader builds and their packaged metadata/resources have been checked during the migration.
- Both loaders initialized the addon and Cobblemon's Showdown service on Java 21. A NeoForge development server loaded a test world and all 1,025 species.
- Targeted headless checks passed for restoration (**38**) and the Cobblemon 1.8.1 shiny event (**15**).
- A full two-player dedicated-server session, existing-world upgrade playtests, and large-collection performance checks remain pending before declaring V2 stable.

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
