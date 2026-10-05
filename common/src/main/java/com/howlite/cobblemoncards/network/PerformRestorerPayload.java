package com.howlite.cobblemoncards.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Payload C2S : le client demande au serveur de démarrer la restauration de grade.
 */
public record PerformRestorerPayload() implements CustomPacketPayload {
    public static final Type<PerformRestorerPayload> ID = new Type<>(
            ResourceLocation.fromNamespaceAndPath("cobblemon-cards", "perform_restorer")
    );

    public static final StreamCodec<FriendlyByteBuf, PerformRestorerPayload> CODEC =
            CustomPacketPayload.codec(PerformRestorerPayload::write, PerformRestorerPayload::new);

    public PerformRestorerPayload(FriendlyByteBuf buf) {
        this();
    }

    private void write(FriendlyByteBuf buf) {
        // Pas de données à écrire
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
