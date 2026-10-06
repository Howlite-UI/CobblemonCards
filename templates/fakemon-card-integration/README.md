# Cobblemon Cards — Fakemon integration template

Template for Minecraft **1.21.1**, Cobblemon **1.8.1**, and Cobblemon Cards **2.0.0-beta.1**. It works with Fabric and NeoForge.

This download contains two packs because Minecraft separates server data from client assets:

- `datapack/` goes in `<world>/datapacks/`;
- `resourcepack/` goes in `.minecraft/resourcepacks/` on every client, or is distributed as the server resource pack.

Ready-to-install archives:

- [Fakemon datapack template](downloads/CobblemonCards-Fakemon-Datapack-Template-1.21.1.zip)
- [Fakemon resource-pack template](downloads/CobblemonCards-Fakemon-ResourcePack-Template-1.21.1.zip)

Use the Datapack ZIP on the server and the ResourcePack ZIP on clients. Do not wrap their contents in another parent folder; `pack.mcmeta` must remain at the root of each ZIP.

Replace every occurrence of `examplemon` with the lowercase internal `name` of the species from its Cobblemon addon. Do not use `addon_namespace:examplemon`.

1. Edit `datapack/data/cobblemon-cards/fakemon_cards/example.json`.
2. Rename the two `examplemon.png` files and replace their artwork.
3. Keep each PNG at 48×32: artwork in the top-left 40×30, transparent padding of 8 px right and 2 px bottom.
4. Install and enable both packs. Run `/reload` for the datapack and `F3+T` for edited client textures.
5. Test normal and shiny cards with the commands in the [complete guide](../../docs/CUSTOM_FAKEMON_SPRITES.md#4-test-the-integration).

The included PNGs are visible placeholders copied from Cobblemon Cards. Replace them before publishing your pack.

## Français

Cette template contient deux packs, car Minecraft sépare les données serveur des textures client :

- place `datapack/` dans `<monde>/datapacks/` ;
- place `resourcepack/` dans `.minecraft/resourcepacks/` chez chaque joueur, ou distribue-le comme resource pack du serveur.

Remplace partout `examplemon` par le champ interne `name` de l'espèce, en minuscules et sans namespace. Par exemple, utilise `examplemon`, pas `mon_addon:examplemon`.

Les PNG doivent faire **48×32 pixels** : dessine dans les 40×30 pixels en haut à gauche et laisse transparents les 8 pixels à droite et les 2 pixels du bas. Fournis toujours les variantes normale et shiny.

Consulte le [guide complet](../../docs/CUSTOM_FAKEMON_SPRITES.md) pour le fonctionnement, les commandes de test et le dépannage.
