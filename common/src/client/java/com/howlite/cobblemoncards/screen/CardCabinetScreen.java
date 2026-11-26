package com.howlite.cobblemoncards.screen;

import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.menu.CardCabinetMenu;
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
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.Screen;
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

public class CardCabinetScreen extends AbstractContainerScreen<CardCabinetMenu> {
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

    // Navigation layout constants (relative to leftPos/topPos)
    // GUI width = 260, center = 130
    // Prev arrow  : x=65  w=18  → ends at 83
    // EditBox     : x=87  w=40  → ends at 127
    // "/ 1000"    : x=130       → ~30px wide → ends at ~160
    // Next arrow  : x=164 w=18  → ends at 182
    private static final int NAV_PREV_X  = 65;
    private static final int NAV_BOX_X   = 87;
    private static final int NAV_BOX_W   = 40;
    private static final int NAV_SLASH_X = 130;
    private static final int NAV_NEXT_X  = 164;
    private static final int NAV_Y_OFFSET = 213;

    private boolean isRendering = false;
    private ImageButton nextButton;
    private ImageButton prevButton;
    private EditBox searchBox;
    private EditBox pageJumpBox;

    public CardCabinetScreen(CardCabinetMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 260;
        this.imageHeight = 320;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = -1000;
        this.titleLabelY = -1000;

        int navY = this.topPos + NAV_Y_OFFSET;

        // ← Previous (shift-click = -10)
        this.prevButton = this.addRenderableWidget(new ImageButton(
                this.leftPos + NAV_PREV_X, navY, 18, 10, PREV_PAGE_SPRITES, (button) -> {
                    changePage(Screen.hasShiftDown() ? -10 : -1);
                }));
        this.prevButton.setTooltip(Tooltip.create(
                Component.literal("Click: -1 page | Shift+Click: -10 pages")));

        // → Next (shift-click = +10)
        this.nextButton = this.addRenderableWidget(new ImageButton(
                this.leftPos + NAV_NEXT_X, navY, 18, 10, NEXT_PAGE_SPRITES, (button) -> {
                    changePage(Screen.hasShiftDown() ? 10 : 1);
                }));
        this.nextButton.setTooltip(Tooltip.create(
                Component.literal("Click: +1 page | Shift+Click: +10 pages")));

        // Page jump EditBox — between the two arrows
        this.pageJumpBox = new EditBox(this.font, this.leftPos + NAV_BOX_X, navY, NAV_BOX_W, 12, Component.empty());
        this.pageJumpBox.setMaxLength(4);
        this.pageJumpBox.setValue(String.valueOf(this.menu.getCurrentPage() + 1));
        this.pageJumpBox.setTooltip(Tooltip.create(Component.literal("Type a page number and press Enter")));
        this.addRenderableWidget(this.pageJumpBox);

        // --- Toolbar: search + sort ---
        int widgetsY = this.topPos - 18;
        this.searchBox = new EditBox(this.font, this.leftPos + 6, widgetsY, 78, 12,
                Component.translatable("gui.cobblemon-cards.binder.search"));
        this.addRenderableWidget(this.searchBox);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblemon-cards.binder.sort.pokemon"), (b) -> {
            PlatformHelper.INSTANCE.sendToServer(new SortBinderPayload(1));
        }).bounds(this.leftPos + 88, widgetsY, 40, 12).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblemon-cards.binder.sort.rarity"), (b) -> {
            PlatformHelper.INSTANCE.sendToServer(new SortBinderPayload(2));
        }).bounds(this.leftPos + 130, widgetsY, 40, 12).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblemon-cards.binder.sort.grade"), (b) -> {
            PlatformHelper.INSTANCE.sendToServer(new SortBinderPayload(3));
        }).bounds(this.leftPos + 172, widgetsY, 40, 12).build());

        this.addRenderableWidget(Button.builder(Component.translatable("gui.cobblemon-cards.binder.sort.stat"), (b) -> {
            PlatformHelper.INSTANCE.sendToServer(new SortBinderPayload(4));
        }).bounds(this.leftPos + 214, widgetsY, 40, 12).build());

        updateButtonVisibility();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.searchBox.isFocused()) {
            if (this.searchBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) return true;
        }
        if (this.pageJumpBox.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                jumpToPage();
                return true;
            }
            if (this.pageJumpBox.keyPressed(keyCode, scanCode, modifiers)) return true;
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void jumpToPage() {
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
        this.pageJumpBox.setValue(String.valueOf(newPage + 1));
        updateButtonVisibility();
    }

    private void updateButtonVisibility() {
        int page = this.menu.getCurrentPage();
        this.nextButton.visible = page < this.menu.getMaxPages() - 1;
        this.prevButton.visible = page > 0;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // "/ 1000" right of the editbox — NAV_SLASH_X is in label-space (relative to leftPos)
        guiGraphics.drawString(this.font,
                Component.literal("/ " + this.menu.getMaxPages()),
                NAV_SLASH_X, NAV_Y_OFFSET + 2, 0x555555, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.isRendering = true;
        this.renderBackground(graphics, mouseX, mouseY, delta);
        super.render(graphics, mouseX, mouseY, delta);
        this.isRendering = false;

        this.hoveredSlot = null;
        int cardSlotsCount = Math.min(CardCabinetMenu.SLOTS_PER_PAGE, this.menu.slots.size());
        for (int i = 0; i < cardSlotsCount; i++) {
            Slot slot = this.menu.slots.get(i);
            if (slot.isActive() && this.isHovering(slot.x - CARD_OFFSET_X, slot.y - CARD_OFFSET_Y, CARD_SLOT_WIDTH, CARD_SLOT_HEIGHT, mouseX, mouseY)) {
                this.hoveredSlot = slot;
                break;
            }
        }
        if (this.hoveredSlot == null) {
            for (int i = CardCabinetMenu.SLOTS_PER_PAGE; i < this.menu.slots.size(); i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.isActive() && this.isHovering(slot.x, slot.y, 16, 16, mouseX, mouseY)) {
                    this.hoveredSlot = slot;
                    break;
                }
            }
        }

        if (this.hoveredSlot != null && this.hoveredSlot.index < CardCabinetMenu.SLOTS_PER_PAGE) {
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
        if (this.isRendering) return false;
        if (width == 16 && height == 16) {
            int cardSlotsCount = Math.min(CardCabinetMenu.SLOTS_PER_PAGE, this.menu.slots.size());
            for (int i = 0; i < cardSlotsCount; i++) {
                Slot slot = this.menu.slots.get(i);
                if (slot.x == x && slot.y == y) {
                    return super.isHovering(x - CARD_OFFSET_X, y - CARD_OFFSET_Y,
                            CARD_SLOT_WIDTH, CARD_SLOT_HEIGHT, mouseX, mouseY);
                }
            }
        }
        return super.isHovering(x, y, width, height, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        graphics.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight, 512, 512);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (!slot.isActive()) return;

        if (slot.index < CardCabinetMenu.SLOTS_PER_PAGE) {
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
                    String name = Component.translatable("cobblemon.species." + data.pokemonId())
                            .getString().toLowerCase();
                    boolean matchesPokemon = name.contains(searchTerm)
                            || data.pokemonId().toLowerCase().contains(searchTerm)
                            || displayName.toLowerCase().contains(searchTerm);
                    boolean matchesStat = false;
                    if (!com.howlite.cobblemoncards.util.CardUtil.isCosmeticCard(data.pokemonId())) {
                        if (data.stat() != null) {
                            String statName = data.stat().getTranslatedName().getString().toLowerCase();
                            String statId = data.stat().getSerializedName().toLowerCase();
                            matchesStat = statName.contains(searchTerm) || statId.contains(searchTerm);
                        }
                        if (!matchesStat && data.trainerStat() != null && data.trainerStat().isPresent()) {
                            com.howlite.cobblemoncards.component.CardStat tStat = data.trainerStat().get();
                            String tStatName = tStat.getTranslatedName().getString().toLowerCase();
                            String tStatId = tStat.getSerializedName().toLowerCase();
                            matchesStat = tStatName.contains(searchTerm) || tStatId.contains(searchTerm);
                        }
                    }
                    if (!matchesPokemon && !matchesStat) {
                        matches = false;
                    }
                }

                graphics.pose().pushPose();
                graphics.pose().translate(slot.x + 8f, slot.y + 8f, 100);
                graphics.pose().scale(50f, -50f, 50f);
                Lighting.setupForFlatItems();
                Minecraft.getInstance().getItemRenderer().renderStatic(
                        stack, ItemDisplayContext.GUI,
                        LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY,
                        graphics.pose(), graphics.bufferSource(),
                        Minecraft.getInstance().level, 0);
                graphics.flush();
                graphics.pose().popPose();

                if (!matches) {
                    graphics.pose().pushPose();
                    graphics.pose().translate(0, 0, 200);
                    graphics.fill(slot.x - CARD_OFFSET_X, slot.y - CARD_OFFSET_Y,
                            slot.x - CARD_OFFSET_X + CARD_SLOT_WIDTH,
                            slot.y - CARD_OFFSET_Y + CARD_SLOT_HEIGHT, 0xAA000000);
                    graphics.pose().popPose();
                }
            }
        } else {
            super.renderSlot(graphics, slot);
        }
    }
}
