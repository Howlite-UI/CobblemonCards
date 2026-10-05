package com.howlite.cobblemoncards.neoforge;

import com.cobblemon.mod.common.api.spawning.spawner.PlayerSpawnerFactory;
import com.howlite.cobblemoncards.CobblemonCards;
import com.howlite.cobblemoncards.CobblemonCardsConfig;
import com.howlite.cobblemoncards.block.ModBlocks;
import com.howlite.cobblemoncards.block.entity.AdvancedHoloProjectorBlockEntity;
import com.howlite.cobblemoncards.block.entity.ModBlockEntities;
import com.howlite.cobblemoncards.command.CustomBoosterCommand;
import com.howlite.cobblemoncards.command.GiveCardCommand;
import com.howlite.cobblemoncards.component.CardData;
import com.howlite.cobblemoncards.component.CardStat;
import com.howlite.cobblemoncards.component.ModDataComponents;
import com.howlite.cobblemoncards.event.BinderSpawnModifier;
import com.howlite.cobblemoncards.event.ModEvents;
import com.howlite.cobblemoncards.item.ModCreativeTabs;
import com.howlite.cobblemoncards.item.ModItems;
import com.howlite.cobblemoncards.item.custom.loot.BoosterLootTable;
import com.howlite.cobblemoncards.manager.BoosterPackManager;
import com.howlite.cobblemoncards.menu.AdvancedHoloProjectorMenu;
import com.howlite.cobblemoncards.menu.BinderMenu;
import com.howlite.cobblemoncards.menu.CardCabinetMenu;
import com.howlite.cobblemoncards.menu.CardRestorerMenu;
import com.howlite.cobblemoncards.menu.ModMenuTypes;
import com.howlite.cobblemoncards.network.*;
import com.howlite.cobblemoncards.neoforge.attachment.NeoForgePlayerDataAttachments;
import com.howlite.cobblemoncards.neoforge.client.NeoForgeCobblemonCardsClient;
import com.howlite.cobblemoncards.neoforge.client.NeoForgePacketHandlerClient;
import com.howlite.cobblemoncards.util.PlatformHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import com.howlite.cobblemoncards.event.BinderSpawnModifier;
import com.howlite.cobblemoncards.util.FakemonWhitelistReloader;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.List;

@Mod("cobblemon_cards")
@SuppressWarnings("null")
public class NeoForgeCobblemonCards {

    public NeoForgeCobblemonCards(IEventBus modEventBus) {
        // 1. Initialiser le PlatformHelper d'abord
        PlatformHelper.PlatformHelperHolder.set(new NeoForgePlatformHelper());

        // 2. Enregistrer le registre d'attachements NeoForge
        NeoForgePlayerDataAttachments.ATTACHMENT_TYPES.register(modEventBus);

        // 3. Écouter les événements du bus de mod
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onRegister);
        modEventBus.addListener(this::registerPayloadHandlers);

        // 4. Écouter les événements du bus de jeu
        NeoForge.EVENT_BUS.register(this);

        // 5. Initialiser les configurations client si nécessaire
        if (FMLEnvironment.dist.isClient()) {
            NeoForgeCobblemonCardsClient.registerClient(modEventBus);
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            // Initialisation de la configuration MidnightLib - ON L'INITIALISE ICI
            eu.midnightdust.lib.config.MidnightConfig.init(CobblemonCards.MOD_ID, CobblemonCardsConfig.class);
            // Force the config file to be (re)written so newly added entries appear on disk
            eu.midnightdust.lib.config.MidnightConfig.write(CobblemonCards.MOD_ID);

            // Binder spawn boosts: hook into Cobblemon's per-player spawner weighting pipeline.
            BinderSpawnModifier.registerSpawnInfluence();

            // Enregistrer les événements communs (ex: capture de Pokémon)
            ModEvents.registerEvents();

            // Register Accessories
            if (net.neoforged.fml.ModList.get().isLoaded("accessories")) {
                io.wispforest.accessories.api.AccessoriesAPI.registerAccessory(ModItems.LEATHER_BINDER, (io.wispforest.accessories.api.Accessory) ModItems.LEATHER_BINDER);
                io.wispforest.accessories.api.AccessoriesAPI.registerAccessory(ModItems.IRON_BINDER, (io.wispforest.accessories.api.Accessory) ModItems.IRON_BINDER);
                io.wispforest.accessories.api.AccessoriesAPI.registerAccessory(ModItems.GOLD_BINDER, (io.wispforest.accessories.api.Accessory) ModItems.GOLD_BINDER);
                io.wispforest.accessories.api.AccessoriesAPI.registerAccessory(ModItems.DIAMOND_BINDER, (io.wispforest.accessories.api.Accessory) ModItems.DIAMOND_BINDER);
                io.wispforest.accessories.api.AccessoriesAPI.registerAccessory(ModItems.NETHERITE_BINDER, (io.wispforest.accessories.api.Accessory) ModItems.NETHERITE_BINDER);
                io.wispforest.accessories.api.AccessoriesAPI.registerAccessory(ModItems.MASTER_ALBUM, (io.wispforest.accessories.api.Accessory) ModItems.MASTER_ALBUM);
            }
        });
    }

    private void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.DATA_COMPONENT_TYPE)) {
            ModDataComponents.register();
        } else if (event.getRegistryKey().equals(Registries.BLOCK)) {
            ModBlocks.register();
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            ModItems.register();
        } else if (event.getRegistryKey().equals(Registries.BLOCK_ENTITY_TYPE)) {
            ModBlockEntities.register();
        } else if (event.getRegistryKey().equals(Registries.MENU)) {
            ModMenuTypes.register();
        } else if (event.getRegistryKey().equals(Registries.CREATIVE_MODE_TAB)) {
            ModCreativeTabs.register();
        } else if (event.getRegistryKey().equals(Registries.SOUND_EVENT)) {
            com.howlite.cobblemoncards.sound.ModSounds.registerSounds();
        }
    }

    private void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(CobblemonCards.MOD_ID);

        // Enregistrement des paquets C2S (Client -> Serveur)
        registrar.playToServer(OpenBinderPayload.ID, OpenBinderPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
                com.howlite.cobblemoncards.util.BinderLocator locator = BinderMenu.findActiveBinderLocator(player);
                if (locator != null) {
                    ItemStack binderStack = locator.findItem(player);
                    if (!binderStack.isEmpty()) {
                        PlatformHelper.INSTANCE.openBinderMenu(player, locator, binderStack.getHoverName());
                    }
                }
            });
        });

        registrar.playToServer(ToggleProjectorNamePayload.ID, ToggleProjectorNamePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof AdvancedHoloProjectorMenu menu) {
                    if (menu.getContainer() instanceof AdvancedHoloProjectorBlockEntity advancedBE) {
                        advancedBE.toggleShowName();
                    }
                }
            });
        });

        registrar.playToServer(CloseBoosterPayload.ID, CloseBoosterPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                List<ItemStack> rewards = BoosterPackManager.getAndClearRewards(context.player().getUUID());
                if (rewards != null) {
                    for (ItemStack reward : rewards) {
                        if (!context.player().getInventory().add(reward)) {
                            context.player().drop(reward, false);
                        }
                    }
                    com.howlite.cobblemoncards.util.CardAdvancementManager.checkAdvancements((net.minecraft.server.level.ServerPlayer) context.player());
                }
            });
        });

        registrar.playToServer(BinderPagePayload.ID, BinderPagePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof BinderMenu menu) {
                    menu.setPage(payload.page());
                } else if (context.player().containerMenu instanceof CardCabinetMenu menu) {
                    menu.setPage(payload.page());
                }
            });
        });

        registrar.playToServer(SortBinderPayload.ID, SortBinderPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof BinderMenu menu) {
                    menu.sort(payload.sortMode());
                } else if (context.player().containerMenu instanceof CardCabinetMenu menu) {
                    menu.sort(payload.sortMode());
                }
            });
        });

        registrar.playToServer(GenerateCardPayload.ID, GenerateCardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
                if (!player.hasPermissions(2)) {
                    player.sendSystemMessage(Component.translatable("message.cobblemon-cards.workshop.no_permission"));
                    return;
                }

                ItemStack cardStack = new ItemStack(ModItems.CARD);

                CardStat stat = CardStat.MOVEMENT_SPEED;
                for (CardStat s : CardStat.values()) {
                    if (s.getSerializedName().equalsIgnoreCase(payload.statName())) {
                        stat = s;
                        break;
                    }
                }

                java.util.Optional<String> background = payload.background().equals("none") ? java.util.Optional.empty() : java.util.Optional.of(payload.background());
                java.util.Optional<String> effect = payload.effect().equals("none") ? java.util.Optional.empty() : java.util.Optional.of(payload.effect());

                if (effect.isPresent() && background.isEmpty()) {
                    background = java.util.Optional.of(com.howlite.cobblemoncards.util.CardUtil.getDefaultBackground(payload.pokemonId()));
                }

                CardData data = new CardData(
                        payload.pokemonId(),
                        payload.isShiny(),
                        payload.rarity(),
                        stat,
                        payload.statValue(),
                        0,
                        background,
                        effect
                );
                cardStack.set(ModDataComponents.CARD_DATA, data);

                if (!player.getInventory().add(cardStack)) {
                    player.drop(cardStack, false);
                }
                com.howlite.cobblemoncards.util.CardAdvancementManager.checkAdvancements(player);
                player.sendSystemMessage(Component.translatable("message.cobblemon-cards.workshop.success"));
            });
        });

        // Enregistrement des paquets S2C (Serveur -> Client)
        registrar.playToClient(OpenBoosterPayload.ID, OpenBoosterPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleOpenBooster(payload));
        });

        registrar.playToClient(GiveRewardPayload.ID, GiveRewardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleGiveReward(payload));
        });

        registrar.playToClient(OpenWorkshopPayload.ID, OpenWorkshopPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleOpenWorkshop(payload));
        });

        registrar.playToClient(SyncDiscoveredCardsPayload.ID, SyncDiscoveredCardsPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleSyncDiscoveredCards(payload));
        });

        registrar.playToClient(RenderCardPayload.ID, RenderCardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleRenderCard(payload));
        });

        registrar.playToServer(com.howlite.cobblemoncards.network.CloseInspectPayload.ID,
                com.howlite.cobblemoncards.network.CloseInspectPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                net.minecraft.server.level.ServerPlayer player = (net.minecraft.server.level.ServerPlayer) context.player();
                if (player != null) {
                    com.howlite.cobblemoncards.network.StopShowCardPayload stopPayload =
                            new com.howlite.cobblemoncards.network.StopShowCardPayload(player.getUUID());
                    player.serverLevel().players().stream()
                            .filter(p -> p != player && p.distanceTo(player) <= 16f)
                            .forEach(p -> PlatformHelper.INSTANCE.sendToPlayer(p, stopPayload));
                }
            });
        });

        registrar.playToClient(com.howlite.cobblemoncards.network.InspectCardPayload.ID,
                com.howlite.cobblemoncards.network.InspectCardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleInspectCard(payload));
        });

        registrar.playToClient(com.howlite.cobblemoncards.network.ShowCardPayload.ID,
                com.howlite.cobblemoncards.network.ShowCardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleShowCard(payload));
        });

        registrar.playToClient(com.howlite.cobblemoncards.network.StopShowCardPayload.ID,
                com.howlite.cobblemoncards.network.StopShowCardPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> NeoForgePacketHandlerClient.handleStopShowCard(payload));
        });

        registrar.playToServer(ChangeRestorerTargetGradePayload.ID, ChangeRestorerTargetGradePayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof CardRestorerMenu menu) {
                    if (menu.getBlockEntity() != null) {
                        if (menu.stillValid(context.player())) {
                            menu.getBlockEntity().setTargetGrade(payload.targetGrade());
                        }
                    }
                }
            });
        });

        registrar.playToServer(PerformRestorerPayload.ID, PerformRestorerPayload.CODEC, (payload, context) -> {
            context.enqueueWork(() -> {
                if (context.player().containerMenu instanceof CardRestorerMenu menu) {
                    if (menu.getBlockEntity() != null) {
                        menu.getBlockEntity().startRestore(context.player());
                    }
                }
            });
        });
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        GiveCardCommand.register(event.getDispatcher());
        CustomBoosterCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onWanderingTraderTrades(WandererTradesEvent event) {
        VillagerTrades.ItemListing cardTrade = new VillagerTrades.ItemListing() {
            @Override
            public MerchantOffer getOffer(Entity entity, RandomSource random) {
                int roll = random.nextInt(100);
                String rarity;
                if (roll < 1) rarity = "legendary";
                else if (roll < 5) rarity = "epic";
                else if (roll < 20) rarity = "rare";
                else if (roll < 50) rarity = "uncommon";
                else rarity = "common";

                ItemStack cardStack = BoosterLootTable.getRandomReward(rarity);

                ItemCost cost;
                int quantity;
                if (rarity.equals("common")) {
                    quantity = 10 + random.nextInt(6);
                    cost = new ItemCost(ModItems.CARD_DUST, quantity);
                } else if (rarity.equals("uncommon")) {
                    quantity = 30 + random.nextInt(11);
                    cost = new ItemCost(ModItems.CARD_DUST, quantity);
                } else if (rarity.equals("rare")) {
                    quantity = 1 + random.nextInt(2);
                    cost = new ItemCost(ModItems.CARD_DUST_POUCH, quantity);
                } else if (rarity.equals("epic")) {
                    quantity = 4 + random.nextInt(2);
                    cost = new ItemCost(ModItems.CARD_DUST_POUCH, quantity);
                } else {
                    cost = new ItemCost(ModBlocks.CARD_DUST_SACK.asItem(), 1);
                }

                return new MerchantOffer(cost, cardStack, 1, 5, 0.05F);
            }
        };

        for (int i = 0; i < 5; i++) {
            event.getGenericTrades().add(cardTrade);
        }
        for (int i = 0; i < 2; i++) {
            event.getRareTrades().add(cardTrade);
        }
    }

    @SubscribeEvent
    public void onLootTableLoad(LootTableLoadEvent event) {
        if (CobblemonCardsConfig.enableBoosterChestSpawn && event.getName() != null && isChestLootTable(event.getName())) {
            float chance = CobblemonCardsConfig.boosterChestSpawnChance / 100.0f;
            if (chance > 0.0f) {
                LootPool pool = LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK).setWeight(10))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN1).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN2).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN3).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN4).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN5).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN6).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN7).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN8).setWeight(3))
                        .add(LootItem.lootTableItem(ModItems.BOOSTER_PACK_GEN9).setWeight(3))
                        .when(LootItemRandomChanceCondition.randomChance(chance))
                        .build();
                event.getTable().addPool(pool);
            }
        }
    }


    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        // Register the Fakemon whitelist datapack reload listener.
        // Fires on server start and on every /reload command.
        event.addListener((ResourceManagerReloadListener) FakemonWhitelistReloader::loadFrom);
    }

    private static boolean isChestLootTable(ResourceLocation location) {
        if (location == null) return false;
        String path = location.getPath().toLowerCase(java.util.Locale.ROOT);
        return path.startsWith("chests/") || path.contains("/chests/") || path.endsWith("_chest") || path.contains("chest");
    }
}
