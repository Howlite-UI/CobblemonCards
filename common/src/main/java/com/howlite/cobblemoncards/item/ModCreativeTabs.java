package com.howlite.cobblemoncards.item;

import com.howlite.cobblemoncards.block.ModBlocks;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.custom.loot.BoosterLootTable;
import com.howlite.cobblemoncards.util.PlatformHelper;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ModCreativeTabs {

    private static final Random RANDOM = new Random();
    private static final String[] RARITIES = {"common", "uncommon", "rare", "epic", "legendary"};

    public static CreativeModeTab CARDS_TAB;
    public static CreativeModeTab BOOSTERS_TAB;

    public static void register() {
        CARDS_TAB = PlatformHelper.INSTANCE.createCreativeTab(
                "cards_tab",
                () -> new ItemStack(ModItems.BOOSTER_PACK),
                (displayParameters, output) -> {
                    // 1. Cartes & Boosters (Obtention & Récompenses)
                    output.accept(ModItems.BOOSTER_PACK);
                    output.accept(ModItems.GOD_PACK_TICKET);
                    output.accept(ModItems.CHAMPION_RIBBON);

                    // Création d'une carte avec des données aléatoires pour le menu créatif
                    List<String> pokemonIds = BoosterLootTable.getPokemonIds();
                    if (!pokemonIds.isEmpty()) {
                        String randomPokemon = pokemonIds.get(RANDOM.nextInt(pokemonIds.size()));
                        
                        ItemStack randomCard = new ItemStack(ModItems.CARD);
                        randomCard.set(ModDataComponents.CARD_DATA, new CardData(
                                randomPokemon,
                                RANDOM.nextBoolean(),
                                RARITIES[RANDOM.nextInt(RARITIES.length)],
                                com.howlite.cobblemoncards.util.CardStatUtil.randomStat(RANDOM),
                                (RANDOM.nextFloat() * 2.0f) - 1.0f, // Valeur entre -1.0 et 1.0
                                RANDOM.nextInt(11), // Grade entre 0 et 10
                                Optional.empty(),
                                Optional.empty()
                        ));
                        output.accept(randomCard);
                    }

                    // 2. Rangement & Stockage (Classeurs & Meuble)
                    output.accept(ModItems.LEATHER_BINDER);
                    output.accept(ModItems.IRON_BINDER);
                    output.accept(ModItems.GOLD_BINDER);
                    output.accept(ModItems.DIAMOND_BINDER);
                    output.accept(ModItems.NETHERITE_BINDER);
                    output.accept(ModItems.MASTER_ALBUM);
                    output.accept(ModBlocks.CARD_CABINET);

                    // 3. Équipement & Machines de tri
                    output.accept(ModBlocks.GRADING_STATION);
                    output.accept(ModItems.GRADING_STATION_BYPASS);
                    output.accept(ModBlocks.CARD_RECYCLER);
                    output.accept(ModBlocks.CARD_RESTORER);
                    output.accept(ModItems.INSTANT_DEX);
                    output.accept(ModItems.CARD_DEX);

                    // 4. Matériaux & Disques
                    output.accept(ModItems.CARD_DUST);
                    output.accept(ModItems.CARD_DUST_POUCH);
                    output.accept(ModBlocks.CARD_DUST_SACK);
                    output.accept(ModItems.CARD_STRUCTURE_DISK);

                    // 6. Hologrammes & Exposition
                    output.accept(ModBlocks.HOLO_PROJECTOR);
                    output.accept(ModBlocks.ADVANCED_HOLO_PROJECTOR);
                    output.accept(ModBlocks.MINI_HOLO_PROJECTOR);
                }
        );

        BOOSTERS_TAB = PlatformHelper.INSTANCE.createCreativeTab(
                "boosters_tab",
                () -> new ItemStack(ModItems.BOOSTER_PACK_FIRE),
                (displayParameters, output) -> {
                    // Generations
                    output.accept(ModItems.BOOSTER_PACK_GEN1);
                    output.accept(ModItems.BOOSTER_PACK_GEN2);
                    output.accept(ModItems.BOOSTER_PACK_GEN3);
                    output.accept(ModItems.BOOSTER_PACK_GEN4);
                    output.accept(ModItems.BOOSTER_PACK_GEN5);
                    output.accept(ModItems.BOOSTER_PACK_GEN6);
                    output.accept(ModItems.BOOSTER_PACK_GEN7);
                    output.accept(ModItems.BOOSTER_PACK_GEN8);
                    output.accept(ModItems.BOOSTER_PACK_GEN9);
                    
                    // Types
                    output.accept(ModItems.BOOSTER_PACK_FIRE);
                    output.accept(ModItems.BOOSTER_PACK_WATER);
                    output.accept(ModItems.BOOSTER_PACK_GRASS);
                    output.accept(ModItems.BOOSTER_PACK_ELECTRIC);
                    output.accept(ModItems.BOOSTER_PACK_ICE);
                    output.accept(ModItems.BOOSTER_PACK_FIGHTING);
                    output.accept(ModItems.BOOSTER_PACK_POISON);
                    output.accept(ModItems.BOOSTER_PACK_GROUND);
                    output.accept(ModItems.BOOSTER_PACK_FLYING);
                    output.accept(ModItems.BOOSTER_PACK_PSYCHIC);
                    output.accept(ModItems.BOOSTER_PACK_BUG);
                    output.accept(ModItems.BOOSTER_PACK_ROCK);
                    output.accept(ModItems.BOOSTER_PACK_GHOST);
                    output.accept(ModItems.BOOSTER_PACK_DRAGON);
                    output.accept(ModItems.BOOSTER_PACK_STEEL);
                    output.accept(ModItems.BOOSTER_PACK_FAIRY);
                    output.accept(ModItems.BOOSTER_PACK_DARK);
                    output.accept(ModItems.BOOSTER_PACK_NORMAL);
                }
        );
    }
}
