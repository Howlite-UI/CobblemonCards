package com.howlite.cobblemoncards.screen;

import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.menu.BinderMenu;
import com.howlite.cobblemoncards.network.BinderPagePayload;
import com.howlite.cobblemoncards.network.SortBinderPayload;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.howlite.cobblemoncards.util.PlatformHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class BinderScreen extends AbstractContainerScreen<BinderMenu> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "textures/gui/binder.png");
    private static final WidgetSprites NEXT_PAGE_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "button/next_page"),
            ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "button/next_page_highlighted")
    );
    private static final WidgetSprites PREV_PAGE_SPRITES = new WidgetSprites(
            ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "button/prev_page"),
            ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "button/prev_page_highlighted")
    );

    private static final int CARD_SLOT_WIDTH = 38;
    private static final int CARD_SLOT_HEIGHT = 54;
    private static final int CARD_OFFSET_X = 11;
    private static final int CARD_OFFSET_Y = 19;

    private boolean isRendering = false;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private EditBox searchBox;
    private EditBox pageJumpBox;

    public BinderScreen(BinderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 260; 
        this.imageHeight = 320;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = -1000;
        this.titleLabelY = -1000;

        if (this.menu.getMaxPages() == 1000) {
            int navY = this.topPos + 213;

            this.prevButton = this.addRenderableWidget(new ImageButton(
                    this.leftPos + 65, navY, 18, 10, PREV_PAGE_SPRITES, (button) -> {
                        changePage(net.minecraft.client.gui.screens.Screen.hasShiftDown() ? -10 : -1);
                    }));
            this.prevButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal("Click: -1 page | Shift+Click: -10 pages")));

            this.nextButton = this.addRenderableWidget(new ImageButton(
                    this.leftPos + 164, navY, 18, 10, NEXT_PAGE_SPRITES, (button) -> {
                        changePage(net.minecraft.client.gui.screens.Screen.hasShiftDown() ? 10 : 1);
                    }));
            this.nextButton.setTooltip(net.minecraft.client.gui.components.Tooltip.create(
                    Component.literal("Click: +1 page | Shift+Click: +10 pages")));

            this.pageJumpBox = new EditBox(this.font, this.leftPos + 87, navY, 40, 12, Component.empty());
            this.pageJumpBox.setMaxLength(4);
            this.pageJumpBox.setValue(String.valueOf(this.menu.getCurrentPage() + 1));
            this.pageJumpBox.setTooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal("Type a page number and press Enter")));
            this.addRenderableWidget(this.pageJumpBox);
        } else {
            this.nextButton = this.addRenderableWidget(new ImageButton(this.leftPos + 225, this.topPos + 150, 18, 10, NEXT_PAGE_SPRITES, (button) -> {
                changePage(1);
            }));

            this.prevButton = this.addRenderableWidget(new ImageButton(this.leftPos + 15, this.topPos + 150, 18, 10, PREV_PAGE_SPRITES, (button) -> {
                changePage(-1);
            }));
        }

        int widgetsY = this.topPos - 18;
        this.searchBox = new EditBox(this.font, this.leftPos + 46, widgetsY, 80, 12, Component.translatable("gui.cobblemon-cards.binder.search"));
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblemon-cards.binder.sort.pokemon"), (button) -> {
            PlatformHelper.INSTANCE.sendToServer(new SortBinderPayload(1));
        }).bounds(this.leftPos + 130, widgetsY, 40, 12).build());
        
        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblemon-cards.binder.sort.rarity"), (button) -> {
            PlatformHelper.INSTANCE.sendToServer(new SortBinderPayload(2));
        }).bounds(this.leftPos + 172, widgetsY, 40, 12).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblemon-cards.binder.sort.grade"), (button) -> {
            PlatformHelper.INSTANCE.sendToServer(new SortBinderPayload(3));
        }).bounds(this.leftPos + 214, widgetsY, 40, 12).build());

        updateButtonVisibility();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                return true;
            }
        }
        if (this.pageJumpBox != null && this.pageJumpBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                jumpToPage();
                return true;
            }
            if (this.pageJumpBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void jumpToPage() {
        if (this.pageJumpBox == null) return;
        String text = this.pageJumpBox.getValue().trim();
        try {
            int targetPage = Integer.parseInt(text) - 1;
            int clampedPage = Math.max(0, Math.min(targetPage, this.menu.getMaxPages() - 1));
            this.menu.setPage(clampedPage);
            PlatformHelper.INSTANCE.sendToServer(new BinderPagePayload(clampedPage));
            updateButtonVisibility();
            this.pageJumpBox.setValue(String.valueOf(clampedPage + 1));
        } catch (NumberFormatException e) {
            this.pageJumpBox.setValue(String.valueOf(this.menu.getCurrentPage() + 1));
        }
        this.pageJumpBox.setFocused(false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY > 0) {
            changePage(1);
        } else if (scrollY < 0) {
            changePage(-1);
        }
        return true;
    }

    private void changePage(int diff) {
        int newPage = Math.max(0, Math.min(this.menu.getCurrentPage() + diff, this.menu.getMaxPages() - 1));
        this.menu.setPage(newPage);
        PlatformHelper.INSTANCE.sendToServer(new BinderPagePayload(newPage));
        if (this.pageJumpBox != null) {
            this.pageJumpBox.setValue(String.valueOf(newPage + 1));
        }
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        this.nextButton.visible = this.menu.getCurrentPage() < this.menu.getMaxPages() - 1;
        this.prevButton.visible = this.menu.getCurrentPage() > 0;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.menu.getMaxPages() == 1000) {
            guiGraphics.drawString(this.font,
                    Component.literal("/ " + this.menu.getMaxPages()),
                    130, 213 + 2, 0x555555, false);
        } else {
            Component pageText = Component.literal((this.menu.getCurrentPage() + 1) + " / " + this.menu.getMaxPages());
            guiGraphics.drawString(this.font, pageText, (this.imageWidth - this.font.width(pageText)) / 2, 215, 0x444444, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.isRendering = true;
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        this.isRendering = false;

        this.hoveredSlot = null;
        int activeStart = this.menu.getCurrentPage() * BinderMenu.SLOTS_PER_PAGE;
        int activeEnd = Math.min(activeStart + BinderMenu.SLOTS_PER_PAGE, this.menu.slots.size());
        for (int i = activeStart; i < activeEnd; i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot.isActive() && this.isHovering(slot.x - CARD_OFFSET_X, slot.y - CARD_OFFSET_Y, CARD_SLOT_WIDTH, CARD_SLOT_HEIGHT, mouseX, mouseY)) {
                this.hoveredSlot = slot;
                break;
            }
        }
        if (this.hoveredSlot == null) {
            int totalBinderSlots = this.menu.getMaxPages() * BinderMenu.SLOTS_PER_PAGE;
            for (int i = totalBinderSlots; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    this.hoveredSlot = slot;
                    break;
                }
            }
        }

        if (this.hoveredSlot != null && this.hoveredSlot.index < this.menu.getMaxPages() * BinderMenu.SLOTS_PER_PAGE) {
            int x = this.leftPos + this.hoveredSlot.x - CARD_OFFSET_X;
            int y = this.topPos + this.hoveredSlot.y - CARD_OFFSET_Y;
            graphics.fill(x, y, x + CARD_SLOT_WIDTH, y + CARD_SLOT_HEIGHT, -2130706433);
        } else if (this.hoveredSlot != null) {
            int x = this.leftPos + this.hoveredSlot.x;
            int y = this.topPos + this.hoveredSlot.y;
            graphics.fill(x, y, x + 16, y + 16, -2130706433);
        }

        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
        if (this.isRendering) {
            return false;
        }
        if (width == 16 && height == 16) {
            int activeStart = this.menu.getCurrentPage() * BinderMenu.SLOTS_PER_PAGE;
            int activeEnd = Math.min(activeStart + BinderMenu.SLOTS_PER_PAGE, this.menu.slots.size());
            for (int i = activeStart; i < activeEnd; i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.x == x && slot.y == y) {
                    return super.isHovering(x - CARD_OFFSET_X, y - CARD_OFFSET_Y, CARD_SLOT_WIDTH, CARD_SLOT_HEIGHT, mouseX, mouseY);
                }
            }
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int x = this.leftPos;
        int y = this.topPos;
        graphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 512, 512);

        if (this.menu.getMaxPages() < 50) {
            renderStatsPanel(graphics, x - 135, y);
        }
    }

    private void renderStatsPanel(GuiGraphics graphics, int x, int y) {
        int panelWidth = 130;
        int panelHeight = 200;
        graphics.blit(TEXTURE, x, y, 261, 0, panelWidth, panelHeight, 512, 512);
        graphics.drawString(this.font, Component.translatable("gui.cobblemon-cards.binder.stats_bonus"), x + 10, y + 10, 0xFFFFFF, true);

        Map<CardStat, Float> stats = new HashMap<>();
        for (int i = 0; i < this.menu.getMaxPages() * BinderMenu.SLOTS_PER_PAGE; i++) {
            ItemStack stack = this.menu.getSlot(i).getItem();
            CardData data = stack.get(ModDataComponents.CARD_DATA);
            if (data != null && !com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(data.pokemonId())) {
                if (data.stat() != null) {
                    stats.put(data.stat(), stats.getOrDefault(data.stat(), 0f) + data.statValue());
                }
                if (data.trainerStat() != null && data.trainerStat().isPresent()
                        && data.trainerStatValue() != null && data.trainerStatValue().isPresent()) {
                    CardStat tStat = data.trainerStat().get();
                    float tVal = data.trainerStatValue().get();
                    stats.put(tStat, stats.getOrDefault(tStat, 0f) + tVal);
                }
            }
        }

        int startY = y + 30;
        if (stats.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.cobblemon-cards.binder.no_bonus"), x + 10, startY, 0xAAAAAA, false);
        } else {
            boolean anyShown = false;
            for (Map.Entry<CardStat, Float> entry : stats.entrySet()) {
                // Same maths as the binder tooltip: the config multiplier is applied exactly once.
                float value = com.howlite.cobblemoncards.util.CardStatUtil.getEffectiveValue(entry.getKey(), entry.getValue());
                if (value <= 0f) continue;
                // The formatting depends on how the stat is applied (flat value vs percentage of base).
                String formatted = com.howlite.cobblemoncards.util.CardStatUtil.formatValue(entry.getKey(), entry.getValue());
                Component text = Component.translatable("gui.cobblemon-cards.binder.stat_entry_value", entry.getKey().getTranslatedName(), formatted);
                graphics.drawString(this.font, text, x + 10, startY, 0x55FF55, false);
                startY += 10;
                anyShown = true;
            }
            if (!anyShown) {
                graphics.drawString(this.font, Component.translatable("gui.cobblemon-cards.binder.no_bonus"), x + 10, startY, 0xAAAAAA, false);
            }
        }
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!slot.isActive()) return;

        if (slot.index < this.menu.getMaxPages() * BinderMenu.SLOTS_PER_PAGE) {
            ItemStack stack = slot.getItem();
            if (!stack.isEmpty()) {
                String searchTerm = searchBox.getValue().toLowerCase();
                CardData data = stack.get(ModDataComponents.CARD_DATA);
                
                boolean matches = true;
                if (!searchTerm.isEmpty() && data != null) {
                    String displayName = data.pokemonId();
                    if (displayName.startsWith("player_")) {
                        displayName = displayName.substring("player_".length());
                    }
                    Component translatableName = Component.translatable("cobblemon.species." + data.pokemonId());
                    String name = translatableName.getString().toLowerCase();
                    if (!name.contains(searchTerm) && !data.pokemonId().toLowerCase().contains(searchTerm) && !displayName.toLowerCase().contains(searchTerm)) {
                        matches = false;
                    }
                }
                
                graphics.pose().pushPose();
                int x = slot.x;
                float centerX = x + 8;
                float centerY = slot.y + 8;
                graphics.pose().translate(centerX, centerY, 100);
                float scale = 50.0f; 
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
                
                if (!matches) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);
                    int slotX = slot.x;
                    graphics.fill(slotX - CARD_OFFSET_X, slot.y - CARD_OFFSET_Y, slotX - CARD_OFFSET_X + CARD_SLOT_WIDTH, slot.y - CARD_OFFSET_Y + CARD_SLOT_HEIGHT, 0xAA000000);
                    graphics.pose().popPose();
                }
            }
        } else {
            super.renderSlot(graphics, slot);
        }
    }
}
