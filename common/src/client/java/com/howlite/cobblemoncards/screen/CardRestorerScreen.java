package com.howlite.cobblemoncards.screen;

import com.howlite.cobblemoncards.menu.CardRestorerMenu;
import com.howlite.cobblemoncards.network.ChangeRestorerTargetGradePayload;
import com.howlite.cobblemoncards.network.PerformRestorerPayload;
import com.howlite.cobblemoncards.util.PlatformHelper;
import com.mojang.blaze3d.platform.Lighting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CardRestorerScreen extends AbstractContainerScreen<CardRestorerMenu> {

    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(
            "cobblemon-cards", "textures/gui/card_restorer.png"
    );

    private static final int GUI_WIDTH = 232;
    private static final int GUI_HEIGHT = 202;
    private static final int TEX_SIZE = 512;

    // =========================================================================
    // Barre de grade (10 segments) — piste dans le GUI
    // =========================================================================
    private static final int GRADE_BAR_X       = 27;
    private static final int GRADE_BAR_Y       = 108;
    private static final int GRADE_BAR_TOTAL_W = 178;
    private static final int GRADE_BAR_H       = 6;

    private static final int UV_GRADE_BLUE_U  = 240;
    private static final int UV_GRADE_BLUE_V  = 0;
    private static final int UV_GRADE_GREEN_U = 240;
    private static final int UV_GRADE_GREEN_V = 6;

    // =========================================================================
    // Boutons [-] [box] [+]
    // =========================================================================
    private static final int BTN_MINUS_X  = 29;
    private static final int BTN_MINUS_Y  = 86;
    private static final int BTN_MIDDLE_X = 41;
    private static final int BTN_MIDDLE_Y = 86;
    private static final int BTN_PLUS_X   = 62;
    private static final int BTN_PLUS_Y   = 86;

    private static final int UV_BTN_MINUS_U  = 272;
    private static final int UV_BTN_MINUS_V  = 16;
    private static final int UV_BTN_MIDDLE_U = 283;
    private static final int UV_BTN_MIDDLE_V = 16;
    private static final int UV_BTN_PLUS_U   = 303;
    private static final int UV_BTN_PLUS_V   = 16;

    // =========================================================================
    // Réservoir vertical de Dust (barre violette hachurée)
    // =========================================================================
    private static final int RESERVOIR_X = 161;
    private static final int RESERVOIR_Y = 24;
    private static final int RESERVOIR_W = 17;
    private static final int RESERVOIR_H = 71;

    private static final int UV_RESERVOIR_FILL_U    = 240;
    private static final int UV_RESERVOIR_FILL_V    = 16;
    private static final int RESERVOIR_FILL_TILE_H  = 70;

    // =========================================================================
    // BOUTON RESTORE
    // =========================================================================
    private static final int RESTORE_BTN_X = 81;
    private static final int RESTORE_BTN_Y = 82;
    private static final int RESTORE_BTN_W = 70;
    private static final int RESTORE_BTN_H = 14;

    private static final int UV_RESTORE_BTN_BASE_U     = 272;
    private static final int UV_RESTORE_BTN_BASE_V     = 32;
    private static final int UV_RESTORE_BTN_SELECTED_U = 272;
    private static final int UV_RESTORE_BTN_SELECTED_V = 46;
    private static final int UV_RESTORE_BTN_BLOCKED_U  = 272;
    private static final int UV_RESTORE_BTN_BLOCKED_V  = 60;

    // =========================================================================
    // Zone centrale du cadre de carte (X=96..135, Y=23..75, W=40, H=53)
    // =========================================================================
    private static final int CARD_FRAME_X = 96;
    private static final int CARD_FRAME_Y = 23;
    private static final int CARD_FRAME_W = 40;
    private static final int CARD_FRAME_H = 53;
    private static final int CARD_HITBOX_H = CARD_FRAME_H + 2;

    // Local observations are used only for visual/audio feedback, never for processing.
    private boolean wasRestoring = false;
    private int previousCardGrade = 0;
    private int previousRestoreProgress = -1;

    // Particules et flash visuel
    private final List<RestorerParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private int successFlashTicks = 0;

    public CardRestorerScreen(CardRestorerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY = 3;
        this.inventoryLabelY = this.imageHeight + 10;
        this.particles.clear();
        this.successFlashTicks = 0;
        this.wasRestoring = menu.isRestoring();
        this.previousCardGrade = menu.getCurrentCardGrade();
        this.previousRestoreProgress = menu.getRestoreProgress();
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.successFlashTicks > 0) this.successFlashTicks--;
        for (int i = particles.size() - 1; i >= 0; i--) {
            if (particles.get(i).tick()) particles.remove(i);
        }

        int cardCenterX = this.leftPos + CARD_FRAME_X + CARD_FRAME_W / 2;
        int cardCenterY = this.topPos + CARD_FRAME_Y + CARD_FRAME_H / 2;
        int progress = menu.getRestoreProgress();
        if (menu.isRestoring() && menu.hasEnoughDustForRestore()) {
            if (progress != previousRestoreProgress && progress > 0 && progress % 15 == 0) {
                float pitch = 0.9f + menu.getRestoreProgressFraction() * 0.6f;
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME, pitch));
            }
            if (random.nextFloat() < 0.4f) {
                float rx = cardCenterX + (random.nextFloat() - 0.5f) * (CARD_FRAME_W - 4);
                float ry = cardCenterY + (random.nextFloat() - 0.5f) * (CARD_FRAME_H - 4);
                int pColor = random.nextBoolean() ? 0xC768FF : 0xFFDF70;
                particles.add(new RestorerParticle(rx, ry,
                        (random.nextFloat() - 0.5f) * 0.3f,
                        -0.4f - random.nextFloat() * 0.3f, pColor, 1.5f, 16));
            }
        }

        if (wasRestoring && menu.getCurrentCardGrade() > previousCardGrade) {
            this.successFlashTicks = 12;
            for (int p = 0; p < 12; p++) {
                float angle = random.nextFloat() * (float) (Math.PI * 2);
                float spd = 0.8f + random.nextFloat() * 1.5f;
                int pColor = random.nextBoolean() ? 0xFFFF77 : 0xC768FF;
                particles.add(new RestorerParticle(cardCenterX, cardCenterY,
                        (float) Math.cos(angle) * spd, (float) Math.sin(angle) * spd,
                        pColor, 1.5f, 16));
            }
        }
        wasRestoring = menu.isRestoring();
        previousCardGrade = menu.getCurrentCardGrade();
        previousRestoreProgress = progress;
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (slot == this.menu.getSlot(0)) {
            return; // Slot carte rendu en taille agrandie via renderCenterEnlargedCard
        }
        super.renderSlot(graphics, slot);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        Slot cardSlot = this.menu.getSlot(0);
        if (cardSlot != null && cardSlot.x == x && cardSlot.y == y) {
            return super.isHovering(CARD_FRAME_X, CARD_FRAME_Y, CARD_FRAME_W, CARD_HITBOX_H, mouseX, mouseY);
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);

        // Highlight du cadre complet de la carte au survol
        Slot cardSlot = this.menu.getSlot(0);
        if (this.hoveredSlot == cardSlot) {
            graphics.fill(this.leftPos + CARD_FRAME_X, this.topPos + CARD_FRAME_Y,
                    this.leftPos + CARD_FRAME_X + CARD_FRAME_W, this.topPos + CARD_FRAME_Y + CARD_HITBOX_H,
                    0x25FFFFFF);
        }

        // Rendu de la carte en GRAND au centre du cadre
        renderCenterEnlargedCard(graphics);

        // Feedbacks visuels (aura d'énergie, flash de réussite, particules)
        renderVisualFeedback(graphics);

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. Fond principal
        graphics.blit(TEXTURE, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT, TEX_SIZE, TEX_SIZE);

        // 2. Réservoir de Dust
        renderDustReservoir(graphics, x, y);

        // 3. Barre de grade
        renderGradeBar(graphics, x, y);

        // 4. Boutons [-] [box] [+]
        renderButtons(graphics, x, y);

        // 5. Grand bouton Restore
        renderRestoreButton(graphics, x, y);

        // 6. Textes d'information
        renderDustCostText(graphics, x, y);
    }

    private void renderDustReservoir(GuiGraphics graphics, int x, int y) {
        int stored = menu.getStoredDust();
        int max    = menu.getMaxStoredDust();

        if (stored > 0 && max > 0) {
            float ratio = Math.min(1.0f, (float) stored / (float) max);
            int   fillH = (int) (ratio * RESERVOIR_H);
            if (fillH > 0) {
                int drawY   = y + RESERVOIR_Y + (RESERVOIR_H - fillH);
                int drawX   = x + RESERVOIR_X + 1;
                int spriteV = UV_RESERVOIR_FILL_V + (RESERVOIR_FILL_TILE_H - fillH);
                graphics.blit(TEXTURE,
                        drawX, drawY,
                        UV_RESERVOIR_FILL_U, spriteV,
                        16, fillH,
                        TEX_SIZE, TEX_SIZE);
            }
        }
    }

    private void renderCenterEnlargedCard(GuiGraphics graphics) {
        Slot cardSlot = this.menu.getSlot(0);
        if (cardSlot != null && cardSlot.hasItem()) {
            ItemStack stack = cardSlot.getItem();
            if (!stack.isEmpty()) {
                graphics.pose().pushPose();

                // Centré dans le cadre X=96..135 (centre=116), Y=23..75 (centre=50)
                int centerX = this.leftPos + CARD_FRAME_X + (CARD_FRAME_W / 2);
                int centerY = this.topPos  + CARD_FRAME_Y + (CARD_FRAME_H / 2) + 1;

                graphics.pose().translate(centerX, centerY, 150);
                float scale = 48.0f;
                graphics.pose().scale(scale, -scale, scale);

                Lighting.setupForFlatItems();
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
                graphics.pose().popPose();
            }
        }
    }

    private void renderVisualFeedback(GuiGraphics graphics) {
        // Contour lumineux subtil autour de la carte pendant la restauration
        if (menu.isRestoring()) {
            float pulse = (float) Math.sin(menu.getRestoreProgress() * 0.25f) * 0.5f + 0.5f;
            int alpha = (int) (40 + pulse * 60);
            int outlineColor = (alpha << 24) | 0xBA55D3;
            graphics.renderOutline(this.leftPos + CARD_FRAME_X - 1, this.topPos + CARD_FRAME_Y - 1,
                    CARD_FRAME_W + 2, CARD_FRAME_H + 2, outlineColor);
        }

        // Bref éclat doux sur la carte à la complétion
        if (successFlashTicks > 0) {
            float flashAlpha = (float) successFlashTicks / 12.0f;
            int flashA = (int) (flashAlpha * 70);
            graphics.fill(this.leftPos + CARD_FRAME_X, this.topPos + CARD_FRAME_Y,
                    this.leftPos + CARD_FRAME_X + CARD_FRAME_W, this.topPos + CARD_FRAME_Y + CARD_FRAME_H,
                    (flashA << 24) | 0xFFFFFF);
        }

        // Rendu des particules magiques
        for (RestorerParticle p : particles) {
            p.render(graphics);
        }
    }

    private void renderRestoreButton(GuiGraphics graphics, int x, int y) {
        int btnX = x + RESTORE_BTN_X;
        int btnY = y + RESTORE_BTN_Y;

        if (menu.isRestoring()) {
            graphics.blit(TEXTURE, btnX, btnY, UV_RESTORE_BTN_BLOCKED_U, UV_RESTORE_BTN_BLOCKED_V, RESTORE_BTN_W, RESTORE_BTN_H, TEX_SIZE, TEX_SIZE);

            String text = formatDuration(menu.getRemainingRestoreTicks());
            int textW = font.width(text);
            graphics.drawString(font, text, btnX + (RESTORE_BTN_W - textW) / 2, btnY + 3, 0xFFFF55, true);
        } else if (menu.canRestore()) {
            graphics.blit(TEXTURE, btnX, btnY, UV_RESTORE_BTN_SELECTED_U, UV_RESTORE_BTN_SELECTED_V, RESTORE_BTN_W, RESTORE_BTN_H, TEX_SIZE, TEX_SIZE);

            String text = "RESTORE";
            int textW = font.width(text);
            graphics.drawString(font, text, btnX + (RESTORE_BTN_W - textW) / 2, btnY + 3, 0xFFFFFF, true);
        } else {
            graphics.blit(TEXTURE, btnX, btnY, UV_RESTORE_BTN_BASE_U, UV_RESTORE_BTN_BASE_V, RESTORE_BTN_W, RESTORE_BTN_H, TEX_SIZE, TEX_SIZE);

            String text = "RESTORE";
            int textW = font.width(text);
            graphics.drawString(font, text, btnX + (RESTORE_BTN_W - textW) / 2, btnY + 3, 0x888888, false);
        }
    }

    private void renderGradeBar(GuiGraphics graphics, int x, int y) {
        int currentGrade = menu.getCurrentCardGrade();
        int targetGrade  = menu.getTargetGrade();

        for (int i = 1; i <= 10; i++) {
            int segX  = (int) ((i - 1) * 17.8f);
            int nextX = (int) (i       * 17.8f);
            int segW  = nextX - segX;

            int cellX = x + GRADE_BAR_X + segX;
            int cellY = y + GRADE_BAR_Y;

            if (i <= currentGrade) {
                int spriteU = UV_GRADE_GREEN_U + segX;
                graphics.blit(TEXTURE, cellX, cellY,
                        spriteU, UV_GRADE_GREEN_V,
                        segW, GRADE_BAR_H,
                        TEX_SIZE, TEX_SIZE);
            } else if (i <= targetGrade) {
                int spriteU = UV_GRADE_BLUE_U + segX;
                graphics.blit(TEXTURE, cellX, cellY,
                        spriteU, UV_GRADE_BLUE_V,
                        segW, GRADE_BAR_H,
                        TEX_SIZE, TEX_SIZE);
            } else {
                graphics.fill(cellX, cellY, cellX + segW, cellY + GRADE_BAR_H, 0xFF3A3A3A);
            }
        }
    }

    private void renderButtons(GuiGraphics graphics, int x, int y) {
        graphics.blit(TEXTURE,
                x + BTN_MINUS_X, y + BTN_MINUS_Y,
                UV_BTN_MINUS_U, UV_BTN_MINUS_V,
                10, 10, TEX_SIZE, TEX_SIZE);

        graphics.blit(TEXTURE,
                x + BTN_MIDDLE_X, y + BTN_MIDDLE_Y,
                UV_BTN_MIDDLE_U, UV_BTN_MIDDLE_V,
                19, 10, TEX_SIZE, TEX_SIZE);

        graphics.blit(TEXTURE,
                x + BTN_PLUS_X, y + BTN_PLUS_Y,
                UV_BTN_PLUS_U, UV_BTN_PLUS_V,
                10, 10, TEX_SIZE, TEX_SIZE);

        int    targetGrade = menu.getTargetGrade();
        String targetText  = targetGrade > 0 ? String.valueOf(targetGrade) : "-";
        int    textW       = font.width(targetText);
        graphics.drawString(font, targetText,
                x + BTN_MIDDLE_X + (19 - textW) / 2,
                y + BTN_MIDDLE_Y + 1,
                0xFFFFFF, false);
    }

    private void renderDustCostText(GuiGraphics graphics, int x, int y) {
        int currentGrade = menu.getCurrentCardGrade();
        int dustCost     = menu.getDustCost();
        int targetGrade  = menu.getTargetGrade();

        int textX = x + 12;
        int textY = y + 36;

        if (currentGrade <= 0) {
            graphics.drawString(font, "Insert Card",  textX, textY,      0xAAAAAA, false);
            graphics.drawString(font, "Grade 1-10",   textX, textY + 11, 0x777777, false);
        } else if (targetGrade <= currentGrade) {
            graphics.drawString(font, "Grade " + currentGrade, textX, textY,      0x55FF55, false);
            graphics.drawString(font, "Use + button",          textX, textY + 11, 0xAAAAAA, false);
        } else {
            graphics.drawString(font, "Cost: " + dustCost + " Dust",
                    textX, textY,      0xFFFF55, false);
            graphics.drawString(font, "G" + currentGrade + " \u2192 G" + targetGrade,
                    textX, textY + 11, 0x55FFFF, false);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;

        // Clic sur bouton [-]
        if (isInBounds(mouseX, mouseY, x + BTN_MINUS_X, y + BTN_MINUS_Y, 10, 10)) {
            if (menu.isRestoring()) return true;
            int currentGrade = menu.getCurrentCardGrade();
            int targetGrade = menu.getTargetGrade();
            int newTarget = Math.max(currentGrade + 1, targetGrade - 1);
            if (newTarget <= currentGrade) {
                newTarget = 0;
            }
            PlatformHelper.INSTANCE.sendToServer(new ChangeRestorerTargetGradePayload(newTarget));
            return true;
        }

        // Clic sur bouton [+]
        if (isInBounds(mouseX, mouseY, x + BTN_PLUS_X, y + BTN_PLUS_Y, 10, 10)) {
            if (menu.isRestoring()) return true;
            int currentGrade = menu.getCurrentCardGrade();
            int targetGrade = menu.getTargetGrade();
            int newTarget;
            if (targetGrade <= currentGrade) {
                newTarget = Math.min(10, currentGrade + 1);
            } else {
                newTarget = Math.min(10, targetGrade + 1);
            }
            PlatformHelper.INSTANCE.sendToServer(new ChangeRestorerTargetGradePayload(newTarget));
            return true;
        }

        // Clic sur le grand bouton Restore
        if (isInBounds(mouseX, mouseY, x + RESTORE_BTN_X, y + RESTORE_BTN_Y, RESTORE_BTN_W, RESTORE_BTN_H)) {
            if (menu.canRestore()) {
                PlatformHelper.INSTANCE.sendToServer(new PerformRestorerPayload());
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, 1.3F
                        )
                );
                Minecraft.getInstance().getSoundManager().play(
                        net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                                net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, 1.4F
                        )
                );
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String formatDuration(int ticks) {
        if (ticks < 1200) return String.format("%.1fs", ticks / 20.0f);
        int seconds = (ticks + 19) / 20;
        return String.format("%d:%02d", seconds / 60, seconds % 60);
    }

    private boolean isInBounds(double mouseX, double mouseY, int bx, int by, int bw, int bh) {
        return mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        // Tooltip — Réservoir de Dust
        if (isInBounds(mouseX, mouseY, x + RESERVOIR_X, y + RESERVOIR_Y, RESERVOIR_W, RESERVOIR_H)) {
            graphics.renderTooltip(font,
                    Component.literal("Stored Dust: "
                            + String.format("%,d", menu.getStoredDust())
                            + " / "
                            + String.format("%,d", menu.getMaxStoredDust())),
                    mouseX, mouseY);
        }

        // Tooltip sur le grand bouton Restore
        if (isInBounds(mouseX, mouseY, x + RESTORE_BTN_X, y + RESTORE_BTN_Y, RESTORE_BTN_W, RESTORE_BTN_H)) {
            if (menu.isRestoring()) {
                String key = menu.hasEnoughDustForRestore() ? "restoring" : "paused";
                graphics.renderTooltip(font, Component.translatable("gui.cobblemon-cards.card_restorer." + key,
                        formatDuration(menu.getRemainingRestoreTicks())), mouseX, mouseY);
            } else if (menu.canRestore()) {
                graphics.renderTooltip(font, Component.translatable("gui.cobblemon-cards.card_restorer.restore_duration",
                        formatDuration(menu.getExpectedRestoreDuration())), mouseX, mouseY);
            } else if (menu.getCurrentCardGrade() > 0 && menu.getTargetGrade() > menu.getCurrentCardGrade()) {
                graphics.renderTooltip(font, Component.translatable("gui.cobblemon-cards.card_restorer.not_enough_dust"), mouseX, mouseY);
            }
        }

        // Tooltip Barre de grade
        if (isInBounds(mouseX, mouseY, x + GRADE_BAR_X, y + GRADE_BAR_Y, GRADE_BAR_TOTAL_W, GRADE_BAR_H)) {
            double relX = mouseX - (x + GRADE_BAR_X);
            int hoverGrade = Math.min(10, Math.max(1, (int) (relX / 17.8f) + 1));
            graphics.renderTooltip(font, Component.literal("Grade " + hoverGrade), mouseX, mouseY);
        }
    }

    // =========================================================================
    // Classe interne pour les particules d'interface
    // =========================================================================
    private static class RestorerParticle {
        float x, y;
        float vx, vy;
        int color;
        float size;
        int maxAge;
        int age;

        RestorerParticle(float x, float y, float vx, float vy, int color, float size, int maxAge) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.color = color;
            this.size = size;
            this.maxAge = maxAge;
            this.age = 0;
        }

        boolean tick() {
            x += vx;
            y += vy;
            vx *= 0.94f;
            vy *= 0.94f;
            return ++age >= maxAge;
        }

        void render(GuiGraphics g) {
            float alpha = 1.0f - (float) age / maxAge;
            int a = (int) (alpha * 255);
            int argb = (a << 24) | (color & 0x00FFFFFF);
            g.fill((int) (x - size / 2), (int) (y - size / 2),
                    (int) (x + size / 2), (int) (y + size / 2), argb);
        }
    }
}
