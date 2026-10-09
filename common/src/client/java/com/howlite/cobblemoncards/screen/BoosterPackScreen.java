package com.howlite.cobblemoncards.screen;

import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.network.CloseBoosterPayload;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.howlite.cobblemoncards.util.PlatformHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.howlite.cobblemoncards.item.ModItems;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoosterPackScreen extends Screen {

    private List<ItemStack> rewards = List.of();
    private boolean[] clicked = new boolean[5]; // Si le joueur a cliqué
    private float[] shakeTicks = new float[5]; // Temps de tremblement restant (en ticks)
    private float[] flipProgress = new float[5]; // De 0.0 (dos) à 1.0 (face)
    private float[] hoverAlpha = new float[5]; // Opacité du texte (0.0 à 1.0)
    private boolean[] introSoundPlayed = new boolean[5]; // Si le son d'intro a été joué
    private Button[] cardButtons = new Button[5];
    private float ticks = 0;

    private float cameraShakeTicks = 0f;
    private long lastTime = 0L;

    private final List<ScreenParticle> screenParticles = new ArrayList<>();
    private final Random random = new Random();

    public BoosterPackScreen() {
        super(Component.translatable("screen.cobblemon-cards.booster_pack.title"));
    }

    public void setRewards(List<ItemStack> rewards) {
        this.rewards = rewards;
    }

    @Override
    protected void init() {
        super.init();

        int cardWidth = 65;
        int cardHeight = 95;
        int spacing = 25;

        int totalWidth = (cardWidth * 5) + (spacing * 4);
        int startX = (this.width / 2) - (totalWidth / 2);
        int y = (this.height / 2) - (cardHeight / 2);

        for (int i = 0; i < 5; i++) {
            final int index = i;
            int x = startX + (i * (cardWidth + spacing));

            this.cardButtons[i] = Button.builder(Component.empty(), button -> {
                if (!clicked[index]) {
                    this.clicked[index] = true;
                    this.shakeTicks[index] = getShakeDuration(index); // Tremblement de durée dynamique selon rareté
                    button.active = false;
                    playRevealSound(index);
                }
            }).bounds(x, y, cardWidth, cardHeight).build();

            this.addRenderableWidget(this.cardButtons[i]);
        }
    }

    @Override
    public void removed() {
        PlatformHelper.INSTANCE.sendToServer(new CloseBoosterPayload());
        super.removed();
    }

    private void playRevealSound(int index) {
        if (rewards.size() <= index) return;

        ItemStack stack = rewards.get(index);
        CardData data = stack.get(ModDataComponents.CARD_DATA);

        if (data == null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
            return;
        }

        String rarity = data.rarity().toLowerCase();
        boolean isShiny = data.isShiny();

        if (isShiny || rarity.equals("legendary") || rarity.equals("mythic")) {
            // Son de suspense majestueux
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BEACON_ACTIVATE, 1.5f));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 0.5f));
        } else if (rarity.equals("epic")) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 0.7f));
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 0.9f));
        } else if (rarity.equals("rare")) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_CHIME, 1.0f));
        } else {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.2f));
        }
    }

    private int getShakeDuration(int index) {
        if (rewards.size() <= index) return 12;
        ItemStack stack = rewards.get(index);
        CardData data = stack.get(ModDataComponents.CARD_DATA);
        if (data == null) return 12;
        String rarity = data.rarity().toLowerCase();
        boolean isShiny = data.isShiny();

        if (isShiny || rarity.equals("legendary") || rarity.equals("mythic")) {
            return 32; // Tremblement prolongé pour le suspense légendaire !
        } else if (rarity.equals("epic")) {
            return 20; // Tremblement moyen
        } else if (rarity.equals("rare")) {
            return 14; // Tremblement court
        }
        return 8; // Très rapide pour les communes
    }

    private float getFlipSpeed(int index) {
        if (rewards.size() <= index) return 0.08f;
        ItemStack stack = rewards.get(index);
        CardData data = stack.get(ModDataComponents.CARD_DATA);
        if (data == null) return 0.08f;
        String rarity = data.rarity().toLowerCase();
        boolean isShiny = data.isShiny();

        if (isShiny || rarity.equals("legendary") || rarity.equals("mythic")) {
            return 0.04f; // Retournement majestueux lent
        } else if (rarity.equals("epic")) {
            return 0.06f; // Vitesse épique
        } else if (rarity.equals("rare")) {
            return 0.08f; // Vitesse rare
        }
        return 0.12f; // Retournement éclair pour communes
    }

    private void spawnShakingParticles(int index, float xCenter, float yCenter) {
        if (rewards.size() <= index) return;
        ItemStack stack = rewards.get(index);
        CardData data = stack.get(ModDataComponents.CARD_DATA);
        if (data == null) return;

        String rarity = data.rarity().toLowerCase();
        boolean isShiny = data.isShiny();
        
        int color = 0xFFFFFF;
        boolean shinyOrLegendary = isShiny || rarity.equals("legendary") || rarity.equals("mythic");
        
        if (shinyOrLegendary) {
            color = isShiny ? 0xFFFF55 : 0xFFAA00;
        } else if (rarity.equals("epic")) {
            color = 0xFF55FF;
        } else if (rarity.equals("rare")) {
            color = 0x5555FF;
        } else {
            return; // Pas d'étincelles préalables pour les communes
        }

        // Fait jaillir des étincelles depuis les bords de la carte
        float angle = random.nextFloat() * 2.0f * (float) Math.PI;
        float dist = 20.0f + random.nextFloat() * 15.0f;
        float px = xCenter + (float) Math.cos(angle) * dist;
        float py = yCenter + (float) Math.sin(angle) * dist;
        float dx = (xCenter - px) * 0.05f + (random.nextFloat() - 0.5f) * 1.5f;
        float dy = (yCenter - py) * 0.05f + (random.nextFloat() - 0.5f) * 1.5f;
        
        screenParticles.add(new ScreenParticle(px, py, dx, dy, color, 1.5f + random.nextFloat() * 2.0f, 10 + random.nextInt(10), true));
    }

    private void renderCardGlowAndRays(GuiGraphics graphics, int color) {
        float time = this.ticks * 0.8f;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        // 1. Rayons de soleil rotatifs
        int rayCount = 8;
        float maxRayLen = 65.0f + (float) Math.sin(this.ticks * 0.1f) * 10.0f;
        int rayAlpha = 45; // Rayons translucides subtils
        int rayColor = (rayAlpha << 24) | (r << 16) | (g << 8) | b;
        
        graphics.pose().pushPose();
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(time));
        for (int i = 0; i < rayCount; i++) {
            graphics.pose().pushPose();
            graphics.pose().mulPose(Axis.ZP.rotationDegrees(i * (360.0f / rayCount)));
            // Dessiner un diamant/rectangle fin rayonnant vers l'extérieur
            graphics.fill(-6, (int)-maxRayLen, 6, (int)maxRayLen, rayColor);
            graphics.pose().popPose();
        }
        graphics.pose().popPose();
        
        // 2. Halo de fond doux pulsant
        int haloAlpha = 30 + (int)(15.0f * Math.sin(this.ticks * 0.15f));
        int haloColor = (haloAlpha << 24) | (r << 16) | (g << 8) | b;
        
        graphics.pose().pushPose();
        graphics.pose().mulPose(Axis.ZP.rotationDegrees(-time * 0.5f));
        float haloSize = 42.0f + (float) Math.sin(this.ticks * 0.05f) * 4.0f;
        graphics.fill((int)-haloSize, (int)-haloSize, (int)haloSize, (int)haloSize, haloColor);
        graphics.pose().popPose();
    }

    private float smoothStep(float x) {
        return x * x * (3.0f - 2.0f * x);
    }

    private void triggerDopamineEffects(int index, float xCenter, float yCenter) {
        if (rewards.size() <= index) return;

        ItemStack stack = rewards.get(index);
        CardData data = stack.get(ModDataComponents.CARD_DATA);
        Player player = Minecraft.getInstance().player;

        int particleCount = 25;
        int particleColor = 0xFFFFFF; // Blanc par défaut

        if (data != null) {
            String rarity = data.rarity().toLowerCase();
            boolean isShiny = data.isShiny();

            if (isShiny || rarity.equals("legendary") || rarity.equals("mythic")) {
                particleCount = 75;
                particleColor = isShiny ? 0xFFFF55 : 0xFFAA00; // Jaune d'or pour shiny / légendaire
                
                // Effets de tremblement
                this.cameraShakeTicks = 14;

                // Tonalité de triomphe ultra-riche
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.1f));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0f));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.LIGHTNING_BOLT_THUNDER, 1.3f));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.PLAYER_LEVELUP, 0.8f));

                // Particules dans le monde réel (3D) autour du joueur !
                if (player != null && player.level() != null) {
                    double px = player.getX();
                    double py = player.getY() + 1.2;
                    double pz = player.getZ();
                    for (int p = 0; p < 35; p++) {
                        player.level().addParticle(
                            ParticleTypes.TOTEM_OF_UNDYING, 
                            px + (random.nextDouble() - 0.5) * 1.5, 
                            py + (random.nextDouble() - 0.5) * 1.5, 
                            pz + (random.nextDouble() - 0.5) * 1.5, 
                            (random.nextDouble() - 0.5) * 0.2, 
                            0.1 + random.nextDouble() * 0.2, 
                            (random.nextDouble() - 0.5) * 0.2
                        );
                    }
                }
            } else if (rarity.equals("epic")) {
                particleCount = 50;
                particleColor = 0xFF55FF; // Rose / Magenta
                
                this.cameraShakeTicks = 7;

                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_BLAST, 1.2f));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f));
                
                if (player != null && player.level() != null) {
                    double px = player.getX();
                    double py = player.getY() + 1.2;
                    double pz = player.getZ();
                    for (int p = 0; p < 20; p++) {
                        player.level().addParticle(ParticleTypes.HAPPY_VILLAGER, px, py, pz, (random.nextDouble() - 0.5) * 0.3, 0.1, (random.nextDouble() - 0.5) * 0.3);
                    }
                }
            } else if (rarity.equals("rare")) {
                particleCount = 35;
                particleColor = 0x5555FF; // Bleu
                
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.FIREWORK_ROCKET_SHOOT, 1.0f));
            } else {
                particleCount = 20;
                particleColor = 0x55FF55; // Vert
                
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.ITEM_PICKUP, 0.8f));
            }
        }

        // Génération des particules 2D sur l'écran
        for (int p = 0; p < particleCount; p++) {
            float angle = random.nextFloat() * 2.0f * (float) Math.PI;
            float speed = 2.0f + random.nextFloat() * 7.5f;
            float dx = (float) Math.cos(angle) * speed;
            float dy = (float) Math.sin(angle) * speed - 2.0f; // Force vers le haut au début
            float size = 2.5f + random.nextFloat() * 4.0f;
            int life = 20 + random.nextInt(25);
            boolean isSparkle = (random.nextFloat() < 0.65f); // 65% de chances d'être une étoile croisée

            screenParticles.add(new ScreenParticle(xCenter, yCenter, dx, dy, particleColor, size, life, isSparkle));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();
        if (this.lastTime == 0L) {
            this.lastTime = now;
        }
        float deltaTime = (now - this.lastTime) / 50.0f; // 1.0 per tick (50ms)
        this.lastTime = now;

        this.renderBackground(graphics, mouseX, mouseY, delta);
        this.ticks += deltaTime;

        // Effet de tremblement de caméra global (Cam Shake)
        if (cameraShakeTicks > 0f) {
            cameraShakeTicks -= deltaTime;
            float intensity = 3.5f * (cameraShakeTicks / 14.0f);
            if (intensity < 0f) intensity = 0f;
            float shakeX = (random.nextFloat() - 0.5f) * intensity;
            float shakeY = (random.nextFloat() - 0.5f) * intensity;
            graphics.pose().translate(shakeX, shakeY, 0);
        }

        int cardWidth = 65;
        int spacing = 25;
        int totalWidth = (cardWidth * 5) + (spacing * 4);
        int startX = (this.width / 2) - (totalWidth / 2);
        int yCenter = this.height / 2;

        Lighting.setupForFlatItems();

        for (int i = 0; i < 5; i++) {
            // Animation d'entrée échelonnée (Staggered card entrance)
            float cardIntro = Math.max(0.0f, Math.min(1.0f, (this.ticks - i * 3.0f) / 12.0f));
            float easedIntro = smoothStep(cardIntro);
            
            if (cardIntro > 0.0f && !introSoundPlayed[i]) {
                introSoundPlayed[i] = true;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.BOOK_PAGE_TURN, 1.3f + i * 0.15f));
            }

            // Met à jour la position Y réelle du bouton correspondant pour qu'elle corresponde au visuel glissant !
            int targetY = yCenter - (95 / 2);
            int currentY = targetY + (int)((1.0f - easedIntro) * 250.0f);
            if (cardButtons[i] != null) {
                cardButtons[i].setY(currentY);
            }

            if (clicked[i]) {
                if (shakeTicks[i] > 0f) {
                    shakeTicks[i] -= deltaTime;
                    
                    // Fait jaillir des étincelles pendant la vibration !
                    if (random.nextFloat() < 0.25f * deltaTime) {
                        int xCenter = startX + (i * (cardWidth + spacing)) + (cardWidth / 2);
                        spawnShakingParticles(i, xCenter, yCenter);
                    }

                    if (shakeTicks[i] <= 0f) {
                        int xCenter = startX + (i * (cardWidth + spacing)) + (cardWidth / 2);
                        triggerDopamineEffects(i, xCenter, yCenter);
                    }
                } else if (flipProgress[i] < 1.0f) {
                    flipProgress[i] += getFlipSpeed(i) * deltaTime;
                    if (flipProgress[i] > 1.0f) flipProgress[i] = 1.0f;
                }
            }

            // Gestion du Fade du texte et de l'état survolé
            boolean isHovered = flipProgress[i] >= 1.0f && i < rewards.size() && isHoveringCard(i, mouseX, mouseY);
            if (isHovered) {
                hoverAlpha[i] = Math.min(1.0f, hoverAlpha[i] + 0.15f * deltaTime);
            } else {
                hoverAlpha[i] = Math.max(0.0f, hoverAlpha[i] - 0.15f * deltaTime);
            }

            int xCenter = startX + (i * (cardWidth + spacing)) + (cardWidth / 2);
            
            float easedProgress = smoothStep(flipProgress[i]);
            float rotationY = (1.0f - easedProgress) * 180.0f;

            // Flottement sinusoïdal élégant de la carte face visible + inclinaison Z
            float floatOffset = 0.0f;
            float ZRotation = 0.0f;
            if (flipProgress[i] > 0.0f) {
                floatOffset = (float) Math.sin(ticks * 0.1f + i * 1.2f) * 3.5f * easedProgress;
                if (isHovered) {
                    ZRotation = (float) Math.sin(ticks * 0.15f) * 2.5f;
                }
            }

            float introYOffset = (1.0f - easedIntro) * 250.0f;
            float currentCardY = yCenter + floatOffset + introYOffset;

            graphics.pose().pushPose();
            graphics.pose().translate(xCenter, currentCardY, 100 + i);

            // Dessiner le soleil tournant (God Rays) derrière la carte si révélée et rare+
            if (flipProgress[i] == 1.0f && i < rewards.size()) {
                ItemStack stack = rewards.get(i);
                CardData data = stack.get(ModDataComponents.CARD_DATA);
                if (data != null) {
                    String rarity = data.rarity().toLowerCase();
                    boolean isShiny = data.isShiny();
                    if (isShiny || rarity.equals("legendary") || rarity.equals("mythic") || rarity.equals("epic") || rarity.equals("rare")) {
                        int glowColor = 0x5555FF; // Rare
                        if (isShiny) glowColor = 0xFFFF55;
                        else if (rarity.equals("legendary") || rarity.equals("mythic")) glowColor = 0xFFAA00;
                        else if (rarity.equals("epic")) glowColor = 0xFF55FF;
                        
                        graphics.pose().pushPose();
                        graphics.pose().translate(0, 0, -0.5f);
                        renderCardGlowAndRays(graphics, glowColor);
                        graphics.pose().popPose();
                    }
                }
            }

            // Physique de tremblement si en cours avec intensité crescendo !
            if (clicked[i] && shakeTicks[i] > 0f) {
                float progress = 1.0f - (shakeTicks[i] / getShakeDuration(i));
                float intensity = 1.5f + progress * 2.0f;
                float shakeX = (random.nextFloat() - 0.5f) * intensity;
                float shakeY = (random.nextFloat() - 0.5f) * intensity;
                float shakeZ = (random.nextFloat() - 0.5f) * (intensity * 0.5f);
                graphics.pose().translate(shakeX, shakeY, shakeZ);
                graphics.pose().mulPose(Axis.ZP.rotationDegrees((random.nextFloat() - 0.5f) * (progress * 8.0f)));
            }

            graphics.pose().mulPose(Axis.ZP.rotationDegrees(ZRotation));
            graphics.pose().mulPose(Axis.YP.rotationDegrees(rotationY));
            
            // Effet de zoom / pop au survol dynamique + échelle d'entrée
            float scale = (isHovered ? 118.0f : 110.0f) * (0.5f + 0.5f * easedIntro);
            graphics.pose().scale(scale, -scale, scale);

            if (i < rewards.size()) {
                ItemStack stackToRender = rewards.get(i);
                // Si l'objet n'est pas une carte de base et n'est pas encore retourné à plus de 50%,
                // on affiche un dos de carte générique pour cacher le modèle 3D de l'item/block !
                if (!stackToRender.is(ModItems.CARD) && flipProgress[i] < 0.5f) {
                    stackToRender = new ItemStack(ModItems.CARD);
                }
                renderItem3D(graphics, stackToRender);
            }
            
            graphics.pose().popPose();

            // Particules d'ambiance post-révélation pour les cartes rares
            if (flipProgress[i] == 1.0f && i < rewards.size() && random.nextFloat() < 0.05f * deltaTime) {
                ItemStack stack = rewards.get(i);
                CardData data = stack.get(ModDataComponents.CARD_DATA);
                if (data != null) {
                    String rarity = data.rarity().toLowerCase();
                    boolean isShiny = data.isShiny();
                    if (isShiny || rarity.equals("legendary") || rarity.equals("mythic") || rarity.equals("epic") || rarity.equals("rare")) {
                        int baseColor = 0x5555FF;
                        if (isShiny) baseColor = 0xFFFF55;
                        else if (rarity.equals("legendary") || rarity.equals("mythic")) baseColor = 0xFFAA00;
                        else if (rarity.equals("epic")) baseColor = 0xFF55FF;
                        
                        float px = xCenter + (random.nextFloat() - 0.5f) * 60.0f;
                        float py = currentCardY + (random.nextFloat() - 0.5f) * 90.0f;
                        float dx = (random.nextFloat() - 0.5f) * 0.6f;
                        float dy = -0.1f - random.nextFloat() * 0.4f;
                        screenParticles.add(new ScreenParticle(px, py, dx, dy, baseColor, 1.5f + random.nextFloat() * 1.5f, 15 + random.nextInt(15), true));
                    }
                }
            }

            // Affichage du nom et du bonus avec Fade et flottement assorti
            if (hoverAlpha[i] > 0.01f && i < rewards.size()) {
                ItemStack stack = rewards.get(i);
                CardData data = stack.get(ModDataComponents.CARD_DATA);
                
                int baseColor = 0xAAAAAA;

                if (data != null) {
                    String rarity = data.rarity().toLowerCase();
                    switch (rarity) {
                        case "uncommon" -> baseColor = 0x55FF55;
                        case "rare" -> baseColor = 0x5555FF;
                        case "epic" -> baseColor = 0xFF55FF;
                        case "legendary" -> baseColor = 0xFFAA00;
                        case "mythic" -> baseColor = 0xFF3333;
                    }
                    if (data.isShiny()) {
                        baseColor = 0xFFFF55;
                    }
                }

                // Application de l'alpha à la couleur (format ARGB)
                int alpha = (int)(hoverAlpha[i] * 255);
                int nameColor = (alpha << 24) | (baseColor & 0xFFFFFF);
                int statColor = (alpha << 24) | (0x55FF55 & 0xFFFFFF);

                // Dessiner le nom avec flottement
                graphics.drawCenteredString(this.font, stack.getHoverName().getString(), xCenter, (int)(currentCardY + 70), nameColor);

                // Dessiner le bonus si les stats sont activées et présentes
                if (com.howlite.cobblemoncards.CobblemonCardsConfig.enableCardStats && data != null) {
                    int nextY = (int)(currentCardY + 82);
                    if (data.stat() != null) {
                        float effValue = com.howlite.cobblemoncards.util.CardStatUtil.getEffectiveValue(data.stat(), data.statValue());
                        if (effValue > 0f) {
                            String percent = com.howlite.cobblemoncards.util.CardStatUtil.formatValue(data.stat(), data.statValue());
                            Component bonusText = Component.literal(percent + " ").append(data.stat().getTranslatedName()).withStyle(ChatFormatting.GREEN);
                            graphics.drawCenteredString(this.font, bonusText, xCenter, nextY, statColor);
                            nextY += 12;
                        }
                    }

                    if (data.trainerStat() != null && data.trainerStat().isPresent()
                            && data.trainerStatValue() != null && data.trainerStatValue().isPresent()) {
                        float tEffValue = com.howlite.cobblemoncards.util.CardStatUtil.getEffectiveValue(data.trainerStat().get(), data.trainerStatValue().get());
                        if (tEffValue > 0f) {
                            String tPercent = com.howlite.cobblemoncards.util.CardStatUtil.formatValue(data.trainerStat().get(), data.trainerStatValue().get());
                            Component tBonusText = Component.literal("★ " + tPercent + " ").append(data.trainerStat().get().getTranslatedName()).withStyle(ChatFormatting.GOLD);
                            int tStatColor = (alpha << 24) | (0xFFAA00 & 0xFFFFFF);
                            graphics.drawCenteredString(this.font, tBonusText, xCenter, nextY, tStatColor);
                        }
                    }
                }
            }
        }

        // Désactiver le depth test pour que les particules soient dessinées absolument devant tout !
        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();

        // Rendu et mise à jour des particules 2D
        for (int p = screenParticles.size() - 1; p >= 0; p--) {
            ScreenParticle sp = screenParticles.get(p);
            if (sp.update(deltaTime)) {
                screenParticles.remove(p);
            } else {
                sp.render(graphics);
            }
        }

        // Réactiver le depth test
        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();

        Lighting.setupForFlatItems();

        int cardsRevealed = 0;
        for (boolean c : clicked) if (c) cardsRevealed++;

        if (cardsRevealed < 5) {
            graphics.drawCenteredString(this.font, Component.translatable("screen.cobblemon-cards.booster_pack.click_cards_instruction"), this.width / 2, 30, 0xFFFFFF);
        } else {
            graphics.drawCenteredString(this.font, Component.translatable("screen.cobblemon-cards.booster_pack.open_instruction"), this.width / 2, 30, 0xFFFF55);
        }
    }

    private boolean isHoveringCard(int index, int mouseX, int mouseY) {
        Button btn = cardButtons[index];
        return btn != null && mouseX >= btn.getX() && mouseX <= btn.getX() + btn.getWidth() &&
               mouseY >= btn.getY() && mouseY <= btn.getY() + btn.getHeight();
    }

    private void renderItem3D(GuiGraphics graphics, ItemStack stack) {
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GUI,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                graphics.pose(),
                graphics.bufferSource(),
                Minecraft.getInstance().level,
                0
        );
        
        graphics.flush();
    }

    // Classe interne représentant une particule à l'écran
    private static class ScreenParticle {
        float x, y;
        float dx, dy;
        int color;
        float scale;
        float life;
        float maxLife;
        boolean isSparkle;

        public ScreenParticle(float x, float y, float dx, float dy, int color, float scale, float maxLife, boolean isSparkle) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.color = color;
            this.scale = scale;
            this.life = maxLife;
            this.maxLife = maxLife;
            this.isSparkle = isSparkle;
        }

        public boolean update(float delta) {
            this.x += this.dx * delta;
            this.y += this.dy * delta;
            // Gravité et frottements légers
            this.dy += 0.08f * delta;
            this.dx *= (float) Math.pow(0.98, delta);
            this.dy *= (float) Math.pow(0.98, delta);
            this.life -= delta;
            return this.life <= 0f;
        }

        public void render(GuiGraphics graphics) {
            float alpha = this.life / this.maxLife;
            if (alpha < 0f) alpha = 0f;
            if (alpha > 1f) alpha = 1f;
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            int argb = ((int)(alpha * 255) << 24) | (r << 16) | (g << 8) | b;
            
            float size = scale * (0.4f + 0.6f * alpha);
            int rx = Math.round(x);
            int ry = Math.round(y);
            int s = Math.round(size);
            if (s < 1) s = 1;
            
            if (isSparkle) {
                // Rendu en croix étincelante (sparkle) style diamant
                graphics.fill(rx - 1, ry - s, rx + 1, ry + s, argb); // Ligne verticale
                graphics.fill(rx - s, ry - 1, rx + s, ry + 1, argb); // Ligne horizontale
                // Hot core blanc brillant
                graphics.fill(rx - 1, ry - 1, rx + 1, ry + 1, ((int)(alpha * 255) << 24) | 0xFFFFFF);
            } else {
                // Rendu standard carré/cercle
                graphics.fill(rx - s, ry - s, rx + s, ry + s, argb);
            }
        }
    }
}
