package com.howlite.cobblemoncards.compat.emi;

import com.howlite.cobblemoncards.block.ModBlocks;
import com.howlite.cobblemoncards.compat.RecyclerRecipeInfo;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;

/**
 * Plugin EMI pour le mod Cobblemon Cards.
 * <p>
 * Chargé par l'entrypoint {@code "emi"} sur Fabric et par annotation sur NeoForge.
 * Si EMI n'est pas installé, cette classe n'est jamais chargée → aucun crash.
 */
@EmiEntrypoint
public class CardRecyclerEmiPlugin implements EmiPlugin {

    /** Instance partagée de la catégorie — référencée par CardRecyclerEmiRecipe */
    public static final CardRecyclerEmiCategory CATEGORY = new CardRecyclerEmiCategory();

    @Override
    public void register(EmiRegistry registry) {
        // 1. Enregistrer la catégorie
        registry.addCategory(CATEGORY);

        // 2. Enregistrer le workstation (le bloc Card Recycler)
        //    Cliquer sur le bloc dans l'inventaire ou dans le monde affichera ces recettes
        registry.addWorkstation(CATEGORY, EmiStack.of(new ItemStack(ModBlocks.CARD_RECYCLER)));

        // 3. Enregistrer les 12 recettes d'exemple (6 raretés × normal/shiny)
        for (RecyclerRecipeInfo info : RecyclerRecipeInfo.buildAll()) {
            registry.addRecipe(new CardRecyclerEmiRecipe(info));
        }
    }
}
