# Custom Fakemon card sprites

Cobblemon Cards can generate cards for species registered by other Cobblemon addons. The integration has two separate parts:

- a **datapack** on the server authorizes selected species for card drops and booster packs;
- a **resource pack** on every client supplies the normal and shiny card sprites.

A datapack cannot provide client textures by itself. For multiplayer, distribute the resource pack to every player or configure it as the server resource pack.

The ready-to-copy files are in [`templates/fakemon-card-integration`](../templates/fakemon-card-integration/README.md).

## 1. Find the species name

Use the species' internal Cobblemon `name`, converted to lowercase. This is usually the `name` field in the addon species JSON. For example, a species declared with `"name": "Examplemon"` uses:

```text
examplemon
```

Do not use the resource identifier (`my_addon:examplemon`) and do not use its translated display name. The datapack entry and both PNG filenames must use the same internal name.

Cobblemon Cards normalizes sprite names to lowercase, removes dots, converts spaces to hyphens, and also tries a hyphenated fallback for underscores. Using lowercase letters, numbers, and underscores for the species name avoids ambiguity.

## 2. Create the server datapack

Minecraft 1.21.1 uses data pack format `48`. Put this file at:

```text
<world>/datapacks/your-pack/data/cobblemon-cards/fakemon_cards/your-pack.json
```

The `cobblemon-cards` namespace contains a hyphen on Fabric and NeoForge.

```json
{
  "replace": false,
  "species": [
    "examplemon",
    "anothermon"
  ]
}
```

Keep `replace` set to `false` when combining several addons. The whitelist is useful even when `allowFakemonCards` is `false`: listed species can still drop cards and appear in boosters. If `allowFakemonCards` is `true`, every registered addon species is eligible, so the whitelist becomes optional.

Run `/reload` after changing the JSON. A successful reload writes this line to the server log:

```text
[CobblemonCards] Fakemon whitelist reloaded - N species whitelisted.
```

The datapack does not register a Pokémon. Its addon must already have registered the species with Cobblemon.

## 3. Create the client resource pack

Minecraft 1.21.1 uses resource pack format `34`. Add both files:

```text
assets/cobblemon-cards/textures/item/cards/pokemon/regular/examplemon.png
assets/cobblemon-cards/textures/item/cards/pokemon/shiny/examplemon.png
```

Each PNG must be **48×32 pixels**. Draw the sprite inside the **40×30 area at the top left**, leaving 8 transparent pixels on the right and 2 transparent pixels at the bottom:

```text
┌────────────────────────────────────────┬────────┐
│                                        │        │
│          visible area: 40×30           │  8 px  │
│                                        │ padding│
├────────────────────────────────────────┼────────┤
│             2 px padding                        │
└─────────────────────────────────────────────────┘
                 complete PNG: 48×32
```

The renderer deliberately crops to the 40×30 area. A plain 40×30 PNG is therefore not valid with the current renderer. Transparent backgrounds give the best result over card backgrounds and holographic effects.

Provide both variants. If the normal or shiny file is missing, Cobblemon Cards displays the matching Substitute fallback instead.

Put the resource pack in `.minecraft/resourcepacks`, enable it, then use `F3+T` after editing a PNG. Every multiplayer client that needs to see the sprites must have the resource pack enabled.

## 4. Test the integration

After enabling both packs, an operator can create a test card with:

```mcfunction
/cobblecard give @s examplemon false common movement_speed 1 none none
/cobblecard give @s examplemon true common movement_speed 1 none none
```

Replace `examplemon` with the actual internal species name. The command rejects names that are not registered in Cobblemon, which is also a useful way to detect a wrong identifier.

Then verify all relevant render locations: inventory, card inspection, binder, cabinet, booster opening, and holo projector.

## Troubleshooting

| Symptom | Cause to check |
| :--- | :--- |
| Substitute appears | Resource pack is disabled, PNG path/name is wrong, or the requested normal/shiny variant is missing. |
| Sprite is cropped or stretched | PNG is not 48×32 or the artwork extends outside the top-left 40×30 area. |
| Species never drops and is absent from boosters | Wrong internal `name`, datapack disabled, JSON path wrong, or neither whitelist nor `allowFakemonCards` permits it. |
| `/reload` reports 0 whitelisted species | Use `data/cobblemon-cards/fakemon_cards/*.json`; the namespace must contain the hyphen. |
| Other players see Substitute | They also need the resource pack; server datapacks do not transmit assets automatically. |
| Command says the Pokémon is invalid | The Fakemon addon did not register that name, or the resource identifier/display name was used instead of the internal species name. |
