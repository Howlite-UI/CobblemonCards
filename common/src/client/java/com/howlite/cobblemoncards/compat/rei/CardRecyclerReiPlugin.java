package com.howlite.cobblemoncards.compat.rei;

import com.howlite.cobblemoncards.block.ModBlocks;
import com.howlite.cobblemoncards.compat.RecyclerRecipeInfo;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.world.item.ItemStack;

/**
 * Plugin REI pour le mod Cobblemon Cards.
 * <p>
 * Chargé par l'entrypoint {@code "rei_client"} sur Fabric et par un adaptateur annoté sur NeoForge.
 * Si REI n'est pas installé, cette classe n'est jamais chargée → aucun crash.
 */
public class CardRecyclerReiPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        // Enregistrer la catégorie
        registry.add(new CardRecyclerReiCategory());

        // Le bloc Card Recycler comme workstation (clic sur le bloc → affiche les recettes)
        registry.addWorkstations(
                CardRecyclerReiCategory.CATEGORY_ID,
                EntryStacks.of(new ItemStack(ModBlocks.CARD_RECYCLER))
        );
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        // Convertir chaque RecyclerRecipeInfo en CardRecyclerDisplay et l'enregistrer
        for (RecyclerRecipeInfo info : RecyclerRecipeInfo.buildAll()) {
            registry.add(new CardRecyclerDisplay(info));
        }
    }
}
