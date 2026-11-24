package com.howlite.cobblemoncards.render;

import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.item.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import com.cobblemon.mod.common.pokemon.Species;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import net.minecraft.Util;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CardItemRenderer {

    private static final ConcurrentHashMap<String, GameProfile> PLAYER_PROFILE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> PLAYER_PROFILE_PENDING = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<String, ResourceLocation> POKEMON_TEXTURE_CACHE = new ConcurrentHashMap<>();
    private static volatile net.minecraft.server.packs.resources.ResourceManager cachedResourceManager = null;

    private static final ResourceLocation TEXTURE_STARS = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/effect/foil_stars.png");
    private static final ResourceLocation TEXTURE_GLINT = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/effect/glint.png");
    private static final ResourceLocation TEXTURE_NOISE = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/effect/noise.png");
    private static final ResourceLocation TEXTURE_FLOW = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/effect/flow.png");

    // UV crop constants for textures padded to the next multiple of 16 (mipmap fix).
    // Original 40x30 sprites are embedded in a 48x32 canvas (padding right+bottom).
    // UV must stop at 40/48 horizontally and 30/32 vertically to avoid sampling transparent padding.
    private static final float UV_40x30_U = 40.0f / 48.0f; // 0.8333f
    private static final float UV_40x30_V = 30.0f / 32.0f; // 0.9375f
    // Original 68x56 sprites (hi-res entity icons) padded to 80x64.
    private static final float UV_68x56_U = 68.0f / 80.0f; // 0.85f
    private static final float UV_68x56_V = 56.0f / 64.0f; // 0.875f

    public static void render(ItemStack stack, ItemDisplayContext mode, PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay) {

        // 🛑 SÉCURITÉ :
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) return;

        // Hide small item in hand while this card is being shown off in big
        if (mode == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND || mode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
            mode == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            if (CardShowRenderer.isShowingStack(stack)) {
                return;
            }
        }

        // Informe ModShaders du contexte courant :
        // - GUI → Iris n'intercepte pas le pipeline → les custom shaders GLSL fonctionnent
        // - Monde (main, sol, holo projector) → Iris peut intercepter → fallback CPU si actif
        boolean isGuiContext = (mode == ItemDisplayContext.GUI);
        ModShaders.setCurrentContext(isGuiContext);

        CardData data = stack.get(ModDataComponents.CARD_DATA);
        // Si la carte n'a pas de données, on affiche un modèle de "carte mystère" par défaut
        if (data == null) {
            renderDefaultCard(matrices, vertexConsumers, light, overlay, mode);
            return;
        }


        // --- 1. CHARGER LE MODÈLE BLOCKBENCH (Le cadre selon la rareté) ---
        Item frameItem = getFrameItem(data);
        ItemStack frameStack = new ItemStack(frameItem);
        BakedModel frameModel = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(frameStack);
        boolean leftHand = mode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        matrices.pushPose();

        // 1. Annuler le décalage natif de Minecraft pour recentrer le point de pivot de la matrice.
        matrices.translate(0.5f, 0.5f, 0.5f);

        // 2. Appliquer les réglages "Display" de ton fichier Blockbench.
        frameModel.getTransforms().getTransform(mode).apply(leftHand, matrices);

        // --- 2. DESSINER LE MODÈLE 3D (CADRE) ---
        Minecraft.getInstance().getItemRenderer().render(
                frameStack, ItemDisplayContext.NONE, leftHand, matrices, vertexConsumers, light, overlay, frameModel
        );

        // Préparation des coordonnées communes
        matrices.translate(-0.5f, -0.5f, -0.5f);

        // --- 🔍 RESOLUTION DU SPRITE ET DU RATIO D'IMAGE ---
        float imageWidth = 40.0f;
        float imageHeight = 30.0f;
        
        ResourceLocation pokemonTex = null;
        boolean isPlayer = data.pokemonId().startsWith("player_");
        boolean isSilhouette = data.pokemonId().startsWith("silhouette_");
        String resolvedPokemonId = isSilhouette ? data.pokemonId().substring("silhouette_".length()) : data.pokemonId();
        
        if (isPlayer) {
            // Player skin textures are resolved dynamically
        } else {
            pokemonTex = resolvePokemonTexture(resolvedPokemonId, data.isShiny());
        }

        float pokeHeight = 6.0f / 16.0f;
        float pokeWidth = pokeHeight * (imageWidth / imageHeight);

        // Les dimensions de la fenêtre de la carte (toujours au ratio 40x30 soit 4:3)
        // afin d'éviter les bandes noires sur les côtés pour les sprites qui ont d'autres ratios (ex: Gen 8/9 en 68x56)
        float cardWindowWidth = 8.0f / 16.0f; // 0.50f
        float cardWindowHeight = 6.0f / 16.0f; // 0.375f

        // --- 3. DESSINER LE FOND (BG) ---
        java.util.Optional<String> backgroundOpt = isSilhouette ? java.util.Optional.empty() : data.background();
        java.util.Optional<String> effectOpt = isSilhouette ? java.util.Optional.empty() : data.effect();
        if (backgroundOpt.isEmpty() && effectOpt.isPresent()) {
            backgroundOpt = java.util.Optional.of(com.howlite.cobblemoncards.util.CardUtil.getDefaultBackground(resolvedPokemonId));
        }

        if (backgroundOpt.isPresent()) {
            matrices.pushPose();
            // On recule le fond (Z plus grand = plus loin cause rotation 180)
            matrices.translate(5.0f / 16.0f, 10.0f / 16.0f, 0.995f / 16.0f);
            matrices.mulPose(Axis.YP.rotationDegrees(180));

            String bgType = backgroundOpt.get();
            if (isProceduralBackground(bgType)) {
                if (ModShaders.isAvailable()) {
                    renderProceduralBackgroundShader(matrices, vertexConsumers, bgType, light, overlay, cardWindowWidth, cardWindowHeight);
                } else {
                    renderProceduralBackground(matrices, vertexConsumers, bgType, light, overlay, cardWindowWidth, cardWindowHeight);
                }
            } else {
                ResourceLocation bgTex = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/background/" + bgType + ".png");
                // UV cropped to original 40x30 content area in the padded 48x32 texture
                renderQuad(matrices.last().pose(), vertexConsumers.getBuffer(RenderType.entityCutout(bgTex)), light, overlay, cardWindowWidth, cardWindowHeight, 0, 0, UV_40x30_U, UV_40x30_V, 255, 255, 255, 255);
            }
            
            matrices.popPose();
        }

        // --- 4. DESSINER LE POKÉMON ---
        matrices.pushPose();
        // On avance le Pokémon (Z plus petit = plus proche du joueur)
        matrices.translate(5.0f / 16.0f, 10.0f / 16.0f, 0.985f / 16.0f);
        matrices.mulPose(Axis.YP.rotationDegrees(180));

        if (isPlayer) {
            String nameAndUuid = data.pokemonId().substring("player_".length());
            int underscoreIdx = nameAndUuid.indexOf('_');
            java.util.UUID uuid;
            String name;
            if (underscoreIdx != -1) {
                try {
                    uuid = java.util.UUID.fromString(nameAndUuid.substring(0, underscoreIdx));
                    name = nameAndUuid.substring(underscoreIdx + 1);
                } catch (IllegalArgumentException e) {
                    uuid = java.util.UUID.nameUUIDFromBytes(nameAndUuid.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    name = nameAndUuid;
                }
            } else {
                uuid = java.util.UUID.nameUUIDFromBytes(nameAndUuid.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                name = nameAndUuid;
            }
            ResourceLocation skinTex = resolvePlayerSkin(uuid, name);
            
            float headSize = pokeHeight * 0.7f;
            
            // Draw base head layer
            renderQuad(matrices.last().pose(), vertexConsumers.getBuffer(RenderType.entityTranslucent(skinTex)), light, overlay, headSize, headSize, 0.125f, 0.125f, 0.25f, 0.25f, 255, 255, 255, 255);
            
            // Draw hat/hair layer (overlay)
            matrices.pushPose();
            matrices.translate(0, 0, -0.0005f); // Tiny Z offset to prevent Z-fighting
            renderQuad(matrices.last().pose(), vertexConsumers.getBuffer(RenderType.entityTranslucent(skinTex)), light, overlay, headSize, headSize, 0.625f, 0.125f, 0.75f, 0.25f, 255, 255, 255, 255);
            matrices.popPose();
        } else {
            // All pokemon sprites come from the entity_icon folder (40x30 padded to 48x32 for mipmap).
            // imageWidth/imageHeight control QUAD sizing only — the actual texture is always 48x32.
            // UV_68x56 would be wrong here (68/80*32=40.8 wide, 56/64*32=28 tall instead of 30 → squish).
            if (isSilhouette) {
                renderQuad(matrices.last().pose(), vertexConsumers.getBuffer(RenderType.entityCutout(pokemonTex)), light, overlay, pokeWidth, pokeHeight, 0, 0, UV_40x30_U, UV_40x30_V, 30, 30, 30, 255);
            } else {
                renderQuad(matrices.last().pose(), vertexConsumers.getBuffer(RenderType.entityCutout(pokemonTex)), light, overlay, pokeWidth, pokeHeight, 0, 0, UV_40x30_U, UV_40x30_V, 255, 255, 255, 255);
            }
        }
        matrices.popPose();

        // --- 5. DESSINER L'EFFET HOLOGRAPHIQUE ---
        if (effectOpt.isPresent()) {
            renderHoloLayer(matrices, vertexConsumers, effectOpt.get(), light, overlay, cardWindowWidth, cardWindowHeight);
        }

        matrices.popPose();
    }

    private static void renderDefaultCard(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, ItemDisplayContext mode) {
        ItemStack defaultStack = new ItemStack(ModItems.FRAME_COMMON);
        BakedModel defaultModel = Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(defaultStack);
        boolean leftHand = mode == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || mode == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

        matrices.pushPose();
        matrices.translate(0.5f, 0.5f, 0.5f);
        defaultModel.getTransforms().getTransform(mode).apply(leftHand, matrices);
        Minecraft.getInstance().getItemRenderer().render(defaultStack, ItemDisplayContext.NONE, leftHand, matrices, vertexConsumers, light, overlay, defaultModel);
        matrices.popPose();
    }
    
    private static boolean isCustomNewBackground(String bg) {
        return bg.equals("mega_energy") || bg.equals("alola_beach") || bg.equals("hisui_ancient") || bg.equals("galar_industrial") || bg.equals("paldea_crystal") || bg.equals("distortion_rift") || bg.equals("dreamscape") || bg.equals("magma_chamber") || bg.equals("stained_glass") || bg.equals("fluid_marble") || bg.equals("fossilized_amber");
    }
    
    private static double getMarbleHeight(double u, double v, double time) {
        double px = u * 4.0;
        double py = v * 5.0;
        
        double qx = Math.sin(px + time * 0.8) + Math.cos(py * 1.5 - time * 0.5);
        double qy = Math.cos(py + time * 0.6) + Math.sin(px * 1.2 + time * 0.4);
        
        double rx = Math.sin(px + 2.5 * qx + time * 1.2) + Math.cos(py + 2.5 * qy - time * 0.9);
        double ry = Math.cos(py + 2.5 * qy + time * 1.0) + Math.sin(px + 2.5 * qx + time * 0.7);
        
        return Math.sin(px + 2.0 * rx) * Math.cos(py + 2.0 * ry) * 0.5 + 0.5;
    }
    
    private static double getMagmaHeight(double u, double v, double time) {
        double scaleX = u * 9.0;
        double scaleY = v * 11.0;
        
        double wobble = Math.sin(scaleX * 1.5 + time * 0.8) * 0.4 + Math.cos(scaleY * 1.5 - time * 0.6) * 0.4;
        double n1 = Math.sin(scaleX + wobble);
        double n2 = Math.sin(scaleY + wobble * 0.7);
        
        double crack = Math.abs(n1 * n2);
        return smoothstep(0.0, 0.22, crack);
    }
    
    private static double smoothstep(double edge0, double edge1, double x) {
        double t = Math.max(0.0, Math.min(1.0, (x - edge0) / (edge1 - edge0)));
        return t * t * (3.0 - 2.0 * t);
    }

    
    private static boolean isProceduralBackground(String bg) {
        return bg.equals("water_anim") || bg.equals("lava_anim") || bg.equals("balatro_swirl") || 
               bg.equals("geometric_pulse") || bg.equals("plasma_bg") || bg.equals("starfield_anim") ||
               bg.equals("cloud_scroll") || bg.equals("neon_grid") || bg.equals("toxic_sludge") ||
               bg.equals("matrix_code") || bg.equals("fire_embers") || bg.equals("crystal_cave") || 
               bg.equals("sandstorm") || bg.equals("aurora_borealis") || bg.equals("deep_ocean") ||
               bg.equals("void_rift") || bg.equals("golden_sunset") || bg.equals("cherry_blossom_wind") ||
               bg.equals("cyber_city") || bg.equals("ancient_ruins") || bg.equals("frozen_tundra") ||
               bg.equals("rainbow_highway") || bg.equals("plasma_storm") || bg.equals("galactic_supernova") ||
               bg.equals("water2") || isCustomNewBackground(bg);
    }
    
    private static void renderProceduralBackground(PoseStack matrices, MultiBufferSource vertexConsumers, String bgType, int light, int overlay, float w, float h) {
        Matrix4f matrix = matrices.last().pose();
        long bucket = ProceduralTextureCache.currentBucket();
        ResourceLocation cachedTex = ProceduralTextureCache.getFreshBgTexture(bgType, bucket);
        if (cachedTex != null) {
            renderQuad(matrix, vertexConsumers.getBuffer(RenderType.entityCutout(cachedTex)), light, overlay, w, h);
            return;
        }

        float time = (System.currentTimeMillis() % 10000) / 1000.0f;
        
        // Grille procédurale 40×30
        int gridX = ProceduralTextureCache.WIDTH;   // 40
        int gridY = ProceduralTextureCache.HEIGHT;  // 30

        // Tableau de pixels en format ABGR (attendu par NativeImage.setPixelRGBA)
        // indexé par (row * gridX + col), row=0 = haut de l'image texture
        int[] bgPixels = new int[gridX * gridY];
        
        // On itère sur chaque "pixel" de la grille
        for (int ix = 0; ix < gridX; ix++) {
            for (int iy = 0; iy < gridY; iy++) {
                
                // Coordonnées de base (0 à 1) pour les calculs de bruit/maths
                float u = (float) ix / gridX;
                float v = (float) iy / gridY;
                
                // Calcul de la couleur pour ce pixel selon l'effet
                float r = 0, g = 0, b = 0;
                
                switch (bgType) {
                    case "water_anim" -> {
                        float wave1 = (float) Math.sin(u * 15.0 - time * 2.0) * 0.1f;
                        float wave2 = (float) Math.sin(u * 5.0 + time * 1.5) * 0.05f;
                        float waterLevel = v + wave1 + wave2;
                        
                        float repeatLevel = (waterLevel * 5.0f - time) % 1.0f;
                        if (repeatLevel < 0) repeatLevel += 1.0f;
                        
                        if (repeatLevel > 0.85f) {
                            r = 0.85f; g = 0.95f; b = 1.0f;
                        } else if (repeatLevel > 0.75f) {
                            r = 0.3f; g = 0.7f; b = 0.95f;
                        } else {
                            r = 0.1f; g = 0.45f; b = 0.85f;
                        }
                    }
                    case "lava_anim" -> {
                        double noise = Math.sin(u * 8.0 - time * 2.0) * Math.sin(v * 8.0 + time * 2.5) 
                                     + Math.sin((u - v) * 12.0 + time);
                        
                        if (noise > 0.8) {
                            r = 1.0f; g = 0.8f; b = 0.0f; 
                        } else if (noise > 0.2) {
                            r = 0.9f; g = 0.3f; b = 0.0f; 
                        } else if (noise > -0.4) {
                            r = 0.6f; g = 0.1f; b = 0.0f; 
                        } else {
                            r = 0.3f; g = 0.1f; b = 0.1f; 
                        }
                    }
                    case "balatro_swirl" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.4f; // Aspect ratio adjustment
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        float theta = (float) Math.atan2(dy, dx);
                        
                        float wave = (float) Math.sin(radius * 16.0f - time * 2.5f + Math.sin(theta * 4.0f + time * 1.2f) * 1.2f);
                        float wave2 = (float) Math.cos(theta * 3.0f - time * 1.8f + radius * 10.0f);
                        float val = wave + wave2 * 0.6f;
                        
                        if (val > 0.4f) {
                            r = 0.9f; g = 0.15f; b = 0.1f;
                        } else if (val > 0.0f) {
                            r = 0.6f; g = 0.05f; b = 0.4f;
                        } else if (val > -0.6f) {
                            r = 0.05f; g = 0.6f; b = 0.85f;
                        } else {
                            r = 0.08f; g = 0.04f; b = 0.18f;
                        }
                    }
                    case "geometric_pulse" -> {
                        float dx = Math.abs(u - 0.5f);
                        float dy = Math.abs(v - 0.5f);
                        float dist = dx + dy;
                        
                        float pulse = (float) Math.sin(dist * 24.0f - time * 3.5f);
                        float lineIntensity = (float) Math.max(0.0, Math.min(1.0, (pulse - 0.7f) / 0.25f)); // smoothstep manual
                        
                        float grid = (float) Math.abs(((dist * 8.0f - time * 1.0f) % 1.0f + 1.0f) % 1.0f - 0.5f);
                        float gridLine = (float) Math.max(0.0, Math.min(1.0, (grid - 0.2f) / 0.05f)); // smoothstep manual
                        
                        float mixRatio = (float) (Math.sin(time * 0.5f + dist) * 0.5f + 0.5f);
                        float pr = 1.0f * (1.0f - mixRatio) + 0.05f * mixRatio;
                        float pg = 0.05f * (1.0f - mixRatio) + 0.85f * mixRatio;
                        float pb = 0.6f * (1.0f - mixRatio) + 1.0f * mixRatio;
                        
                        if (pulse > 0.4f) {
                            r = 0.06f * (1.0f - lineIntensity * 0.7f) + pr * lineIntensity * 0.7f;
                            g = 0.02f * (1.0f - lineIntensity * 0.7f) + pg * lineIntensity * 0.7f;
                            b = 0.12f * (1.0f - lineIntensity * 0.7f) + pb * lineIntensity * 0.7f;
                        } else if (gridLine < 0.15f) {
                            float factor = 1.0f - gridLine / 0.15f;
                            r = 0.04f * (1.0f - factor) + pr * 0.4f * factor;
                            g = 0.02f * (1.0f - factor) + pg * 0.4f * factor;
                            b = 0.08f * (1.0f - factor) + pb * 0.4f * factor;
                        } else {
                            r = 0.04f; g = 0.02f; b = 0.08f;
                        }
                    }
                    case "plasma_bg" -> {
                        double val = Math.sin(u * 10.0 + time) + Math.sin((v * 10.0 + time) / 2.0) + Math.sin((u * 10.0 + v * 10.0 + time) / 2.0);
                        r = (float)(Math.sin(val * Math.PI) * 0.5 + 0.5);
                        g = (float)(Math.sin(val * Math.PI + 2.0 * Math.PI / 3.0) * 0.5 + 0.5);
                        b = (float)(Math.sin(val * Math.PI + 4.0 * Math.PI / 3.0) * 0.5 + 0.5);
                    }
                    case "starfield_anim" -> {
                        float speed = 15.0f;
                        float yOffset = (iy - time * speed);
                        int virtualY = (int) Math.floor(yOffset);
                        double seed = ix * 12.9898 + virtualY * 78.233;
                        double hash = Math.abs(Math.sin(seed) * 43758.5453);
                        hash = hash - Math.floor(hash);

                        if (hash > 0.97) {
                            r = 1.0f; g = 1.0f; b = 1.0f; 
                        } else if (hash > 0.95) {
                            r = 0.5f; g = 0.8f; b = 1.0f; 
                        } else {
                            r = 0.05f; g = 0.05f; b = 0.1f; 
                        }
                    }
                    case "cloud_scroll" -> {
                        double noise = Math.sin((u - time * 0.2) * 10.0) + Math.sin(v * 15.0) + Math.cos((u - time * 0.1 - v) * 5.0);

                        if (noise > 0.8) {
                            r = 1.0f; g = 1.0f; b = 1.0f; 
                        } else if (noise > 0.4) {
                            r = 0.85f; g = 0.95f; b = 1.0f; 
                        } else {
                            r = 0.3f + v * 0.3f;
                            g = 0.6f + v * 0.2f;
                            b = 0.9f + v * 0.1f;
                        }
                    }
                    case "neon_grid" -> {
                        int animY = (int) Math.floor(time * 8.0f);
                        boolean onGridX = (ix % 5 == 0);
                        boolean onGridY = ((iy + animY) % 5 == 0);

                        if (onGridX || onGridY) {
                            r = 0.9f - u * 0.7f;
                            g = 0.1f + u * 0.8f;
                            b = 0.8f + u * 0.2f;
                        } else {
                            r = 0.05f; g = 0.02f; b = 0.1f;
                        }
                    }
                    case "toxic_sludge" -> {
                        double bubble = Math.sin(u * 12.0 + Math.cos(time * 2.0)) * Math.cos(v * 12.0 - time * 4.0) + Math.sin((u+v)*8.0);

                        if (bubble > 0.8) {
                            r = 0.8f; g = 1.0f; b = 0.2f; 
                        } else if (bubble > 0.2) {
                            r = 0.3f; g = 0.8f; b = 0.1f; 
                        } else if (bubble > -0.5) {
                            r = 0.1f; g = 0.5f; b = 0.1f; 
                        } else {
                            r = 0.05f; g = 0.2f; b = 0.05f; 
                        }
                    }
                    case "matrix_code" -> {
                        float speed = 20.0f;
                        double colHash = Math.abs(Math.sin(ix * 12.9898)) * 100.0;
                        float yOffset = iy - time * speed + (float)colHash;
                        
                        int drop = (int) Math.floor(yOffset / 10.0f);
                        int subY = (int) Math.floor(yOffset) % 10;
                        if (subY < 0) subY += 10;
                        
                        double cellHash = Math.abs(Math.sin(ix * 12.9898 + drop * 78.233));
                        cellHash = cellHash - Math.floor(cellHash);
                        
                        if (cellHash > 0.4) {
                            if (subY == 9) {
                                r = 0.8f; g = 1.0f; b = 0.8f; // Tête brillante
                            } else if (subY > 5) {
                                r = 0.2f; g = 0.8f; b = 0.2f; // Corps
                            } else if (subY > 2) {
                                r = 0.05f; g = 0.4f; b = 0.05f; // Traîne
                            } else {
                                r = 0.02f; g = 0.05f; b = 0.02f; // Vide
                            }
                        } else {
                            r = 0.02f; g = 0.05f; b = 0.02f; // Vide
                        }
                    }
                    case "fire_embers" -> {
                        float speed = 12.0f;
                        float sway = (float) Math.sin(iy * 0.1 + time * 2.0) * 3.0f;
                        float yOffset = iy + time * speed;
                        
                        int virtualY = (int) Math.floor(yOffset);
                        int virtualX = (int) Math.floor(ix + sway);
                        
                        double hash = Math.abs(Math.sin(virtualX * 12.9898 + virtualY * 78.233));
                        hash = hash - Math.floor(hash);
                        
                        float glow = Math.max(0, (v - 0.2f) * 1.2f);
                        
                        if (hash > 0.98) {
                            r = 1.0f; g = 0.9f; b = 0.2f; // Étincelle blanche/jaune
                        } else if (hash > 0.96) {
                            r = 1.0f; g = 0.5f; b = 0.1f; // Étincelle orange
                        } else if (hash > 0.94) {
                            r = 0.8f; g = 0.2f; b = 0.0f; // Étincelle rouge
                        } else {
                            r = 0.2f + glow * 0.6f;
                            g = 0.05f + glow * 0.2f;
                            b = 0.0f; // Fond incandescent
                        }
                    }
                    case "crystal_cave" -> {
                        float cx = u * 6.0f + (float) Math.sin(v * 2.0f) * 0.3f;
                        float cy = v * 5.0f - time * 0.15f;
                        
                        int cellX = (int) Math.floor(cx);
                        int cellY = (int) Math.floor(cy);
                        float lx = cx - cellX;
                        float ly = cy - cellY;
                        
                        double fHash = Math.abs(Math.sin(cellX * 12.9898 + cellY * 78.233));
                        fHash = fHash - Math.floor(fHash);
                        
                        float shard = Math.abs(lx - 0.5f) + ly * 0.5f;
                        float shine = (float) (Math.sin(time * 2.5f + fHash * 15.0f) * 0.5f + 0.5f);
                        
                        if (shard > 0.8f) {
                            r = 0.5f * (1.0f - shine) + 0.95f * shine;
                            g = 0.2f * (1.0f - shine) + 0.9f * shine;
                            b = 0.8f * (1.0f - shine) + 1.0f * shine;
                        } else if (shard > 0.4f) {
                            float factor = shine * 0.4f;
                            r = 0.3f * (1.0f - factor) + 0.2f * factor;
                            g = 0.1f * (1.0f - factor) + 0.5f * factor;
                            b = 0.5f * (1.0f - factor) + 0.9f * factor;
                        } else if (lx < 0.08f || lx > 0.92f || ly < 0.08f || ly > 0.92f) {
                            r = 0.06f; g = 0.02f; b = 0.12f;
                        } else {
                            r = 0.12f; g = 0.04f; b = 0.22f;
                        }
                    }

                    case "sandstorm" -> {
                        float speedX = 30.0f;
                        float speedY = 10.0f;
                        
                        float xOffset = ix + time * speedX;
                        float yOffset = iy + time * speedY;
                        
                        double wave = Math.sin((u * 15.0 + v * 20.0) - time * 8.0);
                        
                        int virtualX = (int) Math.floor(xOffset);
                        int virtualY = (int) Math.floor(yOffset);
                        double hash = Math.abs(Math.sin(virtualX * 12.9898 + virtualY * 78.233));
                        hash = hash - Math.floor(hash);
                        
                        if (hash > 0.85) {
                            r = 0.95f; g = 0.85f; b = 0.6f; // Grain de sable brillant
                        } else if (wave > 0.5) {
                            r = 0.8f; g = 0.7f; b = 0.4f; // Vague claire
                        } else if (wave > -0.5) {
                            r = 0.7f; g = 0.6f; b = 0.3f; // Base
                        } else {
                            r = 0.6f; g = 0.5f; b = 0.2f; // Vague sombre
                        }
                    }

                    // ========== NOUVEAUX BACKGROUNDS PROCÉDURAUX ==========

                    case "aurora_borealis" -> {
                        // Rubans d'aurore boréale ondulants (vert-bleu-violet) sur fond nuit étoilée
                        float waveBase = (float) Math.sin(u * 8.0 + time * 0.8) * 0.15f
                                       + (float) Math.sin(u * 3.0 - time * 0.5) * 0.1f;
                        float band = v + waveBase;

                        // Trois rubans lumineux à hauteurs différentes
                        float ribbon1 = (float) Math.exp(-Math.pow((band - 0.35f) * 12.0f, 2));
                        float ribbon2 = (float) Math.exp(-Math.pow((band - 0.55f) * 15.0f, 2));
                        float ribbon3 = (float) Math.exp(-Math.pow((band - 0.72f) * 10.0f, 2));
                        float shimmer = (float) Math.sin(time * 2.0 + u * 5.0) * 0.2f + 0.8f;

                        // Étoiles de fond
                        double starSeed = ix * 7.91 + iy * 3.17;
                        double starHash = Math.abs(Math.sin(starSeed)) * 43758.5;
                        starHash = starHash - Math.floor(starHash);
                        float starGlow = starHash > 0.96 ? (float)(starHash - 0.96) * 25.0f : 0f;

                        r = 0.02f + ribbon2 * 0.4f * shimmer + ribbon3 * 0.6f + starGlow;
                        g = 0.04f + ribbon1 * 0.9f * shimmer + ribbon2 * 0.3f * shimmer + starGlow;
                        b = 0.08f + ribbon1 * 0.5f + ribbon3 * 0.7f * shimmer + starGlow;
                    }

                    case "deep_ocean" -> {
                        // Fond abyssal avec rayons de lumière descendants et bioluminescence
                        float depth = 1.0f - v;
                        float baseR = 0.01f + depth * 0.02f;
                        float baseG = 0.03f + depth * 0.08f;
                        float baseB = 0.08f + depth * 0.25f;

                        // Rayons de lumière en colonnes depuis le haut
                        float rayX = u * 6.0f;
                        int rayCell = (int) Math.floor(rayX);
                        float rayFrac = rayX - rayCell;
                        double raySeed = Math.abs(Math.sin(rayCell * 12.9898));
                        raySeed = raySeed - Math.floor(raySeed);

                        float rayIntensity = 0f;
                        if (raySeed > 0.4) {
                            float rayCenter = 0.5f;
                            float rayWidth = 0.15f + (float)raySeed * 0.1f;
                            rayIntensity = (float) Math.exp(-Math.pow((rayFrac - rayCenter) / rayWidth, 2));
                            float rayFade = v * (1.0f + (float)Math.sin(time * 0.5 + rayCell));
                            rayIntensity *= Math.max(0, 1.0f - rayFade * 0.6f);
                        }

                        // Particules bioluminescentes
                        float bioSeed = (float)(ix * 11.37 + ((int)(iy + time * 3.0f)) * 31.41);
                        double bioHash = Math.abs(Math.sin(bioSeed)) * 43758.5;
                        bioHash = bioHash - Math.floor(bioHash);
                        float bio = bioHash > 0.97 ? (float)(bioHash - 0.97) * 33.0f : 0f;
                        float bioPulse = bio * (float)(Math.sin(time * 4.0 + bioHash * 20.0) * 0.3 + 0.7);

                        r = Math.min(1f, baseR + rayIntensity * 0.05f);
                        g = Math.min(1f, baseG + rayIntensity * 0.15f + bioPulse * 0.8f);
                        b = Math.min(1f, baseB + rayIntensity * 0.4f + bioPulse * 1.0f);
                    }

                    case "void_rift" -> {
                        // Vide cosmique noir-violet avec fissures de lumière déchirées
                        float dx = u - 0.5f;
                        float dy = v - 0.5f;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);

                        // Fond vide profond
                        r = 0.02f + dist * 0.05f;
                        g = 0.01f;
                        b = 0.05f + dist * 0.1f;

                        // Trois familles de fissures diagonales distordues
                        float crack1 = Math.abs((float) Math.sin(u * 20.0 + v * 15.0 - time * 0.3));
                        float crack2 = Math.abs((float) Math.sin(u * 12.0 - v * 25.0 + time * 0.5));
                        float crack3 = Math.abs((float) Math.sin((u + v) * 18.0 + time * 0.7));

                        float rift = (float) Math.pow(Math.max(0, 1.0f - crack1 * 8.0f), 4)
                                   + (float) Math.pow(Math.max(0, 1.0f - crack2 * 8.0f), 4) * 0.8f
                                   + (float) Math.pow(Math.max(0, 1.0f - crack3 * 8.0f), 4) * 0.6f;
                        float riftPulse = (float)(Math.sin(time * 3.0 + dist * 5.0) * 0.2 + 0.8);

                        r = Math.min(1f, r + rift * 0.9f * riftPulse);
                        g = Math.min(1f, g + rift * 0.1f);
                        b = Math.min(1f, b + rift * 1.0f * riftPulse);
                    }

                    case "golden_sunset" -> {
                        // Coucher de soleil doré avec nuages horizontaux illuminés
                        // Dégradé ciel: bleu-violet en haut -> rouge-orangé en bas
                        float skyR = 0.55f + v * 0.45f;
                        float skyG = 0.15f + v * 0.45f;
                        float skyB = 0.45f - v * 0.4f;

                        // Soleil lumineux légèrement en bas au centre
                        float sunDx = u - 0.5f;
                        float sunDy = v - 0.78f;
                        float sunDist = (float) Math.sqrt(sunDx * sunDx * 1.5f + sunDy * sunDy * 4.0f);
                        float sunGlow = (float) Math.exp(-sunDist * 8.0f);

                        // Nuages horizontaux
                        double cloud1 = Math.sin(u * 8.0 + time * 0.15) + Math.sin(u * 3.0 - time * 0.1);
                        double cloud2 = Math.sin(u * 6.0 - time * 0.2) + Math.sin(u * 4.0 + time * 0.12);
                        boolean inCloud1 = Math.abs(v - 0.30f + (float)cloud1 * 0.03f) < 0.055f;
                        boolean inCloud2 = Math.abs(v - 0.52f + (float)cloud2 * 0.03f) < 0.045f;

                        if (inCloud1 || inCloud2) {
                            r = 1.0f;
                            g = inCloud1 ? 0.72f : 0.55f;
                            b = inCloud1 ? 0.35f : 0.2f;
                        } else {
                            r = Math.min(1f, skyR + sunGlow * 0.6f);
                            g = Math.min(1f, skyG + sunGlow * 0.35f);
                            b = Math.min(1f, skyB + sunGlow * 0.1f);
                        }
                    }

                    case "cherry_blossom_wind" -> {
                        // Pétales de sakura rose tombant en diagonale sur fond lavande
                        r = 0.97f - v * 0.08f;
                        g = 0.90f - v * 0.04f;
                        b = 0.93f + v * 0.04f;

                        // Pétales : petits clusters animés
                        float windStrength = 0.4f;
                        for (int petal = 0; petal < 10; petal++) {
                            double seedX = Math.abs(Math.sin(petal * 37.91)) * 0.9 + 0.05;
                            double seedY = Math.abs(Math.sin(petal * 13.37)) * 0.9 + 0.05;
                            double seedSpd = 0.4 + (Math.abs(Math.sin(petal * 7.13))) * 0.4;

                            float px = (float)((seedX + time * seedSpd * windStrength) % 1.0);
                            float py = (float)((seedY + time * seedSpd * 0.25) % 1.0);

                            float dist2 = (u - px) * (u - px) * 1600 + (v - py) * (v - py) * 900;
                            if (dist2 < 2.5f) {
                                r = Math.min(1f, r * 0.4f + 0.95f);
                                g = Math.min(1f, g * 0.4f + 0.55f);
                                b = Math.min(1f, b * 0.4f + 0.73f);
                            }
                        }
                    }

                    case "cyber_city" -> {
                        // Gratte-ciels pixélisés la nuit avec fenêtres néon et reflets au sol
                        float groundLevel = 0.82f;

                        // Hash de hauteur de bâtiment par colonne
                        double buildHash = Math.abs(Math.sin(Math.floor(u * 10) * 12.9898)) * 43758.5;
                        buildHash = buildHash - Math.floor(buildHash);
                        float buildHeight = 0.28f + (float)buildHash * 0.42f;
                        boolean inBuilding = v > (groundLevel - buildHeight) && v <= groundLevel;

                        // Ciel nuit
                        float skyR = 0.02f + v * 0.01f;
                        float skyG = 0.03f + v * 0.02f;
                        float skyB = 0.12f + v * 0.05f;

                        if (inBuilding) {
                            r = 0.06f; g = 0.06f; b = 0.10f;

                            // Fenêtres en grille 3×3 par pavé
                            int winX = (int)(u * gridX) % 3;
                            int winY = (int)(v * gridY) % 3;
                            if (winX == 1 && winY == 1) {
                                double winSeed = Math.floor(u * gridX / 3) * 7.91 + Math.floor(v * gridY / 3) * 3.17;
                                double winHash = Math.abs(Math.sin(winSeed)) * 43758.5;
                                winHash = winHash - Math.floor(winHash);
                                double flicker = Math.sin(time * 0.7 + winHash * 20.0);
                                if (winHash > 0.35 && flicker > -0.2) {
                                    if (winHash > 0.80) { r = 1.0f; g = 0.4f; b = 0.1f; }       // orange
                                    else if (winHash > 0.60) { r = 0.2f; g = 0.8f; b = 1.0f; } // cyan
                                    else { r = 0.8f; g = 0.2f; b = 1.0f; }                      // violet
                                }
                            }
                        } else if (v > groundLevel) {
                            // Reflets néon au sol humide
                            float reflectFade = Math.max(0, 1.0f - (v - groundLevel) * 18.0f);
                            r = 0.03f + reflectFade * 0.2f;
                            g = 0.02f + reflectFade * 0.08f;
                            b = 0.05f + reflectFade * 0.3f;
                        } else {
                            // Ciel avec étoiles
                            double starSeed = ix * 7.91 + iy * 3.17;
                            double starHash = Math.abs(Math.sin(starSeed)) * 43758.5;
                            starHash = starHash - Math.floor(starHash);
                            float star = starHash > 0.96 ? (float)(starHash - 0.96) * 25.0f : 0f;
                            r = skyR + star; g = skyG + star; b = skyB + star;
                        }
                    }

                    case "ancient_ruins" -> {
                        // Pavés de pierre avec runes incrustées brillant par vagues
                        float cellSizeX = 5.0f;
                        float cellSizeY = 5.0f;

                        float cellU = u * gridX / cellSizeX;
                        float cellV = v * gridY / cellSizeY;
                        int cx = (int) Math.floor(cellU);
                        int cy = (int) Math.floor(cellV);
                        float lx = cellU - cx;
                        float ly = cellV - cy;

                        double hash = Math.abs(Math.sin(cx * 12.9898 + cy * 78.233)) * 43758.5;
                        hash = hash - Math.floor(hash);

                        float stoneR = 0.35f + (float)hash * 0.12f;
                        float stoneG = 0.30f + (float)hash * 0.10f;
                        float stoneB = 0.25f + (float)hash * 0.08f;

                        boolean isJoint = lx < 0.07f || lx > 0.93f || ly < 0.07f || ly > 0.93f;

                        if (isJoint) {
                            r = 0.15f; g = 0.12f; b = 0.10f; // Joint sombre
                        } else if (hash > 0.68) {
                            // Rune: croisillon + diagonale simplifiée
                            float runeU = (lx - 0.5f) * 2.0f;
                            float runeV = (ly - 0.5f) * 2.0f;
                            boolean runePixel = (Math.abs(runeU) < 0.15f && Math.abs(runeV) < 0.8f)
                                             || (Math.abs(runeV) < 0.15f && Math.abs(runeU) < 0.8f)
                                             || (Math.abs(Math.abs(runeU) - Math.abs(runeV)) < 0.15f && Math.abs(runeU) > 0.2f);
                            if (runePixel) {
                                float runePulse = (float)(Math.sin(time * 2.0 + cx * 1.7 + cy * 2.3) * 0.3 + 0.7);
                                if (hash > 0.88) { r = 0.9f * runePulse; g = 0.6f * runePulse; b = 0.1f * runePulse; } // Or
                                else             { r = 0.3f * runePulse; g = 0.7f * runePulse; b = 0.9f * runePulse; } // Bleu
                            } else {
                                r = stoneR; g = stoneG; b = stoneB;
                            }
                        } else {
                            r = stoneR; g = stoneG; b = stoneB;
                        }
                    }

                    case "frozen_tundra" -> {
                        // Blizzard avec rafales de flocons horizontaux et reflets glacés
                        // Fond dégradé blanc-bleu givré
                        r = 0.60f + v * 0.25f;
                        g = 0.72f + v * 0.18f;
                        b = 0.88f + v * 0.10f;

                        // Traînées de tempête diagonales
                        float blizzardSpeedX = 28.0f;
                        float drift = 0.35f; // angle
                        float sx = u + v * drift - time * blizzardSpeedX / gridX;
                        int virtualSX = (int) Math.floor(sx * gridX);

                        for (int streak = 0; streak < 7; streak++) {
                            double streakSeed = Math.abs(Math.sin(streak * 17.91)) * 0.5 + 0.5;
                            float streakY = (float)streakSeed;
                            float streakLen = 0.03f + (float)(Math.abs(Math.sin(streak * 7.13))) * 0.05f;
                            float streakFade = Math.max(0, 1.0f - Math.abs(v - streakY) / 0.02f);

                            if (streakFade > 0) {
                                float xPhase = (float)((virtualSX + streak * 37) % gridX) / (float)gridX;
                        float xFade = (float) Math.exp(-Math.pow((xPhase - (float)streakSeed) / streakLen, 2));
                                float snow = streakFade * xFade;
                                r = Math.min(1f, r + snow * 0.35f);
                                g = Math.min(1f, g + snow * 0.35f);
                                b = Math.min(1f, b + snow * 0.35f);
                            }
                        }

                        // Flocons ponctuels
                        double flakeSeed = ix * 11.37 + ((int)(iy + time * 6.0f)) * 31.41;
                        double flakeHash = Math.abs(Math.sin(flakeSeed)) * 43758.5;
                        flakeHash = flakeHash - Math.floor(flakeHash);
                        if (flakeHash > 0.97) { r = 1.0f; g = 1.0f; b = 1.0f; }
                    }
                    case "rainbow_highway" -> {
                        // Retro synthwave vaporwave rainbow highway fallback
                        r = 0.06f; g = 0.02f; b = 0.12f; // baseColor
                        
                        float perspective = v;
                        float gX = Math.abs(u - 0.5f) / (perspective + 0.05f);
                        float gY = 1.0f / (perspective + 0.08f) - time * 2.0f;
                        
                        float lineX = Math.abs((gX * 6.0f) % 1.0f - 0.5f);
                        float lineY = Math.abs((gY * 4.0f) % 1.0f - 0.5f);
                        
                        boolean onGrid = (lineX < 0.08f) || (lineY < 0.08f && v > 0.1f);
                        
                        float hue = (gY * 0.1f + time * 0.5f + u * 1.5f) % 3.0f;
                        if (hue < 0.0f) hue += 3.0f;
                        
                        float rr, gg, bb;
                        if (hue < 1.0f) {
                            rr = 1.0f * (1.0f - hue) + 0.1f * hue;
                            gg = 0.1f * (1.0f - hue) + 0.8f * hue;
                            bb = 0.5f * (1.0f - hue) + 1.0f * hue;
                        } else if (hue < 2.0f) {
                            float t2 = hue - 1.0f;
                            rr = 0.1f * (1.0f - t2) + 1.0f * t2;
                            gg = 0.8f * (1.0f - t2) + 0.8f * t2;
                            bb = 1.0f * (1.0f - t2) + 0.1f * t2;
                        } else {
                            float t2 = hue - 2.0f;
                            rr = 1.0f * (1.0f - t2) + 1.0f * t2;
                            gg = 0.8f * (1.0f - t2) + 0.1f * t2;
                            bb = 0.1f * (1.0f - t2) + 0.5f * t2;
                        }
                        
                        if (onGrid) {
                            float mixFactor = perspective * 0.9f + 0.1f;
                            r = r * (1.0f - mixFactor) + rr * mixFactor;
                            g = g * (1.0f - mixFactor) + gg * mixFactor;
                            b = b * (1.0f - mixFactor) + bb * mixFactor;
                        } else {
                            float sunY = Math.abs(v - 0.18f);
                            if (sunY < 0.05f) {
                                float sunIntensity = 1.0f - sunY / 0.05f;
                                float stripes = (float) Math.sin(v * 60.0f);
                                if (stripes > -0.2f) {
                                    r = r * (1.0f - sunIntensity * 0.7f) + 1.0f * (sunIntensity * 0.7f);
                                    g = g * (1.0f - sunIntensity * 0.7f) + 0.2f * (sunIntensity * 0.7f);
                                    b = b * (1.0f - sunIntensity * 0.7f) + 0.6f * (sunIntensity * 0.7f);
                                }
                            }
                        }
                    }
                    case "plasma_storm" -> {
                        // Storm clouds with lightning fallback
                        float baseR = 0.04f * (1.0f - v) + 0.12f * v;
                        float baseG = 0.02f * (1.0f - v) + 0.08f * v;
                        float baseB = 0.08f * (1.0f - v) + 0.22f * v;
                        
                        float flashTrigger = (float) (Math.sin(time * 3.5f) * Math.cos(time * 1.8f + 2.0f));
                        boolean flash = (flashTrigger > 0.75f);
                        
                        if (flash) {
                            baseR = baseR * 0.65f + 0.4f * 0.35f;
                            baseG = baseG * 0.65f + 0.2f * 0.35f;
                            baseB = baseB * 0.65f + 0.6f * 0.35f;
                            
                            int boltSeed = (int) Math.floor(time * 6.0f);
                            double bHash = Math.abs(Math.sin(boltSeed * 12.9898)) * 43758.5;
                            bHash = bHash - Math.floor(bHash);
                            
                            float boltPath = 0.3f + (float)bHash * 0.4f + (float)Math.sin(v * 15.0f + time * 10.0f) * 0.06f;
                            float d = Math.abs(u - boltPath);
                            
                            if (d < 0.015f) {
                                r = 0.95f; g = 0.9f; b = 1.0f;
                            } else if (d < 0.06f) {
                                float glow = 1.0f - (d - 0.015f) / 0.045f;
                                float gr = 0.9f; float gg = 0.6f; float gb = 0.2f; // Gold
                                r = baseR * (1.0f - glow * 0.85f) + gr * (glow * 0.85f);
                                g = baseG * (1.0f - glow * 0.85f) + gg * (glow * 0.85f);
                                b = baseB * (1.0f - glow * 0.85f) + gb * (glow * 0.85f);
                            } else {
                                r = baseR; g = baseG; b = baseB;
                            }
                        } else {
                            float clouds = (float)(Math.sin(u * 8.0f - time * 0.4f) * Math.cos(v * 6.0f + time * 0.3f) * 0.5f + 0.5f);
                            r = baseR * (1.0f - clouds * 0.3f) + 0.16f * (clouds * 0.3f);
                            g = baseG * (1.0f - clouds * 0.3f) + 0.12f * (clouds * 0.3f);
                            b = baseB * (1.0f - clouds * 0.3f) + 0.28f * (clouds * 0.3f);
                        }
                    }
                    case "galactic_supernova" -> {
                        // Supernova expanding galaxy fallback
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        float theta = (float) Math.atan2(dy, dx);
                        
                        float spiral = (float) (Math.sin(theta * 3.0f - radius * 12.0f + time * 1.5f) * 0.5f + 0.5f);
                        float centerGlow = (float) Math.exp(-Math.pow(radius / 0.18f, 2.0f));
                        
                        float mixRatio = centerGlow;
                        float dustR = 0.9f * (1.0f - mixRatio) + 1.0f * mixRatio;
                        float dustG = 0.1f * (1.0f - mixRatio) + 0.7f * mixRatio;
                        float dustB = 0.5f * (1.0f - mixRatio) + 0.15f * mixRatio;
                        
                        double raySeed = Math.abs(Math.sin(Math.floor(theta * 10.0f) * 123.456f));
                        raySeed = raySeed - Math.floor(raySeed);
                        
                        float flares = (float) (Math.sin(theta * 20.0f + time * 4.0f) * Math.cos(theta * 10.0f - time * 2.0f) * 0.5f + 0.5f);
                        float rayIntensity = (float) (flares * Math.exp(-Math.pow(radius / 0.45f, 2.0f)) * (raySeed * 0.5f + 0.5f));
                        
                        float spiralBlend = (float) (spiral * Math.exp(-Math.pow(radius / 0.35f, 2.0f)) * 0.8f + centerGlow * 0.9f);
                        float finalR = 0.03f * (1.0f - spiralBlend) + dustR * spiralBlend;
                        float finalG = 0.01f * (1.0f - spiralBlend) + dustG * spiralBlend;
                        float finalB = 0.07f * (1.0f - spiralBlend) + dustB * spiralBlend;
                        
                        r = finalR * (1.0f - rayIntensity * 0.4f) + 1.0f * (rayIntensity * 0.4f);
                        g = finalG * (1.0f - rayIntensity * 0.4f) + 0.9f * (rayIntensity * 0.4f);
                        b = finalB * (1.0f - rayIntensity * 0.4f) + 0.5f * (rayIntensity * 0.4f);
                    }
                    case "mega_energy" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        float theta = (float) Math.atan2(dy, dx);
                        
                        float swirl = (float) Math.sin(theta * 2.0f - radius * 15.0f + time * 3.5f);
                        float pulse = (float) Math.sin(radius * 12.0f - time * 2.0f);
                        float val = swirl * 0.7f + pulse * 0.3f;
                        
                        if (val > 0.3f) {
                            r = 0.95f; g = 0.05f; b = 0.85f;
                        } else if (val > -0.2f) {
                            r = 0.45f; g = 0.02f; b = 0.75f;
                        } else {
                            r = 0.10f; g = 0.01f; b = 0.25f;
                        }
                    }
                    case "alola_beach" -> {
                        float wave1 = (float) Math.sin(u * 12.0f - time * 1.5f) * 0.04f;
                        float level = v + wave1;
                        
                        if (level > 0.78f) {
                            r = 0.94f; g = 0.85f; b = 0.55f;
                        } else if (level > 0.70f) {
                            r = 0.95f; g = 0.98f; b = 1.0f;
                        } else if (level > 0.35f) {
                            r = 0.05f; g = 0.75f; b = 0.82f;
                        } else {
                            r = 0.35f; g = 0.78f; b = 0.98f;
                        }
                    }
                    case "hisui_ancient" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        
                        // 1. Base parchment color with vignette
                        float vignette = Math.max(0.0f, Math.min(1.0f, radius * 1.3f));
                        float rBase = 0.88f * (1.0f - vignette) + 0.58f * vignette;
                        float gBase = 0.83f * (1.0f - vignette) + 0.48f * vignette;
                        float bBase = 0.72f * (1.0f - vignette) + 0.35f * vignette;
                        
                        // 2. Slow drifting organic ink wash / paper noise
                        float wash = (float)(Math.sin(u * 6.0f + time * 0.2f) * Math.cos(v * 4.0f - time * 0.15f) * 0.04f
                                   + Math.sin((u - v) * 3.0f + time * 0.08f) * 0.02f);
                        r = rBase + wash * 0.6f;
                        g = gBase + wash * 0.4f;
                        b = bBase;
                        
                        // 3. Two concentric rough calligraphy rings (enso) that rotate in opposite directions
                        float angle1 = (float)Math.atan2(dy, dx) - time * 0.6f;
                        float rough1 = (float)(Math.sin(angle1 * 7.0f) * 0.02f + Math.cos(angle1 * 13.0f) * 0.01f);
                        float enso1 = Math.abs(radius - 0.24f + rough1);
                        float stroke1 = Math.max(0.0f, Math.min(1.0f, (0.035f - enso1) / 0.035f)) 
                                      * (0.35f + 0.65f * ((float)Math.sin(angle1) * 0.5f + 0.5f));
                                      
                        float angle2 = (float)Math.atan2(dy, dx) + time * 0.8f;
                        float rough2 = (float)(Math.cos(angle2 * 6.0f) * 0.015f + Math.sin(angle2 * 11.0f) * 0.008f);
                        float enso2 = Math.abs(radius - 0.13f + rough2);
                        float stroke2 = Math.max(0.0f, Math.min(1.0f, (0.025f - enso2) / 0.025f)) 
                                      * (0.4f + 0.6f * ((float)Math.cos(angle2) * 0.5f + 0.5f));
                                      
                        float rCrimson = 0.55f, gCrimson = 0.12f, bCrimson = 0.08f;
                        float rGold = 0.95f, gGold = 0.75f, bGold = 0.20f;
                        
                        if (stroke1 > 0.05f) {
                            float gMix = Math.max(0.0f, Math.min(1.0f, (0.015f - enso1) / 0.015f)) * 0.5f;
                            float rStroke = rCrimson * (1.0f - gMix) + rGold * gMix;
                            float gStroke = gCrimson * (1.0f - gMix) + gGold * gMix;
                            float bStroke = bCrimson * (1.0f - gMix) + bGold * gMix;
                            
                            r = r * (1.0f - stroke1 * 0.95f) + rStroke * stroke1 * 0.95f;
                            g = g * (1.0f - stroke1 * 0.95f) + gStroke * stroke1 * 0.95f;
                            b = b * (1.0f - stroke1 * 0.95f) + bStroke * stroke1 * 0.95f;
                        }
                        if (stroke2 > 0.05f) {
                            float gMix = Math.max(0.0f, Math.min(1.0f, (0.01f - enso2) / 0.01f)) * 0.5f;
                            float rStroke = rCrimson * (1.0f - gMix) + rGold * gMix;
                            float gStroke = gCrimson * (1.0f - gMix) + gGold * gMix;
                            float bStroke = bCrimson * (1.0f - gMix) + bGold * gMix;
                            
                            r = r * (1.0f - stroke2 * 0.95f) + rStroke * stroke2 * 0.95f;
                            g = g * (1.0f - stroke2 * 0.95f) + gStroke * stroke2 * 0.95f;
                            b = b * (1.0f - stroke2 * 0.95f) + bStroke * stroke2 * 0.95f;
                        }
                        
                        // 4. Floating ancient gold leaf particles
                        double flakeSeed = ix * 19.31 + Math.floor(iy + time * 4.5) * 47.13;
                        double flakeHash = Math.abs(Math.sin(flakeSeed));
                        flakeHash = flakeHash - Math.floor(flakeHash);
                        if (flakeHash > 0.982 && radius > 0.08f) {
                            r = 0.92f; g = 0.72f; b = 0.18f;
                        }
                    }
                    case "galar_industrial" -> {
                        float dx = u - 0.32f;
                        float dy = v - 0.68f;
                        
                        // 1. Dark industrial metal plating with grooves and rust grunge
                        float seamX = Math.abs((u * 3.0f) - (float)Math.floor(u * 3.0f) - 0.5f);
                        float seamY = Math.abs((v * 4.0f) - (float)Math.floor(v * 4.0f) - 0.5f);
                        float seam = (seamX < 0.02f || seamY < 0.02f) ? 0.35f : 1.0f;
                        
                        float textureGrunge = (float)(Math.sin(u * 25.0f) * Math.cos(v * 20.0f) * 0.04f 
                                            + Math.cos(u * 8.0f - v * 15.0f) * 0.02f);
                        float rPlate = 0.15f * (0.8f + textureGrunge) * seam;
                        float gPlate = 0.14f * (0.8f + textureGrunge) * seam;
                        float bPlate = 0.14f * (0.8f + textureGrunge) * seam;
                        
                        r = rPlate; g = gPlate; b = bPlate;
                        
                        // Rivets on the plate corners
                        boolean rivet = (ix % 13 == 1 || ix % 13 == 12) && (iy % 10 == 1 || iy % 10 == 9);
                        if (rivet && seam > 0.4f) {
                            r = 0.35f; g = 0.33f; b = 0.30f;
                        }
                        
                        // 2. Volumetric Glowing pipes with cylinder shading
                        float pipeW = 0.05f;
                        boolean inPipeX = Math.abs(dx) < pipeW;
                        boolean inPipeY = Math.abs(dy) < pipeW;
                        
                        boolean drawPipe = false;
                        float rPipe = 0, gPipe = 0, bPipe = 0;
                        
                        if (inPipeX || inPipeY) {
                            drawPipe = true;
                            if (inPipeX && inPipeY) {
                                float d_center = (float)Math.sqrt(dx*dx + dy*dy);
                                if (d_center < pipeW) {
                                    float valShade = (float)Math.sqrt(1.0f - d_center / pipeW);
                                    rPipe = 0.8f * (0.3f + 0.7f * valShade);
                                    gPipe = 0.5f * (0.3f + 0.7f * valShade);
                                    bPipe = 0.1f * (0.3f + 0.7f * valShade);
                                    
                                    float vPulse = (float)(Math.sin(time * 5.0f) * 0.5f + 0.5f);
                                    if (d_center < 0.015f) {
                                        rPipe = 1.0f * vPulse;
                                        gPipe = 0.8f * vPulse;
                                        bPipe = 0.4f * vPulse;
                                    }
                                } else {
                                    drawPipe = false;
                                }
                            } else if (inPipeX) {
                                float norm = dx / pipeW;
                                float shade = (float)Math.sqrt(1.0f - norm * norm);
                                float flow = (float)(Math.sin(v * 25.0f - time * 4.0f) * 0.3f + 0.7f);
                                
                                float rCore = 1.0f * (0.65f + 0.35f * flow);
                                float gCore = 0.45f * (0.65f + 0.35f * flow);
                                float bCore = 0.0f;
                                
                                float rBorder = 0.3f, gBorder = 0.04f, bBorder = 0.0f;
                                rPipe = rBorder * (1.0f - shade) + rCore * shade;
                                gPipe = gBorder * (1.0f - shade) + gCore * shade;
                                bPipe = bBorder * (1.0f - shade) + bCore * shade;
                            } else {
                                float norm = dy / pipeW;
                                float shade = (float)Math.sqrt(1.0f - norm * norm);
                                float flow = (float)(Math.sin(u * 25.0f - time * 4.0f) * 0.3f + 0.7f);
                                
                                float rCore = 1.0f * (0.65f + 0.35f * flow);
                                float gCore = 0.45f * (0.65f + 0.35f * flow);
                                float bCore = 0.0f;
                                
                                float rBorder = 0.3f, gBorder = 0.04f, bBorder = 0.0f;
                                rPipe = rBorder * (1.0f - shade) + rCore * shade;
                                gPipe = gBorder * (1.0f - shade) + gCore * shade;
                                bPipe = bBorder * (1.0f - shade) + bCore * shade;
                            }
                        }
                        
                        if (drawPipe) {
                            r = rPipe; g = gPipe; b = bPipe;
                        }
                        
                        // 3. Dynamic Orange Steam rising
                        float steamDrift = (float)(Math.sin(u * 6.0f - time * 1.5f) * Math.cos(v * 5.0f - time * 2.0f) * 0.5f + 0.5f);
                        if (dy < 0.0f) {
                            float distAbove = Math.abs(dy);
                            float spread = distAbove * 0.45f;
                            float jetPath = dx - (float)Math.sin(v * 16.0f - time * 6.0f) * 0.04f;
                            
                            float jet = Math.max(0.0f, Math.min(1.0f, (0.08f + spread - Math.abs(jetPath)) / (0.08f + spread)))
                                      * Math.max(0.0f, Math.min(1.0f, (0.45f - distAbove) / 0.45f));
                                      
                            r += 0.95f * jet * (0.4f + 0.6f * steamDrift);
                            g += 0.40f * jet * (0.4f + 0.6f * steamDrift);
                            b += 0.05f * jet * (0.4f + 0.6f * steamDrift);
                        }
                        
                        // 4. Glowing furnace glow
                        float furnace = (float)((0.5f + 0.5f * Math.sin(time * 2.0f)) * (v * v * 0.22f));
                        r += 0.3f * furnace;
                        g += 0.08f * furnace;
                    }
                    case "paldea_crystal" -> {
                        float pulse = (float)(Math.sin(time * 2.0f + u * 4.0f + v * 4.0f) * 0.35f + 0.65f);
                        int cellX = ix / 4;
                        int cellY = iy / 4;
                        float hue = (float)((cellX * 13 + cellY * 7) % 10) / 10.0f;
                        
                        float hr = Math.abs(hue * 6.0f - 3.0f) - 1.0f;
                        float hg = 2.0f - Math.abs(hue * 6.0f - 2.0f);
                        float hb = 2.0f - Math.abs(hue * 6.0f - 4.0f);
                        
                        r = Math.max(0.2f, Math.min(1.0f, hr)) * pulse;
                        g = Math.max(0.2f, Math.min(1.0f, hg)) * pulse;
                        b = Math.max(0.2f, Math.min(1.0f, hb)) * pulse;
                    }
                    case "distortion_rift" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        float theta = (float) Math.atan2(dy, dx);
                        
                        float swirl = (float) Math.sin(theta * 3.0f - radius * 12.0f + time * 2.0f);
                        float pulse = (float) Math.cos(radius * 8.0f - time * 1.5f);
                        float val = swirl * 0.6f + pulse * 0.4f;
                        
                        float rBase = 0.06f, gBase = 0.01f, bBase = 0.12f;
                        float rPurple = 0.5f, gPurple = 0.05f, bPurple = 0.7f;
                        float rLime = 0.35f, gLime = 0.85f, bLime = 0.1f;
                        
                        float mixPurple = Math.max(0.0f, Math.min(1.0f, (val + 0.5f) * 0.8f));
                        float rMix1 = rBase * (1.0f - mixPurple) + rPurple * mixPurple;
                        float gMix1 = gBase * (1.0f - mixPurple) + gPurple * mixPurple;
                        float bMix1 = bBase * (1.0f - mixPurple) + bPurple * mixPurple;
                        
                        float mixLime = Math.max(0.0f, Math.min(1.0f, (swirl - 0.3f) * 0.7f));
                        r = rMix1 * (1.0f - mixLime) + rLime * mixLime;
                        g = gMix1 * (1.0f - mixLime) + gLime * mixLime;
                        b = bMix1 * (1.0f - mixLime) + bLime * mixLime;
                        
                        float pSpeed = 0.06f;
                        for (int p = 0; p < 5; p++) {
                            double seed = p * 17.54;
                            double pX = 0.15 + (Math.abs(Math.sin(seed)) % 1.0) * 0.7;
                            double pY = ((0.1 + (Math.abs(Math.cos(seed)) % 1.0) + time * pSpeed * (1.0 + p * 0.2)) % 1.2) - 0.1;
                            
                            double plateW = 0.08 + (Math.abs(Math.sin(seed + 1.0)) % 1.0) * 0.06;
                            double plateH = 0.05 + (Math.abs(Math.cos(seed + 2.0)) % 1.0) * 0.04;
                            pY += Math.sin(time * 2.0 + seed) * 0.02;
                            
                            if (Math.abs(u - pX) < plateW && Math.abs(v - pY) < plateH) {
                                double edgeDist = Math.min(plateW - Math.abs(u - pX), plateH - Math.abs(v - pY));
                                if (edgeDist < 0.012) {
                                    r = 0.12f; g = 0.1f; b = 0.16f;
                                } else {
                                    r = 0.04f; g = 0.03f; b = 0.06f;
                                }
                                break;
                            }
                        }
                    }
                    case "dreamscape" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        
                        float drift = (float)(Math.sin(u * 4.0f - time * 0.5f) * Math.cos(v * 4.0f + time * 0.4f) * 0.5f + 0.5f);
                        float rPink = 0.98f, gPink = 0.65f, bPink = 0.75f;
                        float rPurple = 0.75f, gPurple = 0.58f, bPurple = 0.95f;
                        float rCyan = 0.55f, gCyan = 0.88f, bCyan = 0.92f;
                        
                        float rMix = rPink * (1.0f - drift) + rPurple * drift;
                        float gMix = gPink * (1.0f - drift) + gPurple * drift;
                        float bMix = bPink * (1.0f - drift) + bPurple * drift;
                        
                        float rFade = radius * 0.8f;
                        r = rMix * (1.0f - rFade) + rCyan * rFade;
                        g = gMix * (1.0f - rFade) + gCyan * rFade;
                        b = bMix * (1.0f - rFade) + bCyan * rFade;
                        
                        float bSpeed = 0.07f;
                        for (int b_idx = 0; b_idx < 4; b_idx++) {
                            double seed = b_idx * 23.87;
                            double bX = 0.2 + (Math.abs(Math.sin(seed)) % 1.0) * 0.6 + Math.sin(time * 0.8 + seed) * 0.04;
                            double bY = ((1.1 - time * bSpeed - (Math.abs(Math.cos(seed)) % 1.0)) % 1.3);
                            if (bY < 0) bY += 1.3;
                            bY -= 0.15;
                            
                            double rad = 0.07 + (Math.abs(Math.sin(seed * 3.0)) % 1.0) * 0.05;
                            double dist = Math.sqrt((u - bX)*(u - bX) + (v - bY)*(v - bY) * 1.69);
                            
                            if (dist < rad) {
                                float edge = (float)(dist / rad);
                                if (edge > 0.85f) {
                                    float shineFactor = 0.75f * (edge - 0.85f) / 0.15f;
                                    r = r * (1.0f - shineFactor) + 1.0f * shineFactor;
                                    g = g * (1.0f - shineFactor) + 1.0f * shineFactor;
                                    b = b * (1.0f - shineFactor) + 1.0f * shineFactor;
                                } else {
                                    r += 0.08f * (1.0f - edge);
                                    g += 0.04f * (1.0f - edge);
                                    b += 0.12f * (1.0f - edge);
                                }
                            }
                        }
                    }
                    case "magma_chamber" -> {
                        double h_center = getMagmaHeight(u, v, time);
                        double h_right = getMagmaHeight(u + 0.025, v, time);
                        double h_down = getMagmaHeight(u, v + 0.025, time);
                        
                        double gradX = (h_right - h_center) / 0.025;
                        double gradY = (h_down - h_center) / 0.025;
                        
                        double nx = -gradX;
                        double ny = -gradY;
                        double nz = 1.0;
                        double nLen = Math.sqrt(nx*nx + ny*ny + nz*nz);
                        nx /= nLen;
                        ny /= nLen;
                        nz /= nLen;
                        
                        double lx = -0.5 / 1.224744871391589;
                        double ly = 0.5 / 1.224744871391589;
                        double lz = 1.0 / 1.224744871391589;
                        
                        double hx = lx;
                        double hy = ly;
                        double hz = lz + 1.0;
                        double hLen = Math.sqrt(hx*hx + hy*hy + hz*hz);
                        hx /= hLen;
                        hy /= hLen;
                        hz /= hLen;
                        
                        double dot = nx*hx + ny*hy + nz*hz;
                        double spec = dot > 0.0 ? Math.pow(dot, 12.0) : 0.0;
                        
                        double rObsidian = 0.06, gObsidian = 0.05, bObsidian = 0.08;
                        
                        double distFromCenter = Math.sqrt((u - 0.5) * (u - 0.5) + (v - 0.5) * (v - 0.5));
                        double pulse = Math.sin(time * 2.0 - distFromCenter * 4.0) * 0.5 + 0.5;
                        
                        double rMagma = 0.65 * (1.0 - pulse) + 1.0 * pulse;
                        double gMagma = 0.03 * (1.0 - pulse) + 0.80 * pulse;
                        double bMagma = 0.0 * (1.0 - pulse) + 0.15 * pulse;
                        
                        double rBase = rMagma * (1.0 - h_center) + rObsidian * h_center;
                        double gBase = gMagma * (1.0 - h_center) + gObsidian * h_center;
                        double bBase = bMagma * (1.0 - h_center) + bObsidian * h_center;
                        
                        double rSpec = 1.0, gSpec = 0.85, bSpec = 0.9;
                        
                        r = (float) (rBase + rSpec * spec * 0.45 * h_center);
                        g = (float) (gBase + gSpec * spec * 0.45 * h_center);
                        b = (float) (bBase + bSpec * spec * 0.45 * h_center);
                        
                        double drift = Math.sin(v * 4.0 + time * 1.0) * 2.0;
                        double virtualX = ix + drift;
                        double virtualY = iy + time * 3.0;
                        
                        double emberSeed = virtualX * 23.31 + Math.floor(virtualY) * 53.41;
                        double emberHash = Math.abs(Math.sin(emberSeed));
                        emberHash = emberHash - Math.floor(emberHash);
                        if (emberHash > 0.994) {
                            r = (float) (r * 0.2 + 1.0 * 0.8);
                            g = (float) (g * 0.2 + 0.65 * 0.8);
                            b = (float) (b * 0.2 + 0.15 * 0.8);
                        }
                    }
                    case "stained_glass" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        float theta = (float) Math.atan2(dy, dx);
                        if (theta < 0.0f) theta += 2.0f * (float)Math.PI;

                        float r1 = 0.12f;
                        float r2 = 0.26f;
                        float r3 = 0.42f;
                        float r4 = 0.58f;

                        float thicknessVal = 0.012f;
                        boolean isBorder = Math.abs(radius - r1) < thicknessVal || 
                                           Math.abs(radius - r2) < thicknessVal || 
                                           Math.abs(radius - r3) < thicknessVal || 
                                           Math.abs(radius - r4) < thicknessVal;

                        int ring = 0;
                        int sectors = 0;

                        if (radius < r1) {
                            ring = 0;
                            sectors = 6;
                        } else if (radius < r2) {
                            ring = 1;
                            sectors = 8;
                        } else if (radius < r3) {
                            ring = 2;
                            sectors = 12;
                        } else {
                            ring = 3;
                            sectors = 16;
                        }

                        float sectorAngle = (2.0f * (float)Math.PI) / (float) sectors;
                        float sectorIdx = (float) Math.floor(theta / sectorAngle);
                        float localTheta = theta - sectorAngle * (float) Math.floor(theta / sectorAngle);

                        if (radius >= r1) {
                            float borderDist = Math.min(localTheta, sectorAngle - localTheta) * radius;
                            if (borderDist < thicknessVal) {
                                isBorder = true;
                            }
                        }

                        if (isBorder) {
                            r = 0.06f; g = 0.06f; b = 0.08f;
                        } else {
                            double hashInput = ring * 12.9898 + sectorIdx * 78.233;
                            double cellHashVal = Math.abs(Math.sin(hashInput)) * 43758.5453;
                            float cellHash = (float) (cellHashVal - Math.floor(cellHashVal));

                            float rA, gA, bA, rB, gB, bB;
                            if (cellHash < 0.2f) {
                                rA = 0.8f;  gA = 0.05f; bA = 0.15f;
                                rB = 0.95f; gB = 0.35f; bB = 0.3f;
                            } else if (cellHash < 0.4f) {
                                rA = 0.05f; gA = 0.25f; bA = 0.8f;
                                rB = 0.15f; gB = 0.6f;  bB = 0.95f;
                            } else if (cellHash < 0.6f) {
                                rA = 0.45f; gA = 0.05f; bA = 0.65f;
                                rB = 0.7f;  gB = 0.25f; bB = 0.9f;
                            } else if (cellHash < 0.8f) {
                                rA = 0.05f; gA = 0.65f; bA = 0.25f;
                                rB = 0.35f; gB = 0.9f;  bB = 0.55f;
                            } else {
                                rA = 0.9f;  gA = 0.55f; bA = 0.05f;
                                rB = 1.0f;  gB = 0.82f; bB = 0.25f;
                            }

                            float rayTheta = theta - time * 0.4f;
                            float ray = (float)Math.sin(rayTheta * 3.0f) * 0.5f + 0.5f;
                            float lightIntensity = (float)(ray * Math.exp(-Math.pow(radius / 0.55f, 2.0f)));

                            float glassNoise = (float)(Math.sin(u * 220.0f) * Math.cos(v * 190.0f) * 0.08f + Math.sin(u * 90.0f + v * 120.0f) * 0.04f);

                            float tBlend = (float)(0.5f + 0.5f * Math.sin(time * 0.8f + cellHash * 10.0f));
                            float rGlass = rA * (1.0f - tBlend) + rB * tBlend + glassNoise;
                            float gGlass = gA * (1.0f - tBlend) + gB * tBlend + glassNoise;
                            float bGlass = bA * (1.0f - tBlend) + bB * tBlend + glassNoise;

                            float rGoldLight = 1.0f, gGoldLight = 0.88f, bGoldLight = 0.45f;
                            float rGlassFinal = rGlass * (1.0f - lightIntensity * 0.55f) + rGoldLight * (lightIntensity * 0.55f);
                            float gGlassFinal = gGlass * (1.0f - lightIntensity * 0.55f) + gGoldLight * (lightIntensity * 0.55f);
                            float bGlassFinal = bGlass * (1.0f - lightIntensity * 0.55f) + bGoldLight * (lightIntensity * 0.55f);

                            float ringMinDist = radius < r1 ? r1 - radius : 
                                                (radius < r2 ? Math.min(radius - r1, r2 - radius) : 
                                                (radius < r3 ? Math.min(radius - r2, r3 - radius) : 
                                                               Math.min(radius - r3, r4 - radius)));
                            float sectorMinDist = radius < r1 ? radius : Math.min(localTheta, sectorAngle - localTheta) * radius;
                            float edgeDist = Math.min(ringMinDist, sectorMinDist);
                            
                            float tBevel = Math.max(0.0f, Math.min(1.0f, (edgeDist - 0.0f) / 0.08f));
                            float bevel = tBevel * tBevel * (3.0f - 2.0f * tBevel);

                            r = rGlassFinal * (0.5f + 0.5f * bevel);
                            g = gGlassFinal * (0.5f + 0.5f * bevel);
                            b = bGlassFinal * (0.5f + 0.5f * bevel);
                        }
                    }
                    case "fluid_marble" -> {
                        double h_center = getMarbleHeight(u, v, time);
                        double h_right = getMarbleHeight(u + 0.025, v, time);
                        double h_down = getMarbleHeight(u, v + 0.025, time);
                        
                        double gradX = (h_right - h_center) / 0.025;
                        double gradY = (h_down - h_center) / 0.025;
                        
                        double nx = -gradX;
                        double ny = -gradY;
                        double nz = 1.0;
                        double nLen = Math.sqrt(nx*nx + ny*ny + nz*nz);
                        nx /= nLen;
                        ny /= nLen;
                        nz /= nLen;
                        
                        double lx = -0.5 / 1.224744871391589;
                        double ly = 0.5 / 1.224744871391589;
                        double lz = 1.0 / 1.224744871391589;
                        
                        double hx = lx;
                        double hy = ly;
                        double hz = lz + 1.0;
                        double hLen = Math.sqrt(hx*hx + hy*hy + hz*hz);
                        hx /= hLen;
                        hy /= hLen;
                        hz /= hLen;
                        
                        double dot = nx*hx + ny*hy + nz*hz;
                        double spec = dot > 0.0 ? Math.pow(dot, 16.0) : 0.0;
                        
                        double rBase, gBase, bBase;
                        if (h_center < 0.35) {
                            double t = smoothstep(0.0, 0.35, h_center);
                            rBase = 0.08 * (1.0 - t) + 0.85 * t;
                            gBase = 0.06 * (1.0 - t) + 0.08 * t;
                            bBase = 0.38 * (1.0 - t) + 0.62 * t;
                        } else if (h_center < 0.65) {
                            double t = smoothstep(0.35, 0.65, h_center);
                            rBase = 0.85 * (1.0 - t) + 1.0 * t;
                            gBase = 0.08 * (1.0 - t) + 0.82 * t;
                            bBase = 0.62 * (1.0 - t) + 0.35 * t;
                        } else {
                            double t = smoothstep(0.65, 1.0, h_center);
                            rBase = 1.0 * (1.0 - t) + 0.08 * t;
                            gBase = 0.82 * (1.0 - t) + 0.06 * t;
                            bBase = 0.35 * (1.0 - t) + 0.38 * t;
                        }
                        
                        r = (float) (rBase + 1.0 * spec * 0.6);
                        g = (float) (gBase + 0.92 * spec * 0.6);
                        b = (float) (bBase + 0.75 * spec * 0.6);
                    }
                    case "fossilized_amber" -> {
                        float rBack = 0.98f * (1.0f - v) + 0.72f * v;
                        float gBack = 0.72f * (1.0f - v) + 0.28f * v;
                        float bBack = 0.12f * (1.0f - v) + 0.02f * v;
                        
                        float shimmer = (float) (Math.sin(u * 5.0f - time * 0.5f) * Math.cos(v * 4.0f + time * 0.3f) * 0.5f + 0.5f);
                        r = rBack * (1.0f - shimmer * 0.35f) + 0.85f * shimmer * 0.35f;
                        g = gBack * (1.0f - shimmer * 0.35f) + 0.48f * shimmer * 0.35f;
                        b = bBack * (1.0f - shimmer * 0.35f) + 0.05f * shimmer * 0.35f;
                        
                        float hX = (float) (0.5f + Math.sin(v * 8.0f + time * 0.9f) * 0.08f);
                        float hX2 = (float) (0.5f - Math.sin(v * 8.0f + time * 0.9f) * 0.08f);
                        float distHelix1 = Math.abs(u - hX);
                        float distHelix2 = Math.abs(u - hX2);
                        
                        if ((distHelix1 < 0.015f || distHelix2 < 0.015f) && v > 0.15f && v < 0.85f) {
                            r = r * 0.25f + 1.0f * 0.75f;
                            g = g * 0.25f + 0.88f * 0.75f;
                            b = b * 0.25f + 0.4f * 0.75f;
                        }
                        
                        float crossInterval = (v * 10.0f + time * 0.15f) % 1.0f;
                        if (crossInterval < 0.08f && u > Math.min(hX, hX2) && u < Math.max(hX, hX2) && v > 0.15f && v < 0.85f) {
                            r = r * 0.4f + 1.0f * 0.6f;
                            g = g * 0.4f + 0.88f * 0.6f;
                            b = b * 0.4f + 0.4f * 0.6f;
                        }
                        
                        double debrisSeed = ix * 37.71 + Math.floor(iy - time * 4.0) * 19.53;
                        double debrisHash = Math.abs(Math.sin(debrisSeed));
                        debrisHash = debrisHash - Math.floor(debrisHash);
                        if (debrisHash > 0.982) {
                            r = r * 0.2f + 1.0f * 0.8f;
                            g = g * 0.2f + 0.95f * 0.8f;
                            b = b * 0.2f + 0.6f * 0.8f;
                        }
                        
                        float crack1 = (float) Math.abs(Math.sin(u * 16.0f + v * 12.0f + Math.cos(time * 0.2f) * 0.5f));
                        float crack2 = (float) Math.abs(Math.cos(u * 8.0f - v * 20.0f + Math.sin(time * 0.3f) * 0.4f));
                        float minCrack = Math.min(crack1, crack2);
                        if (minCrack < 0.012f) {
                            float glint = (float) (Math.sin(time * 2.0f + u * 10.0f) * 0.5f + 0.5f);
                            float factor = (1.0f - minCrack / 0.012f) * glint * 0.65f;
                            r = r * (1.0f - factor) + 1.0f * factor;
                            g = g * (1.0f - factor) + 0.88f * factor;
                            b = b * (1.0f - factor) + 0.45f * factor;
                        }
                    }
                }
                
                // Écriture du pixel dans la NativeImage (format ABGR)
                // iy=0 = bas de la carte (py0=-hh), row NativeImage 0 = haut de la texture (UV v=0)
                // On inverse l'axe Y : row = gridY-1-iy
                int cr = (int)(Math.min(Math.max(r, 0), 1) * 255);
                int cg = (int)(Math.min(Math.max(g, 0), 1) * 255);
                int cb = (int)(Math.min(Math.max(b, 0), 1) * 255);
                bgPixels[(gridY - 1 - iy) * gridX + ix] = ProceduralTextureCache.toABGR(cr, cg, cb, 255);
            }
        }

        // Upload la DynamicTexture et rendu d'un seul quad opaque
        // entityCutout = même render type que les backgrounds statiques PNG → 100% compatible Iris
        net.minecraft.resources.ResourceLocation bgTex = ProceduralTextureCache.getBgTexture(bgType, bgPixels, bucket);
        renderQuad(matrix, vertexConsumers.getBuffer(RenderType.entityCutout(bgTex)), light, overlay, w, h);
    }
    
    private static boolean isCustomNewEffect(String effect) {
        return effect.equals("holo_mega") || effect.equals("holo_regional") || effect.equals("holo_time_gears") || effect.equals("holo_spatial_crack") || effect.equals("holo_prism_stars");
    }
    
    private static boolean isProceduralEffect(String effect) {
        return effect.equals("holo_lines") || effect.equals("holo_pulse") || effect.equals("holo_rainbow") || effect.equals("holo_sparkle") ||
               effect.equals("holo_runes") || effect.equals("holo_circuit") || effect.equals("holo_bubbles") ||
               effect.equals("holo_shatter") || effect.equals("holo_ripple") || effect.equals("holo_scanline") ||
               effect.equals("holo_prism") || effect.equals("holo_aurora") || effect.equals("holo_vortex") || effect.equals("holo_lightning") ||
               effect.equals("holo_galaxy") || effect.equals("holo_sakura") || effect.equals("holo_plasma_arc") ||
               effect.equals("holo_diamond") || effect.equals("holo_aura") || effect.equals("holo_neon_pulse") ||
               effect.equals("holo_constellation") || effect.equals("holo_cyber_dust") || effect.equals("holo_magical_wind") ||
               isCustomNewEffect(effect);
    }
    
    private static void renderProceduralEffect(PoseStack matrices, MultiBufferSource vertexConsumers, String effectType, int light, int overlay, float w, float h) {
        Matrix4f matrix = matrices.last().pose();
        long bucket = ProceduralTextureCache.currentBucket();
        ResourceLocation cachedTex = ProceduralTextureCache.getFreshEffectTexture(effectType, bucket);
        if (cachedTex != null) {
            renderQuad(matrix, vertexConsumers.getBuffer(RenderType.entityTranslucent(cachedTex)), light, overlay, w, h);
            return;
        }

        float time = (System.currentTimeMillis() % 10000) / 1000.0f;
        
        // Grille procédurale 40×30
        int gridX = ProceduralTextureCache.WIDTH;   // 40
        int gridY = ProceduralTextureCache.HEIGHT;  // 30

        // Tableau de pixels ABGR — initialisé à 0 (transparent) par défaut
        // Les pixels avec a=0 resteront transparents dans la texture finale
        int[] fxPixels = new int[gridX * gridY];
        
        for (int ix = 0; ix < gridX; ix++) {
            for (int iy = 0; iy < gridY; iy++) {
                float u = (float) ix / gridX;
                float v = (float) iy / gridY;
                
                float r = 1.0f, g = 1.0f, b = 1.0f, a = 0.0f;
                
                switch (effectType) {
                    case "holo_lines" -> {
                        // Rayons lumineux diagonaux dynamiques (Style "V-MAX" ou "Speed Lines")
                        // On crée une coordonnée oblique
                        float oblique = u * 0.8f + v * 0.6f;

                        // Addition de plusieurs ondes pour créer des barres de tailles différentes
                        double wave = Math.sin(oblique * 20.0 - time * 4.0)
                                    + Math.sin(oblique * 35.0 - time * 6.0) * 0.5
                                    + Math.sin(oblique * 5.0 + time * 2.0) * 0.5;

                        // On "hache" le résultat pour avoir des bandes nettes
                        float band = (float) wave;
                        
                        if (band > 1.2f) {
                            // Coeur du rayon (très brillant, presque blanc)
                            r = 0.9f; g = 1.0f; b = 1.0f;
                            a = 0.8f;
                        } else if (band > 0.8f) {
                            // Bordure irisée du rayon
                            float hue = (u + v - time) % 3.0f;
                            if (hue < 0) hue += 3.0f;

                            if (hue < 1.0f) { r = 0.4f; g = 0.8f; b = 1.0f; } // Cyan
                            else if (hue < 2.0f) { r = 1.0f; g = 0.4f; b = 0.8f; } // Magenta
                            else { r = 1.0f; g = 0.8f; b = 0.4f; } // Doré

                            a = 0.5f;
                        } else if (band > 0.4f) {
                            // Lueur résiduelle sombre
                            r = 0.2f; g = 0.3f; b = 0.6f;
                            a = 0.2f;
                        } else {
                            // Zone transparente entre les rayons
                            a = 0.0f;
                        }
                    }
                    case "holo_pulse" -> {
                        // Un cercle de lumière qui grandit au centre et disparaît
                        float dx = u - 0.5f;
                        float dy = v - 0.5f;
                        float dist = (float) Math.sqrt(dx * dx + dy * dy);
                        
                        float wave = (dist * 5.0f - time * 2.0f);
                        float pulse = wave - (float)Math.floor(wave);
                        
                        if (pulse > 0.8f) {
                            // Anneau brillant
                            r = 1.0f; g = 1.0f; b = 0.8f;
                            a = 0.5f * (1.0f - dist * 2.0f); // S'estompe sur les bords
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_rainbow" -> {
                        // Un effet arc-en-ciel diagonal (style carte secrète)
                        float hue = (u * 2.0f + v * 2.0f + time * 1.5f) % 3.0f;
                        
                        if (hue < 1.0f) {
                            r = 1.0f; g = 0.2f + hue * 0.8f; b = 0.2f;
                        } else if (hue < 2.0f) {
                            hue -= 1.0f;
                            r = 1.0f - hue * 0.8f; g = 1.0f; b = 0.2f + hue * 0.8f;
                        } else {
                            hue -= 2.0f;
                            r = 0.2f + hue * 0.8f; g = 1.0f - hue * 0.8f; b = 1.0f;
                        }
                        
                        // Onde d'opacité pour que ça balaye au lieu d'être statique
                        float sweep = (float) Math.sin((u - v) * 5.0f + time * 2.0f);
                        if (sweep > 0.5f) {
                            a = 0.5f;
                        } else {
                            a = 0.1f;
                        }
                    }
                    case "holo_sparkle" -> {
                        // Motif de confettis/éclats géométriques ("Shattered Ice" foil)
                        
                        // On déforme fortement l'espace UV pour faire des triangles/losanges
                        float distU = u + (float)Math.sin(v * 15.0) * 0.05f;
                        float distV = v + (float)Math.cos(u * 15.0) * 0.05f;
                        
                        // Grille avec un pas assez grand pour faire des "gros éclats"
                        int cellX = (int) Math.floor(distU * 8.0f);
                        int cellY = (int) Math.floor(distV * 8.0f);
                        
                        // Générer un timing aléatoire pour chaque éclat
                        double cellHash = Math.abs(Math.sin(cellX * 12.9898 + cellY * 78.233));
                        cellHash = cellHash - Math.floor(cellHash);
                        
                        // On utilise ce hash pour désynchroniser le clignotement
                        float flash = (float) Math.sin(time * 4.0 + cellHash * 20.0);
                        
                        if (flash > 0.8f) {
                            // Très brillant (Blanc/Bleuté)
                            r = 0.9f; g = 0.95f; b = 1.0f;
                            a = 0.7f;
                        } else if (flash > 0.5f) {
                            // Brillant moyen avec des reflets colorés selon la position
                            float hue = (u + v + time) % 3.0f;
                            if (hue < 1.0f) { r = 1.0f; g = 0.5f; b = 0.5f; }
                            else if (hue < 2.0f) { r = 0.5f; g = 1.0f; b = 0.5f; }
                            else { r = 0.5f; g = 0.5f; b = 1.0f; }
                            
                            a = 0.4f;
                        } else {
                            a = 0.0f; // Éteint
                        }
                    }
                    case "holo_runes" -> {
                        // Système de glyphes mystiques (Alphabet Galactique/Zarbi)
                        // On définit la taille d'une "case" pour chaque rune (6x6 pixels avec 1px d'espacement)
                        int runeGridSize = 6;
                        int runeX = ix / runeGridSize;
                        int runeY = iy / runeGridSize;
                        
                        int localX = ix % runeGridSize;
                        int localY = iy % runeGridSize;
                        
                        // Bordures transparentes pour séparer les runes
                        if (localX == 0 || localX == runeGridSize - 1 || localY == 0 || localY == runeGridSize - 1) {
                            a = 0.0f;
                        } else {
                            // On décale de -1 car on a une bordure. Nos runes font donc 4x4 pixels.
                            int px = localX - 1; // 0 à 3
                            int py = localY - 1; // 0 à 3
                            
                            // Hash de la case
                            double cellHash = Math.abs(Math.sin(runeX * 12.9898 + runeY * 78.233));
                            cellHash = cellHash - Math.floor(cellHash);
                            
                            // Animation de pulsation en diagonale
                            float pulse = (float) Math.sin(time * 2.0f + runeX * 0.3f + runeY * 0.5f);
                            
                            if (cellHash > 0.4 && pulse > 0.0f) { // Ne pas remplir toutes les cases
                                
                                // On génère la forme de la rune (4x4 = 16 bits de données)
                                // On utilise le hash pour choisir une forme "déterministe" par case
                                int runeType = (int) (cellHash * 100.0) % 5;
                                
                                boolean activePixel = false;
                                
                                switch (runeType) {
                                    case 0 -> { // Oeil / Carré avec un point
                                        if (px == 0 || px == 3 || py == 0 || py == 3) activePixel = true;
                                        if (px == 1 && py == 1 && cellHash > 0.8) activePixel = true;
                                    }
                                    case 1 -> { // Z / Éclair
                                        if (py == 0 || py == 3) activePixel = true;
                                        if (px + py == 3) activePixel = true; // Diagonale inversée
                                    }
                                    case 2 -> { // Croix / Cible
                                        if (px == 1 || px == 2) activePixel = true;
                                        if (py == 1 || py == 2) activePixel = true;
                                        if (px == 0 && py == 0) activePixel = false;
                                        if (px == 3 && py == 3) activePixel = false;
                                    }
                                    case 3 -> { // Symbole Pi / Table
                                        if (py == 0) activePixel = true;
                                        if (px == 1 || px == 2) activePixel = true;
                                    }
                                    case 4 -> { // Spirale simple
                                        if (px == 0 || (py == 0 && px > 0)) activePixel = true;
                                        if (px == 3 && py > 0) activePixel = true;
                                        if (py == 3 && px > 0) activePixel = true;
                                        if (px == 1 && py == 2) activePixel = true;
                                    }
                                }
                                
                                if (activePixel) {
                                    // Couleurs mystiques (Violet, Magenta, Cyan)
                                    if (cellHash > 0.8) {
                                        r = 0.6f; g = 0.2f; b = 1.0f; // Violet
                                    } else if (cellHash > 0.6) {
                                        r = 1.0f; g = 0.2f; b = 0.8f; // Magenta
                                    } else {
                                        r = 0.2f; g = 0.8f; b = 1.0f; // Cyan
                                    }
                                    
                                    a = 0.6f * pulse;
                                    
                                    // Effet de "Glow" sur certains pixels pendant la pulsation
                                    double pixelHash = Math.abs(Math.sin(ix * 3.14 + iy * 1.59));
                                    pixelHash = pixelHash - Math.floor(pixelHash);
                                    
                                    if (pulse > 0.8f && pixelHash > 0.5) {
                                        r = 1.0f; g = 1.0f; b = 1.0f; // S'illumine en blanc
                                        a = 0.9f;
                                    }
                                } else {
                                    a = 0.0f;
                                }
                            } else {
                                a = 0.0f;
                            }
                        }
                    }
                    case "holo_circuit" -> {
                        // Motif de circuit imprimé qui ne cache pas le Pokémon, effet "données qui circulent"
                        float scaleX = 8.0f; // Grille plus large (moins de bruit)
                        float scaleY = 8.0f;
                        
                        // Décalage pour faire défiler la grille vers le haut/droite
                        float offsetX = time * 0.5f;
                        float offsetY = time * 0.5f;
                        
                        float gridU = u * scaleX - offsetX;
                        float gridV = v * scaleY - offsetY;
                        
                        int cellX = (int) Math.floor(gridU);
                        int cellY = (int) Math.floor(gridV);
                        
                        // Coordonnées locales dans la cellule [0, 1]
                        float localU = gridU - cellX;
                        float localV = gridV - cellY;
                        
                        // Hash déterministe pour chaque "case" du circuit
                        double cellHash = Math.abs(Math.sin(cellX * 12.9898 + cellY * 78.233));
                        cellHash = cellHash - Math.floor(cellHash);
                        
                        // On crée des chemins seulement si le hash est favorable
                        boolean hasHLine = cellHash > 0.5;
                        boolean hasVLine = (cellHash * 10.0 % 1.0) > 0.5;
                        
                        // Est-ce qu'on est sur une ligne du circuit ? (épaisseur fine)
                        float thickness = 0.15f;
                        boolean onHLine = hasHLine && Math.abs(localV - 0.5f) < thickness;
                        boolean onVLine = hasVLine && Math.abs(localU - 0.5f) < thickness;
                        boolean isNode = onHLine || onVLine || (hasHLine && hasVLine && Math.abs(localU - 0.5f) < 0.25f && Math.abs(localV - 0.5f) < 0.25f);
                        
                        if (isNode) {
                            // "Onde de données" qui parcourt le circuit
                            float dataFlow = (float) Math.sin(cellX * 0.5 + cellY * 0.5 + time * 3.0);
                            
                            if (dataFlow > 0.7f) {
                                // Point très brillant (Jaune/Vert)
                                r = 0.8f; g = 1.0f; b = 0.4f;
                                a = 0.8f;
                            } else if (dataFlow > 0.0f) {
                                // Onde principale (Vert fluo clair)
                                r = 0.3f; g = 0.9f; b = 0.5f;
                                a = 0.5f;
                            } else {
                                // Ligne de base (Cyan sombre / Vert très léger)
                                r = 0.1f; g = 0.4f; b = 0.3f;
                                a = 0.2f;
                            }
                        } else {
                            a = 0.0f; // Le reste est complètement transparent
                        }
                    }
                    case "holo_bubbles" -> {
                        // 5 bulles distinctes, calmes, qui montent lentement (évite le bruit)
                        float b1x = 0.25f + (float)Math.sin(time * 0.8f) * 0.05f;
                        float b1y = 1.2f - (time * 0.35f % 1.5f);
                        float r1 = 0.12f;
                        
                        float b2x = 0.75f + (float)Math.cos(time * 0.6f) * 0.08f;
                        float b2y = 1.2f - (time * 0.45f % 1.5f);
                        float r2 = 0.09f;
                        
                        float b3x = 0.5f + (float)Math.sin(time * 1.1f) * 0.12f;
                        float b3y = 1.2f - (time * 0.3f % 1.5f);
                        float r3 = 0.15f;
                        
                        float b4x = 0.15f + (float)Math.cos(time * 1.3f) * 0.05f;
                        float b4y = 1.2f - (time * 0.55f % 1.5f);
                        float r4 = 0.06f;
                        
                        float b5x = 0.85f + (float)Math.sin(time * 0.9f) * 0.04f;
                        float b5y = 1.2f - (time * 0.65f % 1.5f);
                        float r5 = 0.07f;
                        
                        float d1 = (float)Math.sqrt((u - b1x)*(u - b1x) + (v - b1y)*(v - b1y));
                        float d2 = (float)Math.sqrt((u - b2x)*(u - b2x) + (v - b2y)*(v - b2y));
                        float d3 = (float)Math.sqrt((u - b3x)*(u - b3x) + (v - b3y)*(v - b3y));
                        float d4 = (float)Math.sqrt((u - b4x)*(u - b4x) + (v - b4y)*(v - b4y));
                        float d5 = (float)Math.sqrt((u - b5x)*(u - b5x) + (v - b5y)*(v - b5y));
                        
                        float d = 1.0f;
                        float bx = 0, by = 0, rad = 0;
                        
                        // Détermine dans quelle bulle on se trouve (la plus petite en priorité si chevauchement)
                        if (d1 < r1) { d = d1; bx = b1x; by = b1y; rad = r1; }
                        else if (d2 < r2) { d = d2; bx = b2x; by = b2y; rad = r2; }
                        else if (d3 < r3) { d = d3; bx = b3x; by = b3y; rad = r3; }
                        else if (d4 < r4) { d = d4; bx = b4x; by = b4y; rad = r4; }
                        else if (d5 < r5) { d = d5; bx = b5x; by = b5y; rad = r5; }
                        
                        if (d < rad) {
                            float thickness = 0.03f;
                            if (d > rad - thickness) {
                                // Contour de la bulle (Cyan clair)
                                r = 0.4f; g = 0.8f; b = 1.0f;
                                a = 0.5f;
                            } else {
                                // Reflet spéculaire en haut à gauche
                                float nx = (u - bx) / rad;
                                float ny = (v - by) / rad;
                                if (nx < -0.3f && ny < -0.3f && d > rad - thickness - 0.05f) {
                                    r = 1.0f; g = 1.0f; b = 1.0f; // Blanc pur
                                    a = 0.6f;
                                } else {
                                    // Centre complètement transparent pour la visibilité du Pokémon
                                    a = 0.0f; 
                                }
                            }
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_shatter" -> {
                        // "Cracked Ice" foil — Lignes de fracture irisées sur fond transparent
                        // 3 ondes obliques avec des angles différents pour créer un réseau de fissures
                        double fracture1 = Math.abs(Math.sin(u * 12.0 + v * 6.0 - time * 0.4));
                        double fracture2 = Math.abs(Math.cos(u * 7.0 - v * 14.0 + time * 0.3));
                        double fracture3 = Math.abs(Math.sin(u * 4.0 + v * 18.0 + time * 0.6));
                        
                        // Trouver la fracture la plus proche
                        double minFracture = Math.min(fracture1, Math.min(fracture2, fracture3));
                        
                        // Balayage lumineux diagonal qui traverse la carte
                        float sweep = (float) Math.sin(u * 6.0 - v * 4.0 + time * 2.5);
                        
                        if (minFracture < 0.04) {
                            // Cœur de la fissure — couleur irisée selon la position
                            float hueS = (u * 2.0f + v * 1.5f + time * 0.8f) % 3.0f;
                            if (hueS < 1.0f) {
                                r = 0.7f; g = 0.9f; b = 1.0f; // Cyan glacé
                            } else if (hueS < 2.0f) {
                                r = 1.0f; g = 0.8f; b = 0.9f; // Rose nacré
                            } else {
                                r = 0.8f; g = 1.0f; b = 0.85f; // Vert menthe
                            }
                            a = 0.7f;
                        } else if (minFracture < 0.1) {
                            // Lueur autour de la fissure (halo doux)
                            float glow = 1.0f - (float)(minFracture - 0.04) / 0.06f;
                            r = 0.6f; g = 0.8f; b = 1.0f;
                            a = 0.25f * glow;
                        } else if (sweep > 0.85f) {
                            // Reflet spéculaire qui balaye l'intérieur des morceaux (très bref)
                            r = 1.0f; g = 0.95f; b = 0.9f;
                            a = 0.2f;
                        } else {
                            // Intérieur complètement transparent — le Pokémon reste visible
                            a = 0.0f;
                        }
                    }
                    case "holo_ripple" -> {
                        // Un motif holographique Premium à la "Pokémon Full Art" ou "Gold Star"
                        // Des ondulations horizontales très fines et discrètes (style métal brossé très propre)
                        
                        float wave1 = (float) Math.sin(u * 5.0f + time * 1.5f) * 0.15f;
                        float wave2 = (float) Math.cos(u * 12.0f - time * 0.8f) * 0.05f;
                        
                        float rippleLine = (v * 12.0f - u * 4.0f - time * 2.0f + wave1 + wave2);
                        float lineInPixel = rippleLine - (float)Math.floor(rippleLine);
                        
                        if (lineInPixel > 0.94f) {
                            // Crête de l'énergie très fine, blanc nacré translucide
                            r = 1.0f; g = 0.95f; b = 0.9f;
                            a = 0.35f;
                        } else if (lineInPixel > 0.85f) {
                            // Flanc de l'énergie très doux (dégradé doré / orange très translucide)
                            float fade = (lineInPixel - 0.85f) / 0.09f; // [0, 1]
                            r = 0.9f; g = 0.6f + 0.3f * fade; b = 0.2f;
                            a = 0.15f * fade;
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_scanline" -> {
                        // Un effet beaucoup plus doux de lignes de "données" discrètes
                        
                        // 1. Une ligne très fine et transparente qui balaye de haut en bas lentement
                        float scanY1 = (v - time * 0.3f) % 1.0f;
                        if (scanY1 < 0) scanY1 += 1.0f;
                        
                        // 2. Une autre ligne qui balaye plus vite
                        float scanY2 = (v - time * 0.8f) % 1.0f;
                        if (scanY2 < 0) scanY2 += 1.0f;
                        
                        // Vérifier si le pixel courant est sur la ligne (épaisseur 1 pixel -> environ 0.033 en UV)
                        boolean onLine1 = scanY1 < 0.05f;
                        boolean onLine2 = scanY2 < 0.033f;
                        
                        if (onLine1) {
                            r = 0.4f; g = 1.0f; b = 0.8f; // Cyan doux
                            a = 0.3f; // Très transparent
                        } else if (onLine2) {
                            r = 1.0f; g = 0.4f; b = 0.8f; // Magenta doux
                            a = 0.2f;
                        } else {
                            // Très léger quadrillage permanent en fond
                            if (ix % 4 == 0 || iy % 4 == 0) {
                                r = 0.8f; g = 0.9f; b = 1.0f;
                                a = 0.05f; // À peine visible
                            } else {
                                a = 0.0f;
                            }
                        }
                        
                        // Petit scintillement occasionnel sur les intersections de la grille
                        if (ix % 4 == 0 && iy % 4 == 0) {
                            double hash = Math.abs(Math.sin(ix * 12.9898 + iy * 78.233 + time));
                            hash = hash - Math.floor(hash);
                            if (hash > 0.95) {
                                r = 1.0f; g = 1.0f; b = 1.0f;
                                a = 0.4f;
                            }
                        }
                    }
                    case "holo_prism" -> {
                        // Facettes prismatiques — Seulement les arêtes + reflet glissant
                        // Le Pokémon reste parfaitement visible à travers les facettes
                        float prismU = u * 6.0f;
                        float prismV = v * 5.0f;
                        
                        // Décalage des rangées paires pour créer un motif triangulaire
                        int rowP = (int) Math.floor(prismV);
                        if (rowP % 2 == 0) prismU += 0.5f;
                        
                        int colP = (int) Math.floor(prismU);
                        float localPU = prismU - colP;
                        float localPV = prismV - rowP;
                        
                        // Déterminer dans quel triangle on est (haut ou bas de la cellule)
                        boolean upperTriangle = (localPU + localPV) < 1.0f;
                        int triId = colP * 2 + rowP * 13 + (upperTriangle ? 0 : 1);
                        
                        // Hash déterministe pour chaque facette
                        double facetHash = Math.abs(Math.sin(triId * 12.9898 + 78.233));
                        facetHash = facetHash - Math.floor(facetHash);
                        
                        // Reflet spéculaire diagonal qui glisse sur la carte
                        float sweepP = (float) Math.sin(u * 8.0 - v * 3.0 + time * 2.5);
                        
                        // Distance aux bords des facettes (arêtes fines)
                        float edgeDistH = Math.min(localPV, 1.0f - localPV);
                        float edgeDistV = Math.min(localPU, 1.0f - localPU);
                        float edgeDistDiag = Math.abs(localPU + localPV - 1.0f) / 1.414f;
                        float minEdgeDist = Math.min(edgeDistH, Math.min(edgeDistV, edgeDistDiag));
                        
                        // Couleur irisée des arêtes (arc-en-ciel qui change avec le temps)
                        float hueP = (float)((facetHash * 3.0 + time * 0.8) % 3.0);
                        if (hueP < 1.0f) {
                            r = 0.7f; g = 0.9f; b = 1.0f; // Cyan
                        } else if (hueP < 2.0f) {
                            r = 1.0f; g = 0.7f; b = 0.9f; // Rose
                        } else {
                            r = 0.9f; g = 1.0f; b = 0.7f; // Vert clair
                        }
                        
                        if (minEdgeDist < 0.06f) {
                            // Arête de la facette — fine ligne irisée
                            float edgeIntensity = 1.0f - minEdgeDist / 0.06f;
                            r = 0.9f; g = 0.95f; b = 1.0f;
                            a = 0.5f * edgeIntensity;
                        } else if (sweepP > 0.8f && minEdgeDist > 0.15f) {
                            // Reflet spéculaire qui passe dans la facette (très bref, très léger)
                            float sweepIntensity = (sweepP - 0.8f) / 0.2f;
                            a = 0.3f * sweepIntensity;
                        } else {
                            // Intérieur transparent — le Pokémon est parfaitement visible
                            a = 0.0f;
                        }
                    }
                    case "holo_aurora" -> {
                        // Aurore boréale - bandes de lumière ondulantes horizontales
                        // Plusieurs couches d'ondes sinusoïdales superposées
                        
                        // Onde principale lente et large
                        float wave1A = (float) Math.sin(u * 3.0 + time * 0.6) * 0.15f;
                        float wave2A = (float) Math.sin(u * 7.0 - time * 1.2) * 0.08f;
                        float wave3A = (float) Math.cos(u * 5.0 + time * 0.9) * 0.1f;
                        
                        // Position du "rideau" d'aurore
                        float curtain1 = v - 0.3f + wave1A + wave2A;
                        float curtain2 = v - 0.5f + wave2A + wave3A;
                        float curtain3 = v - 0.7f + wave1A + wave3A;
                        
                        // Intensité de chaque rideau (forme gaussienne aplatie)
                        float i1 = Math.max(0, 1.0f - Math.abs(curtain1) * 8.0f);
                        float i2 = Math.max(0, 1.0f - Math.abs(curtain2) * 10.0f);
                        float i3 = Math.max(0, 1.0f - Math.abs(curtain3) * 8.0f);
                        
                        // Scintillement vertical (les rideaux d'aurore ont des "colonnes" de lumière)
                        float shimmer = (float) Math.sin(v * 40.0 + u * 5.0 + time * 3.0) * 0.5f + 0.5f;
                        i1 *= (0.7f + shimmer * 0.3f);
                        i2 *= (0.7f + shimmer * 0.3f);
                        i3 *= (0.7f + shimmer * 0.3f);
                        
                        // Combinaison des 3 rideaux avec des couleurs différentes
                        // Vert dominant (aurore classique)
                        float rA = i1 * 0.2f + i2 * 0.5f + i3 * 0.8f;
                        float gA = i1 * 1.0f + i2 * 0.3f + i3 * 0.2f;
                        float bA = i1 * 0.4f + i2 * 0.9f + i3 * 1.0f;
                        
                        r = Math.min(1.0f, rA);
                        g = Math.min(1.0f, gA);
                        b = Math.min(1.0f, bA);
                        
                        float totalIntensity = Math.min(1.0f, i1 + i2 + i3);
                        
                        if (totalIntensity > 0.7f) {
                            a = 0.6f;
                        } else if (totalIntensity > 0.3f) {
                            a = 0.35f * totalIntensity;
                        } else if (totalIntensity > 0.05f) {
                            a = 0.15f * totalIntensity;
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_vortex" -> {
                        // Vortex d'énergie — traînées fines et fluides spiralant vers le centre
                        float dxV = u - 0.5f;
                        float dyV = v - 0.5f;
                        float angleV = (float) Math.atan2(dyV, dxV);
                        float radiusV = (float) Math.sqrt(dxV * dxV + dyV * dyV);
                        
                        // Plusieurs bras de spirale fins et fluides
                        float spiral1 = angleV * 2.0f - radiusV * 18.0f + time * 3.0f;
                        float spiral2 = angleV * 2.0f - radiusV * 18.0f + time * 3.0f + (float)Math.PI * 0.66f;
                        float spiral3 = angleV * 2.0f - radiusV * 18.0f + time * 3.0f + (float)Math.PI * 1.33f;
                        
                        // Convertir en position dans le cycle [0, 1]
                        float band1 = spiral1 - (float)Math.floor(spiral1);
                        float band2 = spiral2 - (float)Math.floor(spiral2);
                        float band3 = spiral3 - (float)Math.floor(spiral3);
                        
                        // Intensité de chaque bras (forme fine avec dégradé doux)
                        float arm1 = band1 > 0.85f ? (band1 - 0.85f) / 0.15f : (band1 > 0.75f ? (0.85f - band1) / 0.10f * 0.4f : 0);
                        float arm2 = band2 > 0.85f ? (band2 - 0.85f) / 0.15f : (band2 > 0.75f ? (0.85f - band2) / 0.10f * 0.4f : 0);
                        float arm3 = band3 > 0.85f ? (band3 - 0.85f) / 0.15f : (band3 > 0.75f ? (0.85f - band3) / 0.10f * 0.4f : 0);
                        
                        // Atténuation radiale (le vortex s'estompe vers les bords)
                        float radialFade = Math.max(0, 1.0f - radiusV * 1.8f);
                        arm1 *= radialFade;
                        arm2 *= radialFade;
                        arm3 *= radialFade;
                        
                        // Couleur continue basée sur l'angle (dégradé fluide)
                        float hueV = (angleV / (float)(Math.PI * 2.0) + 0.5f + time * 0.2f) % 1.0f;
                        if (hueV < 0) hueV += 1.0f;
                        
                        // Dégradé continu violet -> cyan -> magenta -> violet
                        float rV, gV, bV;
                        if (hueV < 0.33f) {
                            float t2 = hueV / 0.33f;
                            rV = 0.5f * (1.0f - t2); gV = 0.1f + 0.7f * t2; bV = 0.9f + 0.1f * t2;
                        } else if (hueV < 0.66f) {
                            float t2 = (hueV - 0.33f) / 0.33f;
                            rV = 0.0f + 1.0f * t2; gV = 0.8f * (1.0f - t2); bV = 1.0f - 0.2f * t2;
                        } else {
                            float t2 = (hueV - 0.66f) / 0.34f;
                            rV = 1.0f - 0.5f * t2; gV = 0.0f + 0.1f * t2; bV = 0.8f + 0.1f * t2;
                        }
                        
                        // Combinaison des 3 bras
                        float totalArm = Math.min(1.0f, arm1 + arm2 + arm3);
                        
                        // Petit point brillant au centre
                        float centerGlow = Math.max(0, 1.0f - radiusV * 8.0f);
                        
                        if (centerGlow > 0.3f) {
                            // Point central brillant (petit et discret)
                            r = 1.0f; g = 0.95f; b = 1.0f;
                            a = 0.5f * centerGlow;
                        } else if (totalArm > 0.05f) {
                            r = rV; g = gV; b = bV;
                            // Les crêtes sont plus brillantes (blanc nacré)
                            if (totalArm > 0.7f) {
                                float whiteMix = (totalArm - 0.7f) / 0.3f;
                                r = r + (1.0f - r) * whiteMix * 0.5f;
                                g = g + (1.0f - g) * whiteMix * 0.5f;
                                b = b + (1.0f - b) * whiteMix * 0.5f;
                            }
                            a = 0.5f * totalArm;
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_lightning" -> {
                        // Éclairs électriques qui crépitent sur la carte
                        // On génère plusieurs "branches" d'éclairs pseudo-aléatoires
                        
                        a = 0.0f; // Par défaut transparent
                        
                        // On crée 3 éclairs principaux à des positions différentes
                        for (int bolt = 0; bolt < 3; bolt++) {
                            // Chaque éclair a une phase temporelle différente
                            float boltTime = time + bolt * 3.33f;
                            float boltPhase = (boltTime * 1.5f) % 3.0f;
                            
                            // L'éclair n'est visible que pendant une courte fenêtre (effet flash)
                            float flashIntensity = 0;
                            if (boltPhase < 0.15f) {
                                flashIntensity = 1.0f; // Flash principal
                            } else if (boltPhase < 0.25f) {
                                flashIntensity = 0.5f; // Rémanence
                            } else if (boltPhase > 1.0f && boltPhase < 1.1f) {
                                flashIntensity = 0.7f; // Deuxième flash (double strike)
                            }
                            
                            if (flashIntensity > 0) {
                                // Position de départ de l'éclair (haut de la carte)
                                double boltSeed = Math.abs(Math.sin(bolt * 45.678 + Math.floor(boltTime * 0.5) * 12.345));
                                boltSeed = boltSeed - Math.floor(boltSeed);
                                float startX = 0.1f + (float)boltSeed * 0.8f;
                                
                                // On trace l'éclair du haut vers le bas
                                // Pour chaque rangée, on décale la position X de façon pseudo-aléatoire (zigzag)
                                float boltX = startX;
                                
                                for (int scanRow = 0; scanRow <= iy; scanRow++) {
                                    double rowHash = Math.abs(Math.sin(scanRow * 7.654 + bolt * 23.456 + Math.floor(boltTime * 0.5) * 5.678));
                                    rowHash = rowHash - Math.floor(rowHash);
                                    boltX += (float)(rowHash - 0.5) * 0.08f;
                                }
                                
                                // Vérifier si le pixel est sur le tracé de l'éclair
                                float distToBolt = Math.abs(u - boltX);
                                
                                if (distToBolt < 0.025f) {
                                    // Coeur de l'éclair (blanc pur)
                                    r = 1.0f; g = 1.0f; b = 1.0f;
                                    a = Math.max(a, 0.85f * flashIntensity);
                                } else if (distToBolt < 0.06f) {
                                    // Halo électrique (bleu/cyan)
                                    r = 0.4f; g = 0.7f; b = 1.0f;
                                    a = Math.max(a, 0.5f * flashIntensity * (1.0f - (distToBolt - 0.025f) / 0.035f));
                                } else if (distToBolt < 0.1f) {
                                    // Lueur ambiante (violet très léger)
                                    if (a < 0.1f) {
                                        r = 0.3f; g = 0.1f; b = 0.6f;
                                        a = 0.15f * flashIntensity * (1.0f - (distToBolt - 0.06f) / 0.04f);
                                    }
                                }
                                
                                // Branches secondaires (petits zigzags qui partent du tronc principal)
                                if (iy % 5 == 0 && distToBolt < 0.15f) {
                                    double branchHash = Math.abs(Math.sin(iy * 3.14 + bolt * 9.87 + Math.floor(boltTime * 0.5) * 4.56));
                                    branchHash = branchHash - Math.floor(branchHash);
                                    
                                    if (branchHash > 0.4) {
                                        float branchDir = branchHash > 0.7 ? 1.0f : -1.0f;
                                        float distBranch = Math.abs(u - (boltX + branchDir * 0.05f * ((float)iy % 5)));
                                        if (distBranch < 0.02f) {
                                            r = 0.7f; g = 0.85f; b = 1.0f;
                                            a = Math.max(a, 0.5f * flashIntensity);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    case "holo_galaxy" -> {
                        // Nébuleuse cosmique très douce : étoiles scintillantes discrètes + nuages de gaz hyper-translucides (très propre)
                        double cloud1 = Math.sin(u * 6.0 + time * 0.4) * Math.cos(v * 5.0 - time * 0.3);
                        double cloud2 = Math.sin(u * 11.0 - time * 0.7 + v * 3.0) * 0.5;
                        double cloud3 = Math.cos((u + v) * 8.0 + time * 0.5) * 0.3;
                        double nebula = (cloud1 + cloud2 + cloud3) / 1.8;
                        
                        double starHash = Math.abs(Math.sin(ix * 127.1 + iy * 311.7));
                        starHash = starHash - Math.floor(starHash);
                        double twinkle = Math.sin(time * 5.0 + starHash * 20.0);
                        
                        if (starHash > 0.985) {
                            // Étoile principale scintillante
                            float starIntensity = (float)(twinkle * 0.5 + 0.5);
                            r = 0.85f + 0.15f * starIntensity; g = 0.9f + 0.1f * starIntensity; b = 1.0f;
                            a = 0.7f * starIntensity;
                        } else if (starHash > 0.975) {
                            // Petite étoile scintillante
                            r = 0.7f; g = 0.85f; b = 1.0f;
                            a = 0.4f * (float)(twinkle * 0.4 + 0.6);
                        } else if (nebula > 0.6) {
                            // Nuage de gaz très léger et subtil
                            float t2 = (float)((nebula - 0.6) / 0.4);
                            r = 0.9f; g = 0.2f + t2 * 0.3f; b = 0.8f + t2 * 0.2f;
                            a = 0.08f * t2;
                        } else if (nebula > 0.3) {
                            // Nuage de gaz bleu ultra-translucide
                            float t2 = (float)((nebula - 0.3) / 0.3);
                            r = 0.3f; g = 0.5f + t2 * 0.3f; b = 1.0f;
                            a = 0.05f * t2;
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_sakura" -> {
                        // Pétales de cerisier qui tombent doucement
                        a = 0.0f;
                        float[][] petals = {
                            {0.15f, 0.55f, 0.07f, 0.65f, 0.5f},
                            {0.40f, 0.20f, 0.06f, 0.45f, 0.7f},
                            {0.65f, 0.80f, 0.08f, 0.55f, 0.4f},
                            {0.85f, 0.40f, 0.05f, 0.70f, 0.9f},
                            {0.30f, 0.95f, 0.09f, 0.40f, 0.6f},
                            {0.75f, 0.10f, 0.06f, 0.60f, 0.8f},
                        };
                        for (float[] p : petals) {
                            float pStartX = p[0], pPhase = p[1], pRad = p[2], pFall = p[3], pSway = p[4];
                            float py = ((pPhase + time * pFall) % 1.2f) - 0.1f;
                            float px = pStartX + (float)Math.sin(time * pSway + pPhase * 6.28f) * 0.08f;
                            float rot = time * pSway * 0.5f + pPhase * 3.14f;
                            float cosR = (float)Math.cos(rot), sinR = (float)Math.sin(rot);
                            float dx = (u - px) * cosR + (v - py) * sinR;
                            float dy = -(u - px) * sinR + (v - py) * cosR;
                            float petalDist = (float)Math.sqrt((dx / pRad) * (dx / pRad) + (dy / (pRad * 1.6f)) * (dy / (pRad * 1.6f)));
                            if (petalDist < 1.0f) {
                                float edge = 1.0f - petalDist;
                                float lightness = 0.7f + edge * 0.3f;
                                r = Math.min(1.0f, 1.0f * lightness); g = Math.min(1.0f, 0.55f * lightness); b = Math.min(1.0f, 0.7f * lightness);
                                a = Math.max(a, 0.55f * edge * edge);
                            } else if (petalDist < 1.15f) {
                        float glow = 1.0f - (petalDist - 1.0f) / 0.15f;
                                if (a < 0.1f) { r = 1.0f; g = 0.7f; b = 0.8f; a = 0.15f * glow; }
                            }
                        }
                    }
                    case "holo_plasma_arc" -> {
                        // Arcs d'énergie plasma - éruptions solaires courbées distribuées sur toute la carte
                        a = 0.0f;
                        for (int arc = 0; arc < 4; arc++) {
                            float arcSeed = arc * 0.25f;
                            float arcTime = time * 0.8f + arcSeed * 6.28f;
                            
                            float arcAnchorX = 0.15f + arc * 0.22f + (float)Math.sin(arcTime * 0.3f) * 0.05f;
                            float arcAnchorY = (arc == 0 || arc == 2) ? 0.95f : 0.05f;
                            
                            float arcPeakX = arcAnchorX + (float)Math.sin(arcTime * 0.7f + arcSeed) * 0.18f;
                            float arcPeakY = 0.3f + arc * 0.12f + (float)Math.cos(arcTime * 0.5f + arcSeed) * 0.15f;
                            
                            float arcEndX = arcAnchorX + (float)Math.sin(arcTime * 0.6f + arcSeed + 1.0f) * 0.18f;
                            float arcEndY = (arc == 0 || arc == 3) ? 0.05f : 0.95f;
                            
                            float minArcDist = 9999f;
                            for (int s = 0; s <= 20; s++) {
                                float t2 = (float)s / 20;
                                float bx = (1-t2)*(1-t2)*arcAnchorX + 2*(1-t2)*t2*arcPeakX + t2*t2*arcEndX;
                                float by = (1-t2)*(1-t2)*arcAnchorY + 2*(1-t2)*t2*arcPeakY + t2*t2*arcEndY;
                                float ddx = u - bx, ddy = v - by;
                                float dist = (float)Math.sqrt(ddx*ddx + ddy*ddy);
                                if (dist < minArcDist) minArcDist = dist;
                            }
                            float pulse = (float)(Math.sin(arcTime * 3.0 + arc) * 0.5 + 0.5);
                            if (minArcDist < 0.015f) {
                                r = 1.0f; g = 0.9f; b = 0.5f;
                                a = Math.max(a, 0.65f * pulse);
                            } else if (minArcDist < 0.035f) {
                                float fade = 1.0f - (minArcDist - 0.015f) / 0.02f;
                                r = 1.0f; g = 0.4f; b = 0.1f;
                                a = Math.max(a, 0.45f * fade * pulse);
                            } else if (minArcDist < 0.06f) {
                                float fade = 1.0f - (minArcDist - 0.035f) / 0.025f;
                                if (a < 0.15f) { r = 0.8f; g = 0.1f; b = 0.5f; a = 0.15f * fade * pulse; }
                            }
                        }
                    }
                    case "holo_diamond" -> {
                        // Reflets de taille de diamant — éclats spéculaires vifs et irisés (très discrets)
                        float du = (u - v) * 4.0f, dv = (u + v) * 4.0f;
                        int cellU = (int) Math.floor(du), cellV = (int) Math.floor(dv);
                        float lu = du - cellU, lv = dv - cellV;
                        double fHash = Math.abs(Math.sin(cellU * 17.123 + cellV * 31.456));
                        fHash = fHash - Math.floor(fHash);
                        float lightAngle = time * 1.2f;
                        float nx = (float)(fHash - 0.5) * 0.6f;
                        float ny = (float)((fHash * 7.0 % 1.0) - 0.5) * 0.6f;
                        float nz = (float)Math.sqrt(Math.max(0, 1.0 - nx*nx - ny*ny));
                        float specular = Math.max(0, (float)Math.cos(lightAngle) * nx + (float)Math.sin(lightAngle) * ny + 0.8f * nz);
                        float specPow = specular * specular * specular * specular;
                        float minEdge = Math.min(Math.min(lu, 1.0f - lu), Math.min(lv, 1.0f - lv));
                        if (minEdge < 0.05f) {
                            float hue = (float)((fHash * 3.0 + time * 0.5) % 3.0);
                            if (hue < 1.0f) { r = 0.8f; g = 0.9f; b = 1.0f; }
                            else if (hue < 2.0f) { r = 1.0f; g = 0.8f; b = 0.9f; }
                            else { r = 0.9f; g = 1.0f; b = 0.8f; }
                            a = 0.12f * (1.0f - minEdge / 0.05f);
                        } else if (specPow > 0.3f) {
                            r = 1.0f; g = 0.98f; b = 0.95f;
                            a = 0.25f * (specPow - 0.3f) / 0.7f;
                        } else if (specPow > 0.05f) {
                            float hue = (float)((fHash * 3.0 + u + v + time * 0.3) % 3.0);
                            if (hue < 1.0f) { r = 0.6f; g = 0.8f; b = 1.0f; }
                            else if (hue < 2.0f) { r = 1.0f; g = 0.6f; b = 0.8f; }
                            else { r = 0.8f; g = 1.0f; b = 0.6f; }
                            a = 0.08f * specPow;
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_aura" -> {
                        // Soft, premium waving border aura glow
                        float distToEdge = Math.min(Math.min(u, 1.0f - u), Math.min(v, 1.0f - v));
                        float wave = (float) (Math.sin(u * 12.0f + time * 2.5f) * Math.cos(v * 12.0f - time * 1.8f) * 0.012f);
                        float d = distToEdge + wave;
                        
                        if (d < 0.08f) {
                            float pulse = (float) (Math.sin(time * 2.0f - distToEdge * 15.0f) * 0.2f + 0.8f);
                            float alphaVal = (float) Math.max(0.0, Math.min(1.0, (0.08f - d) / 0.08f)); // smoothstep manual
                            a = alphaVal * 0.45f * pulse;
                            
                            float colorShift = (float) ((time * 0.5f + distToEdge) % 3.0f);
                            if (colorShift < 1.0f) {
                                r = 0.0f * (1.0f - colorShift) + 0.6f * colorShift;
                                g = 0.8f * (1.0f - colorShift) + 0.2f * colorShift;
                                b = 1.0f;
                            } else if (colorShift < 2.0f) {
                                float t2 = colorShift - 1.0f;
                                r = 0.6f * (1.0f - t2) + 1.0f * t2;
                                g = 0.2f * (1.0f - t2) + 0.8f * t2;
                                b = 1.0f * (1.0f - t2) + 0.2f * t2;
                            } else {
                                float t2 = colorShift - 2.0f;
                                r = 1.0f * (1.0f - t2) + 0.0f * t2;
                                g = 0.8f * (1.0f - t2) + 0.8f * t2;
                                b = 0.2f * (1.0f - t2) + 1.0f * t2;
                            }
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_neon_pulse" -> {
                        // Grille néon cyberpunk pulsante avec nœuds brillants
                        float scrollU = u * 7.0f + time * 0.3f, scrollV = v * 7.0f - time * 0.5f;
                        float lineU = scrollU - (float)Math.floor(scrollU);
                        float lineV = scrollV - (float)Math.floor(scrollV);
                        float minLine = Math.min(Math.abs(lineU - 0.5f), Math.abs(lineV - 0.5f));
                        float nodeDistU = Math.min(lineU, 1.0f - lineU), nodeDistV = Math.min(lineV, 1.0f - lineV);
                        float nodeDist = (float)Math.sqrt(nodeDistU*nodeDistU + nodeDistV*nodeDistV);
                        float hueN = (u * 2.0f + time * 0.4f) % 3.0f;
                        float rN, gN, bN;
                        if (hueN < 1.0f) { rN = 0.0f; gN = 1.0f; bN = 0.8f + 0.2f*hueN; }
                        else if (hueN < 2.0f) { float t2 = hueN - 1.0f; rN = 0.8f*t2; gN = 0.2f; bN = 1.0f; }
                        else { float t2 = hueN - 2.0f; rN = 0.8f; gN = 0.2f + 0.8f*t2; bN = 1.0f - 0.2f*t2; }
                        float dxC = u - 0.5f, dyC = v - 0.5f;
                        float radialPulse = (float)(Math.sin((float)Math.sqrt(dxC*dxC + dyC*dyC) * 12.0 - time * 4.0) * 0.5 + 0.5);
                        r = rN; g = gN; b = bN;
                        if (nodeDist < 0.08f) {
                            float nodeHash = (float)(Math.abs(Math.sin((int)Math.floor(scrollU) * 13.7 + (int)Math.floor(scrollV) * 41.3)) % 1.0);
                            float nodeFlash = (float)(Math.sin(time * 6.0 + nodeHash * 20.0) * 0.5 + 0.5);
                            float nodeIntensity = (1.0f - nodeDist / 0.08f) * nodeFlash;
                            r = Math.min(1.0f, rN + 0.4f); g = Math.min(1.0f, gN + 0.4f); b = Math.min(1.0f, bN + 0.4f);
                            a = 0.8f * nodeIntensity;
                        } else if (minLine < 0.06f) {
                            a = 0.4f * (1.0f - minLine / 0.06f) * (0.5f + 0.5f * radialPulse);
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_constellation" -> {
                        // 8 étoiles connectées par de fines lignes d'énergie translucides
                        float minStarDist = 9999.0f;
                        float minLineDist = 9999.0f;
                        
                        float[] starXs = {0.2f, 0.35f, 0.45f, 0.7f, 0.8f, 0.6f, 0.3f, 0.15f};
                        float[] starYs = {0.25f, 0.45f, 0.3f, 0.35f, 0.6f, 0.75f, 0.7f, 0.55f};
                        
                        for (int s = 0; s < 8; s++) {
                            float sx = starXs[s];
                            float sy = starYs[s];
                            float dist = (float) Math.sqrt((u - sx)*(u - sx) + (v - sy)*(v - sy));
                            if (dist < minStarDist) minStarDist = dist;
                            
                            int next = (s + 1) % 8;
                            float nx = starXs[next];
                            float ny = starYs[next];
                            
                            float l2 = (nx - sx)*(nx - sx) + (ny - sy)*(ny - sy);
                            float t_proj = Math.max(0.0f, Math.min(1.0f, ((u - sx)*(nx - sx) + (v - sy)*(ny - sy)) / l2));
                            float projX = sx + t_proj * (nx - sx);
                            float projY = sy + t_proj * (ny - sy);
                            float distLine = (float) Math.sqrt((u - projX)*(u - projX) + (v - projY)*(v - projY));
                            if (distLine < minLineDist) minLineDist = distLine;
                        }
                        
                        float starPulse = (float) (Math.sin(time * 3.0 + u * 10.0 + v * 10.0) * 0.4 + 0.6);
                        
                        if (minStarDist < 0.02f) {
                            r = 0.8f; g = 0.95f; b = 1.0f;
                            a = 0.8f * starPulse;
                        } else if (minStarDist < 0.05f) {
                            float fade = 1.0f - (minStarDist - 0.02f) / 0.03f;
                            r = 0.4f; g = 0.8f; b = 1.0f;
                            a = 0.4f * fade * starPulse;
                        } else if (minLineDist < 0.012f) {
                            r = 0.3f; g = 0.7f; b = 1.0f;
                        a = 0.25f * (float) (Math.sin(time * 1.5 + (u + v) * 4.0) * 0.3 + 0.7);
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_cyber_dust" -> {
                        // Particules numériques carrées défilant sur toute la carte
                        float speed = 0.15f;
                        a = 0.0f;
                        
                        for (int p = 0; p < 12; p++) {
                            double seedX = Math.abs(Math.sin(p * 23.45)) * 0.9 + 0.05;
                            double seedY = Math.abs(Math.sin(p * 45.67)) * 0.9 + 0.05;
                            double seedSpd = 0.5 + Math.abs(Math.sin(p * 12.34)) * 0.5;
                            
                            float px = (float) seedX;
                            float py = (float) ((seedY - time * speed * seedSpd) % 1.0f);
                            if (py < 0.0f) py += 1.0f;
                            
                            if (Math.abs(u - px) < 0.025f && Math.abs(v - py) < 0.025f) {
                                float pTime = time * 4.0f + p * 2.0f;
                                float flash = (float) (Math.sin(pTime) * 0.5 + 0.5);
                                
                                float hue = (p * 0.3f + time * 0.5f) % 3.0f;
                                if (hue < 1.0f) { r = 0.1f; g = 0.9f; b = 1.0f; }
                                else if (hue < 2.0f) { r = 1.0f; g = 0.2f; b = 0.9f; }
                                else { r = 0.2f; g = 0.4f; b = 1.0f; }
                                
                                float edgeFade = (float) (Math.max(0.0, Math.min(1.0, py / 0.12f)) * Math.max(0.0, Math.min(1.0, (1.0f - py) / 0.12f))); // manual smoothstep bounds
                                a = 0.65f * flash * edgeFade;
                                break;
                            }
                        }
                    }
                    case "holo_magical_wind" -> {
                        // Vents magiques ondulants et fluides
                        float wave1 = (float) Math.sin(u * 4.0f - time * 2.0f) * 0.12f;
                        float wave2 = (float) Math.cos(u * 8.0f + time * 1.2f) * 0.05f;
                        
                        float ribbonY1 = v - 0.35f + wave1 + wave2;
                        float ribbonY2 = v - 0.65f - wave1 + wave2;
                        
                        float intensity1 = (float) Math.exp(-Math.pow(ribbonY1 * 12.0f, 2));
                        float intensity2 = (float) Math.exp(-Math.pow(ribbonY2 * 10.0f, 2));
                        float totalIntensity = intensity1 + intensity2;
                        
                        if (totalIntensity > 0.05f) {
                            float hue = (u * 1.5f + time * 0.6f) % 3.0f;
                            if (hue < 1.0f) {
                                r = 0.8f; g = 0.95f; b = 1.0f;
                            } else if (hue < 2.0f) {
                                r = 1.0f; g = 0.8f; b = 0.95f;
                            } else {
                                r = 0.9f; g = 0.8f; b = 1.0f;
                            }
                            a = 0.35f * totalIntensity * (float)(Math.sin(time * 2.0 + u * 3.0) * 0.3 + 0.7);
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_mega" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        
                        float pulse = (float)(Math.sin(time * 4.0f - radius * 10f) * 0.5f + 0.5f);
                        
                        // Thinner DNA strands (approx 1 pixel wide in 40x30 grid)
                        boolean isDna = Math.abs(dx - (float)Math.sin(dy * 12.0f + time * 2.0f) * 0.15f) < 0.025f
                                     || Math.abs(dx + (float)Math.sin(dy * 12.0f + time * 2.0f) * 0.15f) < 0.025f;
                                     
                        if (isDna && radius < 0.35f) {
                            // Protect the center where the Pokemon's face/body is
                            float centerFade = Math.max(0.0f, Math.min(1.0f, (radius - 0.10f) / 0.20f));
                            float dnaAlpha = 0.45f * centerFade;
                            if (dnaAlpha > 0.01f) {
                                r = 1.0f; g = 0.2f; b = 0.8f;
                                a = dnaAlpha;
                            } else {
                                a = 0.0f;
                            }
                        } else if (Math.abs(radius - 0.22f) < 0.025f) {
                            // Soft pulsing ring with center protection
                            float ringFade = Math.max(0.0f, Math.min(1.0f, (radius - 0.10f) / 0.15f));
                            r = 0.8f; g = 0.1f; b = 0.9f;
                            a = 0.35f * pulse * ringFade;
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_regional" -> {
                        float su = (u + v * 0.5f - time * 0.4f) % 0.4f;
                        float sv = (v - u * 0.2f - time * 0.1f) % 0.3f;
                        if (su < 0) su += 0.4f;
                        if (sv < 0) sv += 0.3f;
                        
                        boolean rune = (Math.abs(su - 0.2f) < 0.02f && Math.abs(sv - 0.15f) < 0.1f)
                                    || (Math.abs(sv - 0.15f) < 0.02f && Math.abs(su - 0.2f) < 0.08f);
                                    
                        if (rune) {
                            r = 0.1f; g = 0.9f; b = 1.0f;
                            a = 0.75f;
                        } else {
                            a = 0.0f;
                        }
                    }
                    case "holo_time_gears" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        float theta = (float) Math.atan2(dy, dx);
                        
                        float wave = (radius * 3.0f - time * 0.8f) - (float) Math.floor(radius * 3.0f - time * 0.8f);
                        float pulse = Math.max(0.0f, Math.min(1.0f, (wave - 0.9f) / 0.1f)) 
                                    * Math.max(0.0f, Math.min(1.0f, (1.0f - wave) / 0.1f)) 
                                    * Math.max(0.0f, 1.0f - radius * 1.5f);
                                    
                        float gearAngle1 = theta - time * 0.6f;
                        float gearTeeth1 = (float) Math.sin(gearAngle1 * 10.0f) * 0.02f;
                        float gear1 = Math.abs(radius - 0.28f - gearTeeth1);
                        
                        float gearAngle2 = theta + time * 0.4f + 1.5f;
                        float gearTeeth2 = (float) Math.sin(gearAngle2 * 8.0f) * 0.015f;
                        float gear2 = Math.abs(radius - 0.16f - gearTeeth2);
                        
                        float alphaVal = 0.0f;
                        if (gear1 < 0.018f) {
                            alphaVal = 0.55f * (1.0f - gear1 / 0.018f);
                            r = 0.95f; g = 0.75f; b = 0.2f;
                        } else if (gear2 < 0.015f) {
                            alphaVal = 0.45f * (1.0f - gear2 / 0.015f);
                            r = 0.95f; g = 0.75f; b = 0.2f;
                        } else if (pulse > 0.05f) {
                            alphaVal = pulse * 0.35f;
                            r = 0.1f; g = 0.85f; b = 1.0f;
                        }
                        
                        float centerFade = Math.max(0.0f, Math.min(1.0f, (radius - 0.08f) / 0.15f));
                        a = alphaVal * centerFade;
                    }
                    case "holo_spatial_crack" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        
                        float pulse = (float)(Math.sin(time * 3.5f) * 0.15f + 0.85f);
                        float crack1 = Math.abs((float)Math.sin(u * 14.0f + v * 9.0f + Math.sin(time * 0.5f) * 0.4f));
                        float crack2 = Math.abs((float)Math.cos(u * 8.0f - v * 16.0f - Math.cos(time * 0.6f) * 0.3f));
                        float minCrack = Math.min(crack1, crack2);
                        
                        float alphaVal = 0.0f;
                        if (minCrack < 0.022f && radius > 0.1f) {
                            float factor = 1.0f - minCrack / 0.022f;
                            alphaVal = 0.85f * factor * pulse;
                            r = 0.7f * (1.0f - factor) + 1.0f * factor;
                            g = 0.15f * (1.0f - factor) + 0.9f * factor;
                            b = 1.0f;
                        } else {
                            float dust = (float)(Math.sin(u * 20.0f + time * 1.0f) * Math.cos(v * 20.0f - time * 0.8f));
                            if (minCrack < 0.12f && dust > 0.68f && radius > 0.1f) {
                                alphaVal = 0.4f * (1.0f - minCrack / 0.12f) * ((dust - 0.68f) / 0.32f);
                                r = 0.9f; g = 0.05f; b = 0.6f;
                            }
                        }
                        
                        float centerFade = Math.max(0.0f, Math.min(1.0f, (radius - 0.08f) / 0.15f));
                        a = alphaVal * centerFade;
                    }
                    case "holo_prism_stars" -> {
                        float dx = u - 0.5f;
                        float dy = (v - 0.5f) * 1.3f;
                        float radius = (float) Math.sqrt(dx * dx + dy * dy);
                        
                        float alphaVal = 0.0f;
                        
                        float scaleX = 4.0f;
                        float scaleY = 4.0f;
                        float gridU = u * scaleX;
                        float gridV = v * scaleY + time * 0.15f;
                        
                        int cellU = (int) Math.floor(gridU);
                        int cellV = (int) Math.floor(gridV);
                        float lu = gridU - cellU;
                        float lv = gridV - cellV;
                        
                        double cellHash = Math.abs(Math.sin(cellU * 12.9898 + cellV * 78.233));
                        cellHash = cellHash - Math.floor(cellHash);
                        
                        if (cellHash > 0.7 && radius > 0.12f) {
                            float rot = time * 0.8f + (float) cellHash * 6.28f;
                            float cosR = (float) Math.cos(rot);
                            float sinR = (float) Math.sin(rot);
                            
                            float rx = (lu - 0.5f) * cosR + (lv - 0.5f) * sinR;
                            float ry = -(lu - 0.5f) * sinR + (lv - 0.5f) * cosR;
                            
                            float shard = Math.abs(rx) + ry * 0.5f;
                            float shine = (float)(Math.sin(time * 3.0f + cellHash * 20.0f) * 0.5f + 0.5f);
                            
                            if (shard < 0.22f) {
                                alphaVal = 0.65f * (1.0f - shard / 0.22f);
                                float hue = (float)((u + v + time * 0.6f + cellHash) % 3.0f);
                                if (hue < 0) hue += 3.0f;
                                if (hue < 1.0f) {
                                    r = 1.0f * (1.0f - hue) + 0.2f * hue;
                                    g = 0.2f * (1.0f - hue) + 0.8f * hue;
                                    b = 0.4f * (1.0f - hue) + 1.0f * hue;
                                } else if (hue < 2.0f) {
                                    float h2 = hue - 1.0f;
                                    r = 0.2f * (1.0f - h2) + 0.9f * h2;
                                    g = 0.8f * (1.0f - h2) + 0.9f * h2;
                                    b = 1.0f * (1.0f - h2) + 0.2f * h2;
                                } else {
                                    float h3 = hue - 2.0f;
                                    r = 0.9f * (1.0f - h3) + 1.0f * h3;
                                    g = 0.9f * (1.0f - h3) + 0.2f * h3;
                                    b = 0.2f * (1.0f - h3) + 0.4f * h3;
                                }
                                if (shine > 0.8f) {
                                    float sMix = (shine - 0.8f) / 0.2f;
                                    r = r * (1.0f - sMix) + 1.0f * sMix;
                                    g = g * (1.0f - sMix) + 1.0f * sMix;
                                    b = b * (1.0f - sMix) + 1.0f * sMix;
                                }
                            }
                        }
                        
                        float centerFade = Math.max(0.0f, Math.min(1.0f, (radius - 0.08f) / 0.15f));
                        a = alphaVal * centerFade;
                    }
                }
                
                // Écriture dans la NativeImage — tous les pixels, même ceux avec a=0 (transparents)
                // L'inversion Y est identique au background : row = gridY-1-iy
                int cr = (int)(Math.min(Math.max(r, 0), 1) * 255);
                int cg = (int)(Math.min(Math.max(g, 0), 1) * 255);
                int cb = (int)(Math.min(Math.max(b, 0), 1) * 255);
                int ca = (int)(Math.min(Math.max(a, 0), 1) * 255);
                fxPixels[(gridY - 1 - iy) * gridX + ix] = ProceduralTextureCache.toABGR(cr, cg, cb, ca);
            }
        }

        // Upload la DynamicTexture et rendu d'un seul quad semi-transparent
        // entityTranslucent = même render type que foil_stars, glint, etc. → compatible Iris
        net.minecraft.resources.ResourceLocation fxTex = ProceduralTextureCache.getEffectTexture(effectType, fxPixels, bucket);
        renderQuad(matrix, vertexConsumers.getBuffer(RenderType.entityTranslucent(fxTex)), light, overlay, w, h);
    }

    private static void renderHoloLayer(PoseStack matrices, MultiBufferSource vertexConsumers, String effect, int light, int overlay, float pokeWidth, float pokeHeight) {
        matrices.pushPose();
        
        // Offset pour éviter le Z-fighting
        float zPos = 0.983f;
        
        matrices.translate(5.0f / 16.0f, 10.0f / 16.0f, zPos / 16.0f);
        matrices.mulPose(Axis.YP.rotationDegrees(180));

        if (isProceduralEffect(effect)) {
            if (ModShaders.isAvailable()) {
                renderProceduralEffectShader(matrices, vertexConsumers, effect, light, overlay, pokeWidth, pokeHeight);
            } else {
                renderProceduralEffect(matrices, vertexConsumers, effect, light, overlay, pokeWidth, pokeHeight);
            }
        } else {
            switch (effect) {
                case "foil_stars" -> renderFoilStar(matrices, vertexConsumers, light, overlay, pokeWidth, pokeHeight);
                case "glint" -> renderDynamicGlint(matrices, vertexConsumers, light, overlay, pokeWidth, pokeHeight);
                case "flow" -> renderFlow(matrices, vertexConsumers, light, overlay, pokeWidth, pokeHeight);
                case "noise" -> renderPlasma(matrices, vertexConsumers, light, overlay, pokeWidth, pokeHeight);
            }
        }

        matrices.popPose();
    }

    private static void renderFoilStar(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, float w, float h) {
        float time = (System.currentTimeMillis() % 10000) / 10000f;
        float alpha = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 500.0);

        float uOffset = time;
        float vOffset = time;

        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(TEXTURE_STARS));
        // UV crop: foil_stars was 40x30, now padded to 48x32. Scale u/v range by crop factor.
        renderQuad(matrices.last().pose(), consumer, light, overlay, w, h,
            uOffset * UV_40x30_U, vOffset * UV_40x30_V,
            UV_40x30_U * (1f + uOffset), UV_40x30_V * (1f + vOffset),
            255, 255, 255, (int)(alpha * 255));
    }

    private static void renderDynamicGlint(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, float w, float h) {
        // --- CONFIGURATION DE L'ANIMATION ---
        int totalFrames = 80;      // Nombre de frames réelles dans le PNG
        int frameDurationMs = 50;  // Vitesse : 50ms par frame (soit 20 FPS, comme Minecraft par défaut)
        
        // Calcul de la frame actuelle en fonction du temps système
        int currentFrame = (int) ((System.currentTimeMillis() / frameDurationMs) % totalFrames);
        
        // Calcul des coordonnées UV (V représente l'axe vertical)
        float v0 = (float) currentFrame / totalFrames;
        float v1 = (float) (currentFrame + 1) / totalFrames;

        // On utilise entityTranslucent car energySwirl est fait pour du défilement continu, pas des frames
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucent(TEXTURE_GLINT));

        // On dessine le quad en passant les nouvelles coordonnées V calculées
        // UV crop: glint was 40x2400, padded to 48x2400. Height (2400) is already div by 16, only U changes.
        renderQuad(matrices.last().pose(), consumer, light, overlay, w, h, 0, v0, UV_40x30_U, v1, 255, 255, 255, (int)(0.5f * 255));
    }

    private static void renderFlow(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, float w, float h) {
        float time = (System.currentTimeMillis() % 5000) / 5000f;
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE_FLOW));
        // UV crop: flow was 40x30, padded to 48x32. Scale scrolling UV range accordingly.
        renderQuad(matrices.last().pose(), consumer, light, overlay, w, h,
            0, time * UV_40x30_V, UV_40x30_U, UV_40x30_V + time * UV_40x30_V, 255, 255, 255, 128);
    }

    private static void renderPlasma(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int overlay, float w, float h) {
        float time = (System.currentTimeMillis() % 10000) / 1000.0f;
        float hw = w / 2.0f, hh = h / 2.0f;
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer consumer = vertexConsumers.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE_NOISE));

        // Vertex 1: Bottom Left (-hw, -hh) -> UV(0, 0)
        addPlasmaVertex(matrix, consumer, -hw, -hh, 0, 1, time, light, overlay);
        // Vertex 2: Bottom Right (hw, -hh) -> UV(1, 0)
        addPlasmaVertex(matrix, consumer,  hw, -hh, 1, 1, time, light, overlay);
        // Vertex 3: Top Right (hw, hh) -> UV(1, 1)
        addPlasmaVertex(matrix, consumer,  hw,  hh, 1, 0, time, light, overlay);
        // Vertex 4: Top Left (-hw, hh) -> UV(0, 1)
        addPlasmaVertex(matrix, consumer, -hw,  hh, 0, 0, time, light, overlay);
    }

    private static void addPlasmaVertex(Matrix4f matrix, VertexConsumer consumer, float x, float y, float u, float v, float time, int light, int overlay) {
        // Plasma formula
        double val = Math.sin(u * 10.0 + time) + Math.sin((v * 10.0 + time) / 2.0) + Math.sin((u * 10.0 + v * 10.0 + time) / 2.0);

        float r = (float)(Math.sin(val * Math.PI) * 0.5 + 0.5);
        float g = (float)(Math.sin(val * Math.PI + 2.0 * Math.PI / 3.0) * 0.5 + 0.5);
        float b = (float)(Math.sin(val * Math.PI + 4.0 * Math.PI / 3.0) * 0.5 + 0.5);
        
        consumer.addVertex(matrix, x, y, 0)
                .setColor((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(0.5f * 255))
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(0, 0, 1);
    }

    private static Item getFrameItem(CardData data) {
        if (data.isShiny()) return ModItems.FRAME_SHINY;

        return switch (data.rarity().toLowerCase()) {
            case "uncommon" -> ModItems.FRAME_UNCOMMON;
            case "rare" -> ModItems.FRAME_RARE;
            case "epic" -> ModItems.FRAME_EPIC;
            case "legendary" -> ModItems.FRAME_LEGENDARY;
            case "shiny" -> ModItems.FRAME_SHINY;
            default -> ModItems.FRAME_COMMON;
        };
    }

    private static void renderQuad(Matrix4f matrix, VertexConsumer consumer, int light, int overlay, float w, float h) {
        renderQuad(matrix, consumer, light, overlay, w, h, 0, 0, 1, 1, 255, 255, 255, 255);
    }

    private static void renderQuad(Matrix4f matrix, VertexConsumer consumer, int light, int overlay, float w, float h, float u0, float v0, float u1, float v1, int r, int g, int b, int a) {
        float hw = w / 2.0f, hh = h / 2.0f;
        consumer.addVertex(matrix, -hw, -hh, 0).setColor(r, g, b, a).setUv(u0, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix,  hw, -hh, 0).setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix,  hw,  hh, 0).setColor(r, g, b, a).setUv(u1, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -hw,  hh, 0).setColor(r, g, b, a).setUv(u0, v0).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
    }

    private static ResourceLocation resolvePokemonTexture(String resolvedPokemonId, boolean isShiny) {
        net.minecraft.server.packs.resources.ResourceManager rm = Minecraft.getInstance().getResourceManager();
        if (rm != cachedResourceManager) {
            POKEMON_TEXTURE_CACHE.clear();
            cachedResourceManager = rm;
        }
        String cacheKey = resolvedPokemonId.toLowerCase() + (isShiny ? "|1" : "|0");
        return POKEMON_TEXTURE_CACHE.computeIfAbsent(cacheKey, k -> lookupPokemonTexture(rm, resolvedPokemonId, isShiny));
    }

    private static ResourceLocation lookupPokemonTexture(net.minecraft.server.packs.resources.ResourceManager rm, String resolvedPokemonId, boolean isShiny) {
        if (resolvedPokemonId.equalsIgnoreCase("missingno")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/easter_egg/missingno.png");
        } else if (resolvedPokemonId.equalsIgnoreCase("you_and_mew")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/easter_egg/you_and_mew.png");
        } else if (resolvedPokemonId.equalsIgnoreCase("ghost")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/easter_egg/ghost.png");
        } else if (resolvedPokemonId.equalsIgnoreCase("god_bidoof")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/easter_egg/god_bidoof.png");
        } else if (resolvedPokemonId.equalsIgnoreCase("crystal_onix")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/easter_egg/crystal_onix.png");
        } else if (resolvedPokemonId.equalsIgnoreCase("shadow_lugia")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/easter_egg/shadow_lugia.png");
        } else if (resolvedPokemonId.equalsIgnoreCase("pride_sylveon")) {
            return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/easter_egg/pride_sylveon.png");
        }

        String folder = isShiny ? "shiny" : "regular";
        String sanitizedName = sanitizeStandardPath(resolvedPokemonId);
        ResourceLocation primaryTex = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/" + folder + "/" + sanitizedName + ".png");

        if (rm != null && rm.getResource(primaryTex).isPresent()) {
            return primaryTex;
        }

        String dashed = sanitizedName.replace("_", "-");
        ResourceLocation dashedTex = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/" + folder + "/" + dashed + ".png");
        if (rm != null && rm.getResource(dashedTex).isPresent()) {
            return dashedTex;
        }

        if (rm != null) {
            String baseSpecies = getBaseSpeciesName(resolvedPokemonId);
            Species species = com.howlite.cobblemoncards.util.CardUtil.getSpecies(baseSpecies);
            if (species != null) {
                String cleanName = sanitizeEntityIconPath(species.getName());
                String dexString = String.format("%04d", species.getNationalPokedexNumber());
                String entityFolderName = dexString + "_" + cleanName;

                // A. Essayer d'abord la texture exacte de la variante (ex: vulpix_alolan.png, unown_a.png)
                String vName = resolvedPokemonId.toLowerCase();
                String[] variantFiles = {
                    vName + (isShiny ? "_shiny" : ""),
                    vName.replace("-", "_") + (isShiny ? "_shiny" : ""),
                    vName.replace("_", "-") + (isShiny ? "_shiny" : "")
                };
                for (String vf : variantFiles) {
                    String cleanVf = sanitizeStandardPath(vf) + ".png";
                    ResourceLocation vTex = ResourceLocation.fromNamespaceAndPath(
                        "cobblemon-cards",
                        "textures/item/cards/pokemon/entity_icon/" + entityFolderName + "/" + cleanVf
                    );
                    if (rm.getResource(vTex).isPresent()) {
                        return vTex;
                    }
                }

                // B. Essayer le fichier standard avec le nom remappé
                String remappedName = getRemappedEntityName(cleanName);
                String standardFileName = remappedName + (isShiny ? "_shiny.png" : ".png");
                ResourceLocation standardTex = ResourceLocation.fromNamespaceAndPath(
                    "cobblemon-cards",
                    "textures/item/cards/pokemon/entity_icon/" + entityFolderName + "/" + standardFileName
                );
                if (rm.getResource(standardTex).isPresent()) {
                    return standardTex;
                }

                // C. Essayer avec les suffixes courants (genres, formes, segments, ou numéros de sprite)
                String[] suffixes = {
                    "_1", "_male", "_a", "_m", "_female", "_f", "_left", "_right", "_chest", "_roaming", "_zero", "_hero",
                    "_solo", "_school", "_amped", "_lowkey", "_curly", "_droopy", "_stretchy",
                    "_two_segment", "_three_segment", "_green", "_blue", "_yellow", "_gray",
                    "_spring", "_summer", "_autumn", "_winter", "_west", "_east",
                    "_redstripe", "_bluestripe", "_whitestripe", "_shadow",
                    "_incarnate", "_therian", "_vanilla"
                };
                for (String suffix : suffixes) {
                    if (isShiny) {
                        ResourceLocation t1 = ResourceLocation.fromNamespaceAndPath(
                            "cobblemon-cards",
                            "textures/item/cards/pokemon/entity_icon/" + entityFolderName + "/" + remappedName + suffix + "_shiny.png"
                        );
                        if (rm.getResource(t1).isPresent()) return t1;

                        ResourceLocation t2 = ResourceLocation.fromNamespaceAndPath(
                            "cobblemon-cards",
                            "textures/item/cards/pokemon/entity_icon/" + entityFolderName + "/" + remappedName + "_shiny" + suffix + ".png"
                        );
                        if (rm.getResource(t2).isPresent()) return t2;
                    } else {
                        ResourceLocation t = ResourceLocation.fromNamespaceAndPath(
                            "cobblemon-cards",
                            "textures/item/cards/pokemon/entity_icon/" + entityFolderName + "/" + remappedName + suffix + ".png"
                        );
                        if (rm.getResource(t).isPresent()) return t;
                    }
                }
            }
        }

        // Fallback sur substitute.png ou substitute_shiny.png (40x30)
        String fallbackName = isShiny ? "substitute_shiny.png" : "substitute.png";
        return ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/item/cards/pokemon/entity_icon/" + fallbackName);
    }

    private static ResourceLocation resolvePlayerSkin(UUID uuid, String name) {
        Minecraft mc = Minecraft.getInstance();

        // 1. Online player on the current server/world (already has signed skin textures from tablist)
        ClientPacketListener connection = mc.getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(uuid);
            if (info == null && name != null && !name.isEmpty()) {
                info = connection.getPlayerInfo(name);
            }
            if (info != null) {
                return info.getSkin().texture();
            }
        }

        // 2. Local player fallback
        if (mc.player != null && uuid.equals(mc.player.getUUID())) {
            return mc.player.getSkin().texture();
        }

        // 3. Cached profile fetched from Mojang session service (for offline players)
        String cacheKey = uuid.toString();
        GameProfile cached = PLAYER_PROFILE_CACHE.get(cacheKey);
        if (cached != null) {
            return mc.getSkinManager().getInsecureSkin(cached).texture();
        }

        // 4. Kick off a one-time async profile fetch for real v4 UUIDs
        if (uuid.version() == 4 && PLAYER_PROFILE_PENDING.add(cacheKey)) {
            Util.backgroundExecutor().execute(() -> {
                try {
                    ProfileResult result = mc.getMinecraftSessionService().fetchProfile(uuid, false);
                    if (result != null && result.profile() != null) {
                        PLAYER_PROFILE_CACHE.put(cacheKey, result.profile());
                    }
                } catch (Exception ignored) {
                    // Keep default skin if offline/rate-limited
                }
            });
        }

        return mc.getSkinManager().getInsecureSkin(new GameProfile(uuid, name)).texture();
    }

    private static String getBaseSpeciesName(String name) {
        String lower = name.toLowerCase();
        if (lower.startsWith("unown_") || lower.startsWith("unown-")) return "unown";
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
        
        if (lower.endsWith("-alolan")) return lower.substring(0, lower.length() - "-alolan".length());
        if (lower.endsWith("-galarian")) return lower.substring(0, lower.length() - "-galarian".length());
        if (lower.endsWith("-hisuian")) return lower.substring(0, lower.length() - "-hisuian".length());
        if (lower.endsWith("-paldean-combat")) return "tauros";
        if (lower.endsWith("-paldean-blaze")) return "tauros";
        if (lower.endsWith("-paldean-aqua")) return "tauros";
        if (lower.endsWith("-paldean")) return lower.substring(0, lower.length() - "-paldean".length());
        if (lower.endsWith("-mega-x")) return lower.substring(0, lower.length() - "-mega-x".length());
        if (lower.endsWith("-mega-y")) return lower.substring(0, lower.length() - "-mega-y".length());
        if (lower.endsWith("-mega")) return lower.substring(0, lower.length() - "-mega".length());
        if (lower.endsWith("-gmax")) return lower.substring(0, lower.length() - "-gmax".length());
        if (lower.endsWith("-gigantamax")) return lower.substring(0, lower.length() - "-gigantamax".length());
        return lower;
    }

    private static String sanitizeStandardPath(String id) {
        if (id == null) return "missing";
        return id.toLowerCase()
                 .replace(".", "")
                 .replace(" ", "-")
                 .replaceAll("[^a-z0-9/._-]", "");
    }

    private static String sanitizeEntityIconPath(String id) {
        if (id == null) return "missing";
        return id.toLowerCase()
                 .replace(".", "")
                 .replace(" ", "")
                 .replace("-", "")
                 .replaceAll("[^a-z0-9/._-]", "");
    }

    private static String getRemappedEntityName(String cleanName) {
        return switch (cleanName) {
            case "mrmime" -> "mr_mime";
            case "mimejr" -> "mime_jr";
            case "mrrime" -> "mr_rime";
            case "hooh" -> "ho_oh";
            case "porygonz" -> "porygon-z";
            case "jangmoo" -> "jangmo-o";
            case "hakamoo" -> "hakamo-o";
            case "kommoo" -> "kommo-o";
            default -> cleanName;
        };
    }

    private static void renderProceduralBackgroundShader(PoseStack matrices, MultiBufferSource vertexConsumers, String bgType, int light, int overlay, float w, float h) {
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer consumer = vertexConsumers.getBuffer(ModShaders.PROCEDURAL_BG);
        int effectId = getBackgroundEffectId(bgType);
        
        int timeMs = (int) (System.currentTimeMillis() % 65536);
        int g = (timeMs >> 8) & 0xFF;
        int b = timeMs & 0xFF;
        
        float hw = w / 2.0f;
        float hh = h / 2.0f;
        
        consumer.addVertex(matrix, -hw, -hh, 0).setColor(effectId, g, b, 255).setUv(0.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix,  hw, -hh, 0).setColor(effectId, g, b, 255).setUv(1.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix,  hw,  hh, 0).setColor(effectId, g, b, 255).setUv(1.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -hw,  hh, 0).setColor(effectId, g, b, 255).setUv(0.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
    }

    private static void renderProceduralEffectShader(PoseStack matrices, MultiBufferSource vertexConsumers, String effectType, int light, int overlay, float w, float h) {
        Matrix4f matrix = matrices.last().pose();
        VertexConsumer consumer = vertexConsumers.getBuffer(ModShaders.PROCEDURAL_HOLO);
        int effectId = getHoloEffectId(effectType);
        
        int timeMs = (int) (System.currentTimeMillis() % 65536);
        int g = (timeMs >> 8) & 0xFF;
        int b = timeMs & 0xFF;
        
        float hw = w / 2.0f;
        float hh = h / 2.0f;
        
        consumer.addVertex(matrix, -hw, -hh, 0).setColor(effectId, g, b, 255).setUv(0.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix,  hw, -hh, 0).setColor(effectId, g, b, 255).setUv(1.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix,  hw,  hh, 0).setColor(effectId, g, b, 255).setUv(1.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
        consumer.addVertex(matrix, -hw,  hh, 0).setColor(effectId, g, b, 255).setUv(0.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(0, 0, 1);
    }

    private static int getBackgroundEffectId(String bgType) {
        return switch (bgType) {
            case "water_anim" -> 0;
            case "lava_anim" -> 1;
            case "balatro_swirl" -> 2;
            case "geometric_pulse" -> 3;
            case "plasma_bg" -> 4;
            case "starfield_anim" -> 5;
            case "cloud_scroll" -> 6;
            case "neon_grid" -> 7;
            case "toxic_sludge" -> 8;
            case "matrix_code" -> 9;
            case "fire_embers" -> 10;
            case "crystal_cave" -> 11;
            case "sandstorm" -> 12;
            case "aurora_borealis" -> 13;
            case "deep_ocean" -> 14;
            case "void_rift" -> 15;
            case "golden_sunset" -> 16;
            case "cherry_blossom_wind" -> 17;
            case "cyber_city" -> 18;
            case "ancient_ruins" -> 19;
            case "frozen_tundra" -> 20;
            case "rainbow_highway" -> 21;
            case "plasma_storm" -> 22;
            case "galactic_supernova" -> 23;
            case "water2" -> 24;
            case "mega_energy" -> 25;
            case "alola_beach" -> 26;
            case "hisui_ancient" -> 27;
            case "galar_industrial" -> 28;
            case "paldea_crystal" -> 29;
            case "distortion_rift" -> 30;
            case "dreamscape" -> 31;
            case "magma_chamber" -> 32;
            case "stained_glass" -> 33;
            case "fluid_marble" -> 34;
            case "fossilized_amber" -> 35;
            default -> 0;
        };
    }

    private static int getHoloEffectId(String effectType) {
        return switch (effectType) {
            case "holo_lines" -> 0;
            case "holo_pulse" -> 1;
            case "holo_rainbow" -> 2;
            case "holo_sparkle" -> 3;
            case "holo_runes" -> 4;
            case "holo_circuit" -> 5;
            case "holo_bubbles" -> 6;
            case "holo_shatter" -> 7;
            case "holo_ripple" -> 8;
            case "holo_scanline" -> 9;
            case "holo_prism" -> 10;
            case "holo_aurora" -> 11;
            case "holo_vortex" -> 12;
            case "holo_lightning" -> 13;
            case "holo_galaxy" -> 14;
            case "holo_sakura" -> 15;
            case "holo_plasma_arc" -> 16;
            case "holo_diamond" -> 17;
            case "holo_aura" -> 18;
            case "holo_neon_pulse" -> 19;
            case "holo_constellation" -> 20;
            case "holo_cyber_dust" -> 21;
            case "holo_magical_wind" -> 22;
            case "holo_mega" -> 23;
            case "holo_regional" -> 24;
            case "holo_time_gears" -> 25;
            case "holo_spatial_crack" -> 26;
            case "holo_prism_stars" -> 27;
            default -> 0;
        };
    }
}