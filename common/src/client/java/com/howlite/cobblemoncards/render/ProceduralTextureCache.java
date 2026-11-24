package com.howlite.cobblemoncards.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Gère les DynamicTextures pour les backgrounds et effets holographiques procéduraux.
 *
 * PROBLÈME RÉSOLU :
 * L'approche précédente (1200 vertex-quads avec vertex colors) était incompatible avec
 * Iris/Complementary : Iris remplace le pipeline shader vanilla, les vertex colors sont
 * écrasées par le PBR (lighting directionnel, ombres, metallic/roughness).
 *
 * SOLUTION NativeImage :
 * On génère les couleurs dans un tableau Java (même math qu'avant), on upload ce tableau
 * dans une DynamicTexture 40×30, puis on rend UN SEUL quad avec entityCutout/entityTranslucent.
 * Iris traite cette texture exactement comme n'importe quelle autre texture d'entité →
 * les couleurs restent correctes.
 *
 * AVANTAGES :
 * - Couleurs 100% correctes avec tous les shader packs (Complementary, BSL, SEUS...)
 * - 1 quad au lieu de 1200 → vertex count divisé par 1200
 * - Fonctionne en GUI, dans la main, dans le HoloProjector
 * - Pas de dépendance sur l'état d'Iris (plus besoin de détecter isIrisShadersActive())
 */
public class ProceduralTextureCache {

    /** Résolution de la grille procédurale (doit correspondre à CardItemRenderer) */
    public static final int WIDTH  = 40;
    public static final int HEIGHT = 30;

    /** Intervalle minimum (ms) entre deux recalculs CPU d'un même effet (~30 FPS). */
    private static final long UPDATE_INTERVAL_MS = 33L;

    private static final class Entry {
        final DynamicTexture texture;
        final ResourceLocation location;
        long lastUpdateBucket = -1L;

        Entry(DynamicTexture texture, ResourceLocation location) {
            this.texture = texture;
            this.location = location;
        }
    }

    private static final Map<String, Entry> bgCache     = new HashMap<>();
    private static final Map<String, Entry> effectCache = new HashMap<>();

    public static long currentBucket() {
        return System.currentTimeMillis() / UPDATE_INTERVAL_MS;
    }

    /**
     * Retourne la ResourceLocation déjà calculée pour ce bucket temporel si elle est encore fraîche,
     * sinon {@code null} (l'appelant doit alors recalculer les pixels et appeler {@link #getBgTexture}).
     */
    public static ResourceLocation getFreshBgTexture(String name, long bucket) {
        Entry entry = bgCache.get(name);
        return (entry != null && entry.lastUpdateBucket == bucket) ? entry.location : null;
    }

    /**
     * Retourne la ResourceLocation déjà calculée pour ce bucket temporel si elle est encore fraîche,
     * sinon {@code null} (l'appelant doit alors recalculer les pixels et appeler {@link #getEffectTexture}).
     */
    public static ResourceLocation getFreshEffectTexture(String name, long bucket) {
        Entry entry = effectCache.get(name);
        return (entry != null && entry.lastUpdateBucket == bucket) ? entry.location : null;
    }

    /**
     * Retourne la ResourceLocation d'une DynamicTexture pour un background procédural.
     * La texture est mise à jour avec les couleurs calculées puis uploadée au GPU.
     *
     * @param name        identifiant de l'effet (ex: "water_anim", "aurora_borealis")
     * @param pixelColors tableau au format ABGR (NativeImage.setPixelRGBA),
     *                    indexé par (row * WIDTH + col), row=0 en haut de l'image
     */
    public static ResourceLocation getBgTexture(String name, int[] pixelColors) {
        return getOrCreate(bgCache, "cobblemon_bg_", name, pixelColors, currentBucket());
    }

    public static ResourceLocation getBgTexture(String name, int[] pixelColors, long bucket) {
        return getOrCreate(bgCache, "cobblemon_bg_", name, pixelColors, bucket);
    }

    /**
     * Retourne la ResourceLocation d'une DynamicTexture pour un effet holographique.
     * Les pixels à alpha=0 restent transparents → laissent voir le Pokémon en dessous.
     */
    public static ResourceLocation getEffectTexture(String name, int[] pixelColors) {
        return getOrCreate(effectCache, "cobblemon_fx_", name, pixelColors, currentBucket());
    }

    public static ResourceLocation getEffectTexture(String name, int[] pixelColors, long bucket) {
        return getOrCreate(effectCache, "cobblemon_fx_", name, pixelColors, bucket);
    }

    private static ResourceLocation getOrCreate(Map<String, Entry> cache,
                                                 String prefix, String name,
                                                 int[] pixels, long bucket) {
        Entry entry = cache.get(name);
        if (entry == null) {
            // Première utilisation : créer la NativeImage + DynamicTexture + enregistrer
            NativeImage img = new NativeImage(NativeImage.Format.RGBA, WIDTH, HEIGHT, false);
            DynamicTexture tex = new DynamicTexture(img);

            // Sanitize le nom pour le ResourceLocation (doit être en minuscules, pas de caractères spéciaux)
            String safeName = name.replace('_', '-').replaceAll("[^a-z0-9/._-]", "");
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(
                    "cobblemon-cards", "dynamic/" + prefix + safeName);

            Minecraft.getInstance().getTextureManager().register(loc, tex);
            entry = new Entry(tex, loc);
            cache.put(name, entry);
        }

        // Mettre à jour les pixels et uploader au GPU uniquement si le bucket a avancé
        if (entry.lastUpdateBucket != bucket) {
            NativeImage img = entry.texture.getPixels();
            if (img != null) {
                for (int i = 0; i < pixels.length && i < WIDTH * HEIGHT; i++) {
                    img.setPixelRGBA(i % WIDTH, i / WIDTH, pixels[i]);
                }
                entry.texture.upload();
            }
            entry.lastUpdateBucket = bucket;
        }

        return entry.location;
    }

    /**
     * Encode une couleur (r, g, b, a chacun 0–255) au format ABGR attendu par NativeImage.setPixelRGBA.
     *
     * NativeImage utilise le format OpenGL ABGR little-endian :
     *   byte 0 (LSB) = Rouge, byte 1 = Vert, byte 2 = Bleu, byte 3 (MSB) = Alpha
     */
    public static int toABGR(int r, int g, int b, int a) {
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    /**
     * Vide le cache (libère les références aux DynamicTextures).
     * Le TextureManager de Minecraft gère la destruction des ressources GPU.
     * À appeler lors du rechargement des ressources ou de l'arrêt du client.
     */
    public static void clear() {
        bgCache.clear();
        effectCache.clear();
    }
}
