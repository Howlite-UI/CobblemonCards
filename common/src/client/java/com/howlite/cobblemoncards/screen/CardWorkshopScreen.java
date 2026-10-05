package com.howlite.cobblemoncards.screen;

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.ModItems;
import com.howlite.cobblemoncards.network.GenerateCardPayload;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CardWorkshopScreen extends Screen {

    private static final List<String> RARITIES = Arrays.asList("common", "uncommon", "rare", "epic", "legendary");
    
    private static final List<String> BACKGROUNDS = Arrays.asList(
            "skybg2", "skybg3", "skybg4", "skybg5", "rockbg1", "grassbg1", "grassbg2", 
            "swampbg1", "waterbg1", "forestbg1", "water_anim", "lava_anim", "balatro_swirl", 
            "geometric_pulse", "plasma_bg", "starfield_anim", "cloud_scroll", "neon_grid", "toxic_sludge", 
            "matrix_code", "fire_embers", "crystal_cave", "sandstorm",
            "aurora_borealis", "deep_ocean", "void_rift", "golden_sunset",
            "cherry_blossom_wind", "cyber_city", "ancient_ruins", "frozen_tundra",
            "rainbow_highway", "plasma_storm", "galactic_supernova", "water2", 
            "mega_energy", "alola_beach", "hisui_ancient", "galar_industrial", "paldea_crystal",
            "distortion_rift", "dreamscape", "magma_chamber", 
            "stained_glass", "fluid_marble", "fossilized_amber", "none"
    );

    private static final List<String> EFFECTS = Arrays.asList(
            "flow", "glint", "noise", "foil_stars", 
            "holo_lines", "holo_pulse", "holo_rainbow", "holo_sparkle",
            "holo_runes", "holo_circuit", "holo_bubbles", 
            "holo_shatter", "holo_ripple", "holo_scanline",
            "holo_prism", "holo_aurora", "holo_vortex", "holo_lightning",
            "holo_galaxy", "holo_sakura", "holo_plasma_arc", "holo_diamond",
            "holo_aura", "holo_neon_pulse", "holo_constellation", "holo_cyber_dust", "holo_magical_wind", 
            "holo_mega", "holo_regional",
            "holo_time_gears", "holo_spatial_crack", "holo_prism_stars", "none"
    );

    private static final List<Float> STAT_VALUES = Arrays.asList(0.10f, 0.25f, 0.50f, 0.75f, 1.00f, 1.50f, 2.00f, 2.50f, 5.00f);

    private List<String> allSpeciesNames = new ArrayList<>();
    private List<String> filteredItems = new ArrayList<>();
    
    private String pokemonId = "pikachu";
    private boolean isShiny = false;
    private int rarityIndex = 0;
    private int backgroundIndex = BACKGROUNDS.indexOf("none");
    private int effectIndex = EFFECTS.indexOf("none");
    private int statIndex = 0;
    private int statValueIndex = 2; // Default: +5% (0.50f)
    private boolean isCardLocked = false;

    private EditBox searchBox;
    private Button bgButton;
    private Button holoButton;
    private Button statButton;
    private int leftPanelMode = 0; // 0 = POKEMON, 1 = BACKGROUND, 2 = HOLO, 3 = STAT
    private int scrollOffset = 0;
    private float ticks = 0;
    private long lastTime = 0L;
    private String lastQuery = "";
    
    // États pour le drag de la scrollbar
    private boolean isDraggingScrollbar = false;

    public CardWorkshopScreen() {
        super(Component.translatable("gui.cobblemon-cards.card_workshop.title"));
        if (backgroundIndex == -1) backgroundIndex = BACKGROUNDS.size() - 1;
        if (effectIndex == -1) effectIndex = EFFECTS.size() - 1;
    }

    @Override
    protected void init() {
        super.init();

        // 1. Initialiser la liste complète des Pokémon de Cobblemon avec variantes régionales et Mégas
        java.util.Set<String> alolan = java.util.Set.of("vulpix", "ninetales", "sandshrew", "sandslash", "raichu", "meowth", "persian", "geodude", "graveler", "golem", "grimer", "muk", "exeggutor", "marowak");
        java.util.Set<String> galarian = java.util.Set.of("zigzagoon", "linoone", "ponyta", "rapidash", "farfetchd", "weezing", "mr_mime", "corsola", "darumaka", "darmanitan", "yamask", "stunfisk", "slowpoke", "slowbro", "slowking", "articuno", "zapdos", "moltres");
        java.util.Set<String> hisuian = java.util.Set.of("growlithe", "arcanine", "voltorb", "electrode", "typhlosion", "qwilfish", "sneasel", "zorua", "zoroark", "braviary", "sliggoo", "goodra", "avalugg", "decidueye", "samurott", "lilligant", "basculin");
        java.util.Set<String> paldean = java.util.Set.of("wooper", "tauros");
        java.util.Set<String> mega = java.util.Set.of("venusaur", "charizard", "blastoise", "alakazam", "gengar", "kangaskhan", "pinsir", "gyarados", "aerodactyl", "mewtwo", "ampharos", "scizor", "heracross", "tyranitar", "blaziken", "gardevoir", "mawile", "aggron", "medicham", "manectric", "banette", "absol", "garchomp", "lucario", "abomasnow", "beedrill", "pidgeot", "steelix", "sceptile", "swampert", "sableye", "sharpedo", "camerupt", "altaria", "glalie", "salamence", "metagross", "latias", "latios", "rayquaza", "lopunny", "gallade", "audino", "diancie");
        java.util.Set<String> gmax = java.util.Set.of("venusaur", "butterfree", "meowth", "machamp", "gengar", "corviknight", "alcremie", "urshifu");
        java.util.Set<String> gigantamax = java.util.Set.of("charizard", "blastoise", "pikachu", "eevee", "snorlax", "melmetal", "rillaboom", "cinderace", "sandaconda", "toxtricity");

        List<String> baseNames = PokemonSpecies.getSpecies().stream()
                .map(s -> s.getName().toLowerCase())
                .distinct()
                .sorted()
                .toList();

        List<String> populatedList = new ArrayList<>();
        for (String base : baseNames) {
            populatedList.add(base);
            if (alolan.contains(base)) populatedList.add(base + "_alolan");
            if (galarian.contains(base)) populatedList.add(base + "_galarian");
            if (hisuian.contains(base)) populatedList.add(base + "_hisuian");
            if (paldean.contains(base)) {
                if (base.equals("tauros")) {
                    populatedList.add(base + "_paldean_combat");
                    populatedList.add(base + "_paldean_blaze");
                    populatedList.add(base + "_paldean_aqua");
                } else if (base.equals("wooper")) {
                    populatedList.add(base + "_paldean");
                }
            }
            if (mega.contains(base)) {
                if (base.equals("charizard") || base.equals("mewtwo")) {
                    populatedList.add(base + "_mega_x");
                    populatedList.add(base + "_mega_y");
                } else {
                    populatedList.add(base + "_mega");
                }
            }
            if (gmax.contains(base)) {
                populatedList.add(base + "_gmax");
                if (base.equals("urshifu")) {
                    populatedList.add("urshifu_rapidstrike_gmax");
                }
            }
            if (gigantamax.contains(base)) {
                populatedList.add(base + "_gigantamax");
            }
            if (base.equals("eternatus")) {
                populatedList.add("eternamax_eternatus");
            }
        }
        populatedList.sort(String::compareTo);
        this.allSpeciesNames = populatedList;

        // Initialiser la sélection par défaut
        if (!this.allSpeciesNames.isEmpty()) {
            if (this.allSpeciesNames.contains("pikachu")) {
                this.pokemonId = "pikachu";
            } else {
                this.pokemonId = this.allSpeciesNames.get(0);
            }
        }

        // Mettre à jour la recherche initiale
        updateSearch("");

        // 2. Enregistrer le rendu personnalisé comme premier renderable de l'interface
        // Tous les éléments personnalisés (panneaux, textes et carte 3D) sont dessinés dans ce widget
        // pour s'assurer qu'ils passent dans la passe de rendu officielle de Minecraft,
        // ce qui évite d'être flouté par les shaders de DoF/Menu blur des shaderpacks.
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) -> {
            // Rendu du fond translucide premium de l'écran
            graphics.fill(0, 0, this.width, this.height, 0x900A0A0F);
            
            // Cadre en verre Left Panel (Opaque pour éviter le flou de transparence des shaders shaderpacks)
            int leftX = 15;
            graphics.fill(leftX, 15, leftX + 140, this.height - 15, 0xFF121215);
            graphics.renderOutline(leftX, 15, 140, this.height - 30, 0xFF3A3A3E);

            // Titre dynamique et bouton retour sur le panneau de gauche
            if (leftPanelMode == 0) {
                graphics.drawString(this.font, "§e§lPOKÉMON CHOICE", leftX + 8, 25, 0xFFFFFF, true);
            } else if (leftPanelMode == 1) {
                graphics.drawString(this.font, "§b§lSELECT BACKGROUND", leftX + 8, 25, 0xFFFFFF, true);
                graphics.drawString(this.font, "§c§l[X]", leftX + 122, 25, 0xFFFFFF, true);
            } else if (leftPanelMode == 2) {
                graphics.drawString(this.font, "§d§lSELECT HOLO", leftX + 8, 25, 0xFFFFFF, true);
                graphics.drawString(this.font, "§c§l[X]", leftX + 122, 25, 0xFFFFFF, true);
            } else {
                graphics.drawString(this.font, "§a§lSELECT STAT", leftX + 8, 25, 0xFFFFFF, true);
                graphics.drawString(this.font, "§c§l[X]", leftX + 122, 25, 0xFFFFFF, true);
            }

            // Rendu de l'outline du SearchBox
            if (this.searchBox != null) {
                if (this.searchBox.isFocused()) {
                    graphics.renderOutline(leftX + 4, 39, 132, 18, 0xFFFFFF55);
                } else {
                    graphics.renderOutline(leftX + 4, 39, 132, 18, 0x40FFFFFF);
                }
            }

            // Rendu de la liste d'autocomplétion dynamique avec défilement (scroll)
            int listY = 65;
            int maxDisplayed = Math.max(1, (this.height - 20 - listY) / 16);
            int endIndex = Math.min(filteredItems.size(), scrollOffset + maxDisplayed);
            
            // S'assurer que le scrollOffset reste dans les clous si la liste se rétrécit
            if (scrollOffset > Math.max(0, filteredItems.size() - maxDisplayed)) {
                scrollOffset = Math.max(0, filteredItems.size() - maxDisplayed);
            }
            
            for (int i = scrollOffset; i < endIndex; i++) {
                String spec = filteredItems.get(i);
                
                String label;
                if (leftPanelMode == 0) {
                    String baseName = getBaseSpeciesName(spec);
                    String localized = net.minecraft.client.resources.language.I18n.get("cobblemon.species." + baseName);
                    
                    String suffix = "";
                    if (spec.endsWith("_alolan")) suffix = " (Alola)";
                    else if (spec.endsWith("_galarian")) suffix = " (Galar)";
                    else if (spec.endsWith("_hisuian")) suffix = " (Hisui)";
                    else if (spec.endsWith("_paldean") || spec.contains("_paldean_")) {
                        if (spec.endsWith("_combat")) suffix = " (Paldea Combat)";
                        else if (spec.endsWith("_blaze")) suffix = " (Paldea Blaze)";
                        else if (spec.endsWith("_aqua")) suffix = " (Paldea Aqua)";
                        else suffix = " (Paldea)";
                    }
                    else if (spec.endsWith("_mega")) suffix = " (Mega)";
                    else if (spec.endsWith("_mega_x")) suffix = " (Mega X)";
                    else if (spec.endsWith("_mega_y")) suffix = " (Mega Y)";
                    else if (spec.endsWith("_gmax") || spec.endsWith("_gigantamax")) suffix = " (G-Max)";
                    else if (spec.startsWith("eternamax_")) suffix = " (Eternamax)";
                    
                    label = localized + suffix;
                } else {
                    label = getCapitalized(spec);
                }
                
                int displayIndex = i - scrollOffset;
                int itemY = listY + displayIndex * 16;
                
                boolean isHovered = mouseX >= 20 && mouseX <= 145 && mouseY >= itemY && mouseY <= itemY + 14;
                int bgCol = isHovered ? 0x4DFFFFFF : 0x15FFFFFF;
                int textCol = isHovered ? 0xFFFF55 : 0xCCCCCC;

                graphics.fill(20, itemY, 145, itemY + 14, bgCol);
                graphics.renderOutline(20, itemY, 125, 14, 0x15FFFFFF);
                
                graphics.drawString(this.font, label, 24, itemY + 3, textCol, false);
            }

            // Rendu d'une barre de défilement (Scrollbar) ultra-premium sur le panneau de gauche
            if (filteredItems.size() > maxDisplayed) {
                int trackX = leftX + 134;
                int trackY = 65;
                int trackHeight = this.height - 20 - trackY;
                
                // Dessiner le rail de défilement en blanc translucide discret
                graphics.fill(trackX, trackY, trackX + 2, trackY + trackHeight, 0x20FFFFFF);
                
                // Calculer la hauteur et la position de la poignée de défilement (Scroll Handle)
                float handleHeight = Math.max(8.0f, ((float) maxDisplayed / filteredItems.size()) * trackHeight);
                float maxScroll = (float) (filteredItems.size() - maxDisplayed);
                float scrollPercentage = maxScroll > 0 ? (float) scrollOffset / maxScroll : 0.0f;
                float handleY = trackY + (trackHeight - handleHeight) * scrollPercentage;
                
                // Dessiner la poignée couleur or/jaune lumineuse
                graphics.fill(trackX, (int) handleY, trackX + 2, (int) (handleY + handleHeight), 0xFFFFAA00);
            }

            // Rendu 3D central interactif
            graphics.flush();
            
            float rotationY = 0;
            float rotationX = 0;
            float rotationZ = 0;
            float yFloat = 0;

            if (!isCardLocked) {
                // Calcul de l'orientation de la carte vers la souris (effet poupée d'inventaire)
                float centerX = this.width / 2.0f;
                float centerY = this.height / 2.0f;
                
                // Ratios de distance par rapport au centre (-1.0 à 1.0)
                float ratioX = (mouseX - centerX) / centerX;
                float ratioY = (mouseY - centerY) / centerY;
                
                // Rotation sur l'axe Y (gauche / droite) : max 40 degrés
                rotationY = ratioX * 40.0f;
                // Rotation sur l'axe X (haut / bas) : max 30 degrés
                rotationX = ratioY * -30.0f;
                // Légère inclinaison sur l'axe Z (roll) pour le dynamisme : max 8 degrés
                rotationZ = ratioX * -8.0f;

                // Flottement vertical de la carte pour la garder vivante
                yFloat = (float) Math.sin(ticks * 0.08f) * 5.0f;
            }

            graphics.pose().pushPose();
            graphics.pose().translate(this.width / 2.0f, this.height / 2.0f + yFloat, 100.0f);
            
            // Appliquer les rotations pour "regarder" la souris (ou 0 si lockée)
            graphics.pose().mulPose(Axis.XP.rotationDegrees(rotationX));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(rotationY));
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(rotationZ));
            
            // Taille premium de la carte
            float scale = 145.0f;
            graphics.pose().scale(scale, -scale, scale);

            Lighting.setupForFlatItems();
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    getPreviewStack(),
                    ItemDisplayContext.GUI,
                    LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY,
                    graphics.pose(),
                    graphics.bufferSource(),
                    Minecraft.getInstance().level,
                    0
            );
            graphics.flush();
            graphics.pose().popPose();

            // Rendu du Tooltip Preview en bas au centre (Arrière-plan Minecraft très premium)
            int tooltipW = 160;
            int tooltipH = 34;
            int tooltipX = this.width / 2 - tooltipW / 2;
            int tooltipY = this.height - 47;

            // Opaque dark violet Minecraft tooltip background
            graphics.fill(tooltipX, tooltipY, tooltipX + tooltipW, tooltipY + tooltipH, 0xF00D0914);
            graphics.renderOutline(tooltipX, tooltipY, tooltipW, tooltipH, 0xFF2D0A4F);
            graphics.renderOutline(tooltipX + 1, tooltipY + 1, tooltipW - 2, tooltipH - 2, 0xFF4A0896);

            String nameText = getRarityColor() + (isShiny ? "Shiny " : "") + getCapitalized(pokemonId);
            // Preview must match the real card tooltip: the config multiplier is applied exactly once.
            String statText = "§a" + com.howlite.cobblemoncards.util.CardStatUtil.formatValue(
                    CardStat.values()[statIndex], STAT_VALUES.get(statValueIndex))
                    + " " + getCapitalized(CardStat.values()[statIndex].getSerializedName());

            graphics.drawString(this.font, nameText, tooltipX + 8, tooltipY + 6, 0xFFFFFF, true);
            graphics.drawString(this.font, statText, tooltipX + 8, tooltipY + 18, 0xFFFFFF, true);

            // Rendu du titre principal de l'atelier d'administration
            String mainTitle = "§6§lCARD WORKSHOP §7- §c§lADMIN";
            graphics.drawCenteredString(this.font, mainTitle, this.width / 2, 10, 0xFFFFFF);
        });

        // 3. Zone de texte pour rechercher
        int leftX = 15;
        this.searchBox = new EditBox(this.font, leftX + 5, 40, 130, 16, Component.literal("Search..."));
        this.searchBox.setValue("");
        this.addRenderableWidget(this.searchBox);

        // 4. Boutons de cycle/choix sur le panneau de droite
        int rightX = this.width - 150;

        // Rareté
        this.addRenderableWidget(Button.builder(Component.literal("Rarity: " + getCapitalized(RARITIES.get(rarityIndex))), b -> {
            rarityIndex = (rarityIndex + 1) % RARITIES.size();
            b.setMessage(Component.literal("Rarity: " + getCapitalized(RARITIES.get(rarityIndex))));
            playClickSound();
        }).bounds(rightX, 40, 130, 18).build());

        // Shiny
        this.addRenderableWidget(Button.builder(Component.literal("Shiny: " + (isShiny ? "YES" : "NO")), b -> {
            isShiny = !isShiny;
            b.setMessage(Component.literal("Shiny: " + (isShiny ? "YES" : "NO")));
            playClickSound();
        }).bounds(rightX, 63, 130, 18).build());

        // Background (Ouvre la liste à gauche)
        this.bgButton = Button.builder(Component.literal("Bg: " + BACKGROUNDS.get(backgroundIndex)), b -> {
            this.leftPanelMode = 1; // Mode Arrière-plan
            this.searchBox.setValue("");
            updateSearch("");
            playClickSound();
        }).bounds(rightX, 86, 130, 18).build();
        this.addRenderableWidget(this.bgButton);

        // Effet Holo (Ouvre la liste à gauche)
        this.holoButton = Button.builder(Component.literal("Holo: " + EFFECTS.get(effectIndex)), b -> {
            this.leftPanelMode = 2; // Mode Effet Holo
            this.searchBox.setValue("");
            updateSearch("");
            playClickSound();
        }).bounds(rightX, 109, 130, 18).build();
        this.addRenderableWidget(this.holoButton);

        // Statistique (Ouvre la liste à gauche)
        this.statButton = Button.builder(Component.literal("Stat: " + getCapitalized(CardStat.values()[statIndex].getSerializedName())), b -> {
            this.leftPanelMode = 3; // Mode Statistique
            this.searchBox.setValue("");
            updateSearch("");
            playClickSound();
        }).bounds(rightX, 132, 130, 18).build();
        this.addRenderableWidget(this.statButton);

        // Valeur Statistique (valeur brute stockée sur la carte, avant multiplicateur de config)
        this.addRenderableWidget(Button.builder(Component.literal("Raw Value: +" + Math.round(STAT_VALUES.get(statValueIndex) * 100) + "%"), b -> {
            statValueIndex = (statValueIndex + 1) % STAT_VALUES.size();
            b.setMessage(Component.literal("Raw Value: +" + Math.round(STAT_VALUES.get(statValueIndex) * 100) + "%"));
            playClickSound();
        }).bounds(rightX, 155, 130, 18).build());

        // Bouton Lock Card
        this.addRenderableWidget(Button.builder(Component.literal("Lock Card: " + (isCardLocked ? "YES" : "NO")), b -> {
            isCardLocked = !isCardLocked;
            b.setMessage(Component.literal("Lock Card: " + (isCardLocked ? "YES" : "NO")));
            playClickSound();
        }).bounds(rightX, 178, 130, 18).build());

        // Bouton final GENERATE CARD
        this.addRenderableWidget(Button.builder(Component.literal("§a§lGENERATE CARD"), b -> {
            generateCard();
        }).bounds(rightX, this.height - 35, 130, 20).build());
    }

    private String getRarityColor() {
        String r = RARITIES.get(rarityIndex);
        return switch (r) {
            case "uncommon" -> "§a"; // Green
            case "rare" -> "§9"; // Blue
            case "epic" -> "§d"; // Purple
            case "legendary" -> "§6"; // Gold
            default -> "§f"; // White
        };
    }

    private String getCapitalized(String text) {
        if (text == null || text.isEmpty()) return "";
        return text.substring(0, 1).toUpperCase() + text.substring(1).replace("_", " ");
    }

    private void updateSearch(String query) {
        this.scrollOffset = 0; // Réinitialiser le scroll lors de la recherche
        String cleanQuery = query.trim().toLowerCase();
        if (leftPanelMode == 0) { // POKEMON
            if (cleanQuery.isEmpty()) {
                this.filteredItems = this.allSpeciesNames;
                return;
            }
            this.filteredItems = this.allSpeciesNames.stream()
                    .filter(name -> {
                        if (name.contains(cleanQuery)) return true;
                        String localized = net.minecraft.client.resources.language.I18n.get("cobblemon.species." + name).toLowerCase();
                        return localized.contains(cleanQuery);
                    })
                    .toList();
        } else if (leftPanelMode == 1) { // BACKGROUND
            this.filteredItems = BACKGROUNDS.stream()
                    .filter(name -> name.toLowerCase().contains(cleanQuery))
                    .toList();
        } else if (leftPanelMode == 2) { // HOLO
            this.filteredItems = EFFECTS.stream()
                    .filter(name -> name.toLowerCase().contains(cleanQuery))
                    .toList();
        } else { // STAT
            this.filteredItems = Arrays.stream(CardStat.values())
                    .map(CardStat::getSerializedName)
                    .filter(name -> name.toLowerCase().contains(cleanQuery))
                    .toList();
        }
    }

    private void generateCard() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.2f));
        
        String rarity = RARITIES.get(rarityIndex);
        String bg = BACKGROUNDS.get(backgroundIndex);
        String eff = EFFECTS.get(effectIndex);
        String statName = CardStat.values()[statIndex].getSerializedName();
        float statVal = STAT_VALUES.get(statValueIndex);

        com.howlite.cobblemoncards.util.PlatformHelper.INSTANCE.sendToServer(
                new GenerateCardPayload(pokemonId, isShiny, rarity, statName, statVal, bg, eff)
        );

        this.onClose();
    }

    private void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private ItemStack getPreviewStack() {
        ItemStack stack = new ItemStack(ModItems.CARD);
        Optional<String> bg = BACKGROUNDS.get(backgroundIndex).equals("none") ? Optional.empty() : Optional.of(BACKGROUNDS.get(backgroundIndex));
        Optional<String> eff = EFFECTS.get(effectIndex).equals("none") ? Optional.empty() : Optional.of(EFFECTS.get(effectIndex));
        
        CardStat stat = CardStat.values()[statIndex];
        float statValue = STAT_VALUES.get(statValueIndex);
        
        CardData data = new CardData(this.pokemonId, this.isShiny, RARITIES.get(rarityIndex), stat, statValue, 10, bg, eff);
        stack.set(ModDataComponents.CARD_DATA, data);
        return stack;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (this.searchBox.isFocused() && this.searchBox.isVisible() && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (this.searchBox.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // Gérer le scroll molette au-dessus du panneau de gauche (x de 15 à 155)
        if (mouseX >= 15 && mouseX <= 155) {
            int maxDisplayed = Math.max(1, (this.height - 20 - 65) / 16);
            int maxScroll = Math.max(0, filteredItems.size() - maxDisplayed);
            
            if (scrollY > 0) { // Défiler vers le haut
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else if (scrollY < 0) { // Défiler vers le bas
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDraggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Drag glissé-déposé sur la scrollbar de gauche
        int listY = 65;
        int maxDisplayed = Math.max(1, (this.height - 20 - listY) / 16);
        if (this.isDraggingScrollbar && filteredItems.size() > maxDisplayed) {
            int trackY = 65;
            int trackHeight = this.height - 20 - trackY;
            float pct = (float) (mouseY - trackY) / trackHeight;
            int maxScroll = filteredItems.size() - maxDisplayed;
            this.scrollOffset = Math.max(0, Math.min(maxScroll, Math.round(pct * maxScroll)));
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int leftX = 15;
        int listY = 65;
        int maxDisplayed = Math.max(1, (this.height - 20 - listY) / 16);
        
        // 1. Clic / Grab sur la Scrollbar du panneau de gauche (x de 147 à 152)
        if (button == 0 && filteredItems.size() > maxDisplayed && mouseX >= 147 && mouseX <= 152) {
            int trackY = 65;
            int trackHeight = this.height - 20 - trackY;
            if (mouseY >= trackY && mouseY <= trackY + trackHeight) {
                this.isDraggingScrollbar = true;
                float pct = (float) (mouseY - trackY) / trackHeight;
                int maxScroll = filteredItems.size() - maxDisplayed;
                this.scrollOffset = Math.max(0, Math.min(maxScroll, Math.round(pct * maxScroll)));
                playClickSound();
                return true;
            }
        }
        
        // 2. Clic sur le bouton de retour [X] dans le panneau de gauche
        if (leftPanelMode != 0 && mouseX >= leftX + 118 && mouseX <= leftX + 138 && mouseY >= 20 && mouseY <= 32) {
            this.leftPanelMode = 0; // Retour en mode Pokémon
            this.searchBox.setValue("");
            updateSearch("");
            playClickSound();
            return true;
        }

        // 3. Clic sur les éléments de la liste de gauche
        if (mouseX >= 20 && mouseX <= 145) {
            int startY = 65;
            int endIndex = Math.min(filteredItems.size(), scrollOffset + maxDisplayed);
            
            for (int i = scrollOffset; i < endIndex; i++) {
                int displayIndex = i - scrollOffset;
                int itemY = startY + displayIndex * 16;
                if (mouseY >= itemY && mouseY <= itemY + 14) {
                    String selected = filteredItems.get(i);
                    
                    if (leftPanelMode == 0) { // POKEMON
                        this.pokemonId = selected;
                        this.searchBox.setValue(net.minecraft.client.resources.language.I18n.get("cobblemon.species." + this.pokemonId));
                        this.searchBox.setFocused(false);
                    } else if (leftPanelMode == 1) { // BACKGROUND
                        this.backgroundIndex = BACKGROUNDS.indexOf(selected);
                        if (this.bgButton != null) {
                            this.bgButton.setMessage(Component.literal("Bg: " + getCapitalized(selected)));
                        }
                    } else if (leftPanelMode == 2) { // HOLO
                        this.effectIndex = EFFECTS.indexOf(selected);
                        if (this.holoButton != null) {
                            this.holoButton.setMessage(Component.literal("Holo: " + getCapitalized(selected)));
                        }
                    } else { // STAT
                        for (int k = 0; k < CardStat.values().length; k++) {
                            if (CardStat.values()[k].getSerializedName().equals(selected)) {
                                this.statIndex = k;
                                break;
                            }
                        }
                        if (this.statButton != null) {
                            this.statButton.setMessage(Component.literal("Stat: " + getCapitalized(selected)));
                        }
                    }
                    
                    playClickSound();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        if (this.lastTime == 0L) {
            this.lastTime = now;
        }
        float deltaTime = (now - this.lastTime) / 50.0f;
        this.lastTime = now;

        this.ticks += deltaTime;
        
        // Mettre à jour l'autocomplétion si la saisie change
        String query = this.searchBox.getValue();
        if (!query.equals(lastQuery)) {
            lastQuery = query;
            updateSearch(query);
        }

        // Le rendu d'arrière-plan de base applique le flou Minecraft.
        // Tous nos éléments personnalisés sont dessinés dans l'ordre par le premier renderable
        // enregistré dans init(), garantissant qu'ils restent parfaitement nets.
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String getBaseSpeciesName(String name) {
        String lower = name.toLowerCase();
        if (lower.startsWith("eternamax_")) return "eternatus";
        if (lower.endsWith("_rapidstrike_gmax")) return "urshifu";
        if (lower.endsWith("_alolan")) return lower.substring(0, lower.length() - "_alolan".length());
        if (lower.endsWith("_galarian")) return lower.substring(0, lower.length() - "_galarian".length());
        if (lower.endsWith("_hisuian")) return lower.substring(0, lower.length() - "_hisuian".length());
        if (lower.endsWith("_paldean_combat")) return "tauros";
        if (lower.endsWith("_paldean_blaze")) return "tauros";
        if (lower.endsWith("_paldean_aqua")) return "tauros";
        if (lower.endsWith("_paldean")) return lower.substring(0, lower.length() - "_paldean".length());
        if (lower.endsWith("_mega_x")) return lower.substring(0, lower.length() - "_mega_x".length());
        if (lower.endsWith("_mega_y")) return lower.substring(0, lower.length() - "_mega_y".length());
        if (lower.endsWith("_mega")) return lower.substring(0, lower.length() - "_mega".length());
        if (lower.endsWith("_gmax")) return lower.substring(0, lower.length() - "_gmax".length());
        if (lower.endsWith("_gigantamax")) return lower.substring(0, lower.length() - "_gigantamax".length());
        return lower;
    }
}
