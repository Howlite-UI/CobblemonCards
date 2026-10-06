<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/addon_cobblemon_cards.png" alt="Cobblemon Cards Addon" width="600">
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/icon.png" alt="Cobblemon Cards Icon" width="120" />
</p>

<p align="center">
  <strong>An immersive and feature-rich Minecraft mod (Fabric & NeoForge) that introduces the ultimate Trading Card Game to the Cobblemon universe!</strong><br>
  <em>Collect, trade, grade, and proudly display your favorite Pokémon on gorgeous 3D cards complete with animated holographic shaders.</em>
</p>

<p align="center">
  <a href="https://minecraft.net"><img src="https://img.shields.io/badge/Minecraft-1.21.1-blue.svg?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft Version"></a>
  <a href="https://fabricmc.net"><img src="https://img.shields.io/badge/Loader-Fabric%20%2F%20NeoForge-lightgrey.svg?style=for-the-badge" alt="Fabric & NeoForge Loaders"></a>
  <a href="https://cobblemon.com"><img src="https://img.shields.io/badge/Cobblemon-Compatible-orange.svg?style=for-the-badge&logo=pokemon" alt="Cobblemon Compatible"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-CC0_1.0-blue.svg?style=for-the-badge" alt="License"></a>
  <a href="https://discord.gg/cmKcXaNnmy"><img src="https://img.shields.io/badge/Discord-Join%20Server-5865F2.svg?style=for-the-badge&logo=discord&logoColor=white" alt="Discord"></a>
</p>

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

## V2 beta — 2.0.0-beta.1

This prerelease targets **Minecraft 1.21.1**, **Java 21**, and **Cobblemon 1.8.1** on **Fabric and NeoForge**.

- **Trainer bonuses**: cards can carry an additional Experience, Catch Rate, or Shiny Chance bonus while keeping their primary stat.
- **Card Restorer**: higher target grades take longer; restoration continues after closing the GUI and saves its progress.
- **Readable configuration**: six MidnightLib tabs with section headings, individual stat multipliers, and English/French tooltips.
- **Spawn weighting**: equipped cards influence eligible Pokémon spawns while respecting Cobblemon's biome and spawn-bucket rules.
- **Updated dependencies**: both loaders use Accessories for equipped binders, with matching owo-lib and Kotlin runtimes.

[Read the V2 guide](https://github.com/Howlite-UI/CobblemonCards/blob/main/docs/V2_GUIDE.md) · [Custom Fakemon sprites](https://github.com/Howlite-UI/CobblemonCards/blob/main/docs/CUSTOM_FAKEMON_SPRITES.md) · [Full changelog](https://github.com/Howlite-UI/CobblemonCards/blob/main/CHANGELOG.md)

The automated builds and targeted checks pass. Dedicated-server multiplayer, upgrades of existing worlds, and large-collection performance still need playtesting before the stable V2 release.

## 🌟 Key Features

### 📦 Thematic & Generational Booster Packs

* **Over 20 distinct booster packs** to open using an interactive custom opening interface.
* **Generations 1 to 9**: Focus your collection on specific regions and generations!
* **Type-Themed Boosters**: Target your search with elemental packs containing only specific types (Fire, Water, Grass, Electric, Ghost, etc.).
* **God Pack Ticket**: A legendary item that guarantees your next booster pack will be an ultra-rare "God Pack"!

### 🎴 Collectible Pokémon Cards

* **Multiple Rarity Tiers**: *Common*, *Uncommon*, *Rare*, *Epic*, *Legendary*, and *Mythic*.
* **Visual Variants**: Normal cards and full-art **Shiny** (Chromatiques) variants with custom visual models.
* **Special Forms**: Complete integration of Regional variants (Alola, Galar, Hisui) and Mega Evolutions.
* **Fakemon Integration**: Add card drops, booster eligibility, and normal/shiny sprites for addon species with the included [datapack and resource-pack template](https://github.com/Howlite-UI/CobblemonCards/tree/main/templates/fakemon-card-integration).

### ✨ Dynamic Holographic Shaders & Visual Effects
Witness over **20 unique procedural holographic effects** powered by custom shaders that glisten and shift as you look around:
* *Foil Stars, Rainbow Prism, Plasma, Cosmic Constellation, Cyber Dust, Magical Wind...*
* Custom special-form effects like *Mega Vortex, Alolan Shore, Galarian Steam, Paldean Terastal, Distortion Rift, Time Gears, Spatial Crack, and Prism Stars...*

### 📖 Card Binders & Storage Cabinets

* **Tiered Binders**: Craft Leather, Iron, Gold, Diamond, Netherite, and the ultimate **Master Album**.
* **RPG Stats & Passive Bonuses**: Equip your binders in your Accessories slot! Slotted cards grant passive stat boosts (Mining Speed, Movement Speed, Attack Damage, Luck, Armor, Max Health, and custom wild spawn rate multipliers).
* **Card Cabinet**: A beautiful piece of furniture storing up to **12,000 cards** featuring built-in search, sorting, and filter controls.

### Trainer Bonuses & Stat Progression

Equip a binder in the **binder accessory slot** to apply the bonuses of the cards it contains. Trainer bonuses affect **Pokémon experience gains**, **catch rates**, and **wild shiny chances**. Cosmetic cards grant no bonuses, and the Master Album grants none unless `masterAlbumGivesStats` is enabled.

Grading increases card stat values by **3% per grade**, up to **30% at grade 10**. By default, reaching grade **9 or higher** during grading—or crossing that threshold during restoration—gives a **50% chance** of adding a trainer bonus. Newly generated Legendary, Mythic, or Shiny cards also have a **5% chance** of carrying one. These bonuses preserve the primary stat and use `trainerStatLuckyValueMultiplier` (default **0.25**) to scale their value.

A **+600% Shiny Chance** bonus means **7× the base chance**. With Cobblemon's `shinyRate` set to `8192`, that is about **1 shiny per 1,170 new Pokémon**, not a guarantee. The default `maxShinyBoostDivisor = 10` caps the multiplier at **10×**. Spawn-type bonuses change relative spawn weights rather than the number of Pokémon spawned.

### 🔬 Grading, Restoration & Recycling

* **Grading Station**: Grade an ungraded card from 1 to 10. Each grade adds 3% to its stat values; high grades can unlock an additional trainer bonus.
* **Card Restorer**: Improve an already graded card to a higher target grade using Cobblecard Dust. The default duration rises from **3 seconds at target grade 2** to **27 seconds at target grade 10**.
* **Persistent processing**: Restoration continues with the GUI closed while the chunk is loaded. Progress is saved and resumes after reloading the world or chunk. Dust is spent once, at completion.
* **Card Recycler**: Recycle duplicate or unwanted cards into **Cobblecard Dust**, also used by the grading station and structure disks.

### 📡 Instant-Dex Tool & Structure Disks

* **Instant-Dex Scanner**: A handheld utility tool to scan wild Pokémon in the wild.
* **Card Structure Disks**: Load them with card dust, lock in a target species, scan them in the wild, and print a physical card once compilation hits 100%!

### 🌌 3D Holographic Projectors

* Showcase your trophy cards in your base using regular and advanced **Holo Projectors**.
* **6 Display Modes**: Continuous Rotation, Face Player, Dynamic (Spin & Face), Fixed, Flat, and Simple Bobbing.
* The advanced projector lets you slot in and sequence a moving gallery of up to **27 cards**!

## ⚙️ Configuration (MidnightLib)

<details>
<summary>⚙️ Click to expand configuration options</summary>

Use **MidnightLib's configuration screen** or edit `config/cobblemon-cards.json` while the game/server is stopped. Mod Menu provides access to the configuration screen on Fabric. In multiplayer, gameplay values are read from the **server's configuration**; editing only the client's file does not change server gameplay.

The menu groups settings into six tabs: **General**, **Player**, **Spawning**, **Trainer**, **Machines**, and **Binders**. Colored section headings and English/French tooltips explain related options. Existing configuration keys and values are preserved.

These are the main settings; the in-game screen also exposes per-stat multipliers and binder capacities.

| Config Option | Default Value | Range / Type | Description |
| :--- | :--- | :--- | :--- |
| `enableCardStats` | `true` | Boolean | Master switch for passive card bonuses. |
| `globalStatMultiplier` | `10.0` | `0.0`–`100.0` | Global multiplier for passive bonuses. |
| `enableTrainerStats` | `true` | Boolean | Apply Experience, Catch Rate, and Shiny Chance bonuses. |
| `trainerStatMultiplier` | `1.0` | `0.0`–`100.0` | Additional multiplier for trainer bonuses. |
| `maxExpBoostMultiplier` | `5.0` | `1.0`–`100.0` | Maximum experience multiplier. |
| `maxCatchBoostMultiplier` | `5.0` | `1.0`–`100.0` | Maximum catch-rate multiplier. |
| `maxShinyBoostDivisor` | `10.0` | `1.0`–`100.0` | Maximum multiplier of the base shiny chance. |
| `maxSpawnBoostMultiplier` | `100.0` | `0.0`–`10000.0` | Cap for spawn-weight multipliers. |
| `enableEggGroupStats` | `false` | Boolean | Enable egg-group spawn bonuses. |
| `enableEvYieldStats` | `false` | Boolean | Enable EV-yield spawn bonuses. |
| `recyclerProcessTime` | `40` | `1`–`1200` | Recycling duration in ticks. |
| `gradingStationProcessTime` | `100` | `1`–`12000` | Grading duration in ticks. |
| `gradingStationDustCost` | `5` | `0`–`64` | Dust used to grade a card. |
| `restorerBaseCost` | `5` | `1`–`1000` | Base factor for the restoration dust cost. |
| `restorerBaseProcessTime` | `60` | `1`–`72000` | Restoration ticks for target grade 2. |
| `restorerProcessTimePerGrade` | `60` | `0`–`72000` | Extra ticks per target grade above 2; 0 gives a fixed duration. |
| `masterAlbumGivesStats` | `false` | Boolean | Allow the Master Album to grant passive bonuses. |
| `godPackTicketChance` | `1.0` | `0.0`–`100.0` | Ticket drop chance when opening a booster (%). |
| `cardDropChance` | `1.0` | `0.0`–`100.0` | Card drop chance from defeated/captured Pokémon (%). |
| `enableBoosterChestSpawn` | `true` | Boolean | Add classic boosters to structure chests. |
| `boosterChestSpawnChance` | `2.0` | `0.0`–`100.0` | Chance to find a classic booster in a structure chest (%). |

Restoration duration uses the server settings: `restorerBaseProcessTime + (targetGrade - 2) × restorerProcessTimePerGrade`. At 20 ticks per second, the defaults give **3 seconds for grade 2**, **12 seconds for grade 5**, and **27 seconds for grade 10**. Duration depends on the target grade. New timing settings apply to new operations; an active restoration keeps its original duration and dust cost.

Removing or replacing the card cancels restoration. Removing required dust pauses progress until enough dust is available again. The GUI shows the remaining time when reopened; unloaded chunks do not process restorations.

</details>

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

## 📦 Modpack Integration & Standalone Notice

> [!NOTE]
> **Designed for Modpacks:** Cobblemon Cards can be used as a standalone Cobblemon addon or integrated into a modpack. Configurable drop rates, machine costs, and passive bonuses let pack authors fit card collection into quests and progression systems.

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

## 🛠️ Required Dependencies

To run **Cobblemon Cards**, use **Minecraft 1.21.1**, **Java 21**, and **Cobblemon 1.8.1**. Install the dependencies for your loader; Fabric and NeoForge JARs are separate downloads. This branch targets Cobblemon 1.8.x.

### 🪶 For Fabric Users

| Mod | Version | Purpose |
| :--- | :--- | :--- |
| **Fabric Loader** | `0.19.5` | Mod loader |
| **Fabric API** | `0.116.17+1.21.1` | Core Fabric library |
| **Fabric Language Kotlin** | `1.14.1+kotlin.2.4.20` | Kotlin runtime |
| **Cobblemon** | `1.8.1+1.21.1` | Core Pokémon mod |
| **Architectury API** | `13.0.11` | Cross-platform compatibility library |
| **Cloth Config** | `15.0.140` | Configuration UI dependency |
| **Accessories** | `1.1.0-beta.53+1.21.1` | Accessory slots for binders and passive bonuses |
| **owo-lib** | `0.12.15.4+1.21` | Runtime library required by Accessories |
| **MidnightLib** | `1.9.3` (bundled) | Categorized configuration screen; no separate download needed |

### 🛠️ For NeoForge Users

| Mod | Version | Purpose |
| :--- | :--- | :--- |
| **NeoForge** | `21.1.255` | Mod loader |
| **Kotlin for Forge** | `5.12.0` | Kotlin runtime for Cobblemon |
| **Cobblemon** | `1.8.1+1.21.1` | Core Pokémon mod |
| **Architectury API** | `13.0.11` | Cross-platform compatibility library |
| **Cloth Config** | `15.0.140` | Configuration UI dependency |
| **Accessories** | `1.1.0-beta.53+1.21.1` | Accessory slots for binders and passive bonuses |
| **owo-lib** | `0.12.15.5-beta.1+1.21` | Runtime library required by Accessories (includes its helper libraries) |
| **MidnightLib** | `1.9.3+1.21.1-neoforge` | Categorized configuration screen |

These are the dependency versions used by this beta, centralized in `gradle.properties`. Binders use Accessories on both loaders; Trinkets and Cardinal Components are not required by this addon.

> [!TIP]
> **Optional recipe viewers:** JEI `19.51.0.418`, REI `16.0.799`, and EMI `1.1.24+1.21.1` have integrations for the Card Recycler. Install a viewer for your loader if you want to browse its recycling recipes in-game.

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

## 🚀 Installation Guide

1. Install **Java 21** and **Fabric Loader 0.19.5** or **NeoForge 21.1.255** for Minecraft `1.21.1`.
2. Grab the dependencies listed above and place them into your `.minecraft/mods` folder.
3. Download or compile the **Cobblemon Cards** `.jar` file and drop it into the `mods` folder.
4. Launch the game and start your collection!

### Upgrading from 1.x

1. Keep a backup of your existing world and configuration before trying the beta.
2. Replace the old Cobblemon Cards JAR with **one** `2.0.0-beta.1` JAR for your loader, and update Cobblemon and the dependencies listed above. Other Cobblemon addons must also support Cobblemon 1.8.1.
3. Keep `config/cobblemon-cards.json`: existing values are retained, including settings that disable bonuses. Newly added settings are written during startup.
4. Existing cards keep their stored stats. Older cards without an additional trainer stat and binders using the legacy container component remain readable. Opening a world does not reroll its cards.


<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

## 💻 For Developers: Compiling from Source

If you want to modify the source code or build the mod manually:

1. Clone this repository:
   ```bash
   git clone https://github.com/Howlite-UI/CobblemonCards.git
   cd CobblemonCards
   ```
2. Use Java 21. The Gradle wrapper uses Gradle 9.5.1, and the Java/Kotlin toolchains target Java 21. Gradle can download a matching JDK through Foojay if necessary.
3. Build both loader versions:
   ```bash
   ./gradlew :fabric:build :neoforge:build
   ```
   On Windows, use `gradlew.bat` instead of `./gradlew`. The installable JARs for this beta are:

   - `fabric/build/libs/cobblemon-cards-fabric-2.0.0-beta.1.jar`
   - `neoforge/build/libs/cobblemon-cards-neoforge-2.0.0-beta.1.jar`

   Use the JAR matching your loader. Common, dev, and sources JARs are development artifacts.
4. Launch a development client with `./gradlew :fabric:runClient` or `./gradlew :neoforge:runClient`. Both use Java 21.

Fabric development runs explicitly include the GraalJS, ICU, and MongoDB libraries bundled by Cobblemon. Their versions match the [official Cobblemon addon template](https://gitlab.com/cable-mc/cobblemon-mdks/-/blob/master/fabric-java/build.gradle.kts); NeoForge discovers these libraries from Cobblemon's nested JARs. Player installations receive them through Cobblemon itself.

Validation covers both loader builds, packaged classes/resources, and dependency metadata. Both loaders initialized the addon and Cobblemon's Showdown service on Java 21; a NeoForge development server also loaded a test world and all 1,025 species. Targeted headless checks passed for restoration (38 checks) and the Cobblemon 1.8.1 shiny event (15 checks). These checks do not replace multiplayer or performance playtests. Client gameplay has been tried during development, but a full release validation across both loaders is still pending.

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

## 🤫 Easter Eggs

<details>
<summary>🔍 Click here to reveal the mod's secrets! (Spoilers)</summary>

### 👤 Custom Player Cards

* Using the **Instant-Dex Scanner** on another player while having a **Card Structure Disk** in your inventory will instantly consume the disk and print a **Mythic Grade 10 Cosmetic Card** featuring that player's Minecraft skin!
* *Note: Player cards are purely cosmetic and do not grant passive stat boosts.*

### 👾 The Legendary MissingNo.

* If you scan Pokémon during a **Full Moon** at night while affected by the **Darkness** effect, the fabric of reality glitches! 
* You will hear a haunting glitch scream and receive the legendary **Mythic Grade 10 MissingNo.** card, featuring a custom glitched pixel art texture and special stats!

### 👻 Ghost of Lavender Town

* Scan Gastly, Haunter, Gengar, Cubone, or Marowak near midnight (world time ticks 16000 to 20000) while standing on Soul Sand or Soul Soil.
* Yields a **Mythic Grade 10 Cosmetic Card** featuring the spooky Lavender Town Ghost!

### 🌟 Divine Bidoof

* Scan a wild Bidoof while holding a Golden Apple or an Enchanted Golden Apple in your off-hand.
* Yields the legendary **Mythic Grade 10 Cosmetic Card** of Divine Bidoof!

### 💎 Crystal Onix

* Scan a wild Onix while holding an Amethyst Shard in your off-hand.
* Yields a **Mythic Grade 10 Cosmetic Card** featuring the stunning Crystal Onix!

### 🖤 Shadow Lugia

* Scan a wild Lugia during a Thunderstorm while affected by the Wither status effect.
* Yields a **Mythic Grade 10 Cosmetic Card** featuring the corrupted Shadow Lugia!

### 🏳️‍⚧️ Pride Sylveon

* Scan a wild Sylveon while holding Pink Dye, Light Blue Dye, or White Dye (the colors of the Trans pride flag) in your off-hand.
* Yields a **Mythic Grade 10 Cosmetic Card** of Pride Sylveon!

### 💝 You & Mew

* Scan a wild Mew while carrying a custom Player Card (obtained from scanning another player) in your inventory.
* Consumes both the Player Card and the structure disk, and yields the ultimate **Mythic Grade 10 Cosmetic Card** representing you and Mew!

### 🎵 Jukebox Holo-Music

* Placing a **Holo Projector** or **Advanced Holo Projector** directly on top of a **Jukebox** and slotting in a card will trigger custom Pokémon music tracks!
* The track played adapts dynamically based on the card's rarity, shiny status, or stats:
  * **Mythic**: *Soul Heart*
  * **Legendary & Shiny**: *Battle! Necrozma*
  * **Legendary**: *Cynthia*
  * **Other Shiny**: *Battle! Zinnia*
  * **Fire or Attack stats**: *Battle! Team Plasma*
  * **Water or Speed stats**: *Route 209*
  * **Grass or Health stats**: *Littleroot Town*
  * **Ice or Armor stats**: *Snowpoint City*
</details>

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

## 📜 License

This project is licensed under the CC0 1.0 Universal License - see the [LICENSE](LICENSE) file for details.

<p align="center">
  <img src="https://raw.githubusercontent.com/Howlite-UI/CobblemonCards/main/common/src/main/resources/assets/cobblemon-cards/textures/graphics/cobblemon_divider.png" alt="Divider" />
</p>

*Made with ❤️ by Pokemon card enthusiasts in Minecraft.*
