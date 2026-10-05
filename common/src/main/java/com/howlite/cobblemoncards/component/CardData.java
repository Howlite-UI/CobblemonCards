package com.howlite.cobblemoncards.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;

// Ce Record est la "carte d'identité" de notre item
public record CardData(
        String pokemonId,
        boolean isShiny,
        String rarity,
        CardStat stat,
        float statValue,
        int grade,
        Optional<String> background,
        Optional<String> effect,
        Optional<CardStat> trainerStat,
        Optional<Float> trainerStatValue
) {

    // Constructeur rétrocompatible pour les cartes sans stat trainer
    public CardData(String pokemonId, boolean isShiny, String rarity, CardStat stat, float statValue, int grade, Optional<String> background, Optional<String> effect) {
        this(pokemonId, isShiny, rarity, stat, statValue, grade, background, effect, Optional.empty(), Optional.empty());
    }

    // Ce CODEC est le traducteur qui permet à Minecraft de sauvegarder
    // ces infos dans le monde ou de les envoyer par réseau !
    public static final Codec<CardData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("pokemon_id").forGetter(CardData::pokemonId),
            Codec.BOOL.fieldOf("is_shiny").forGetter(CardData::isShiny),
            Codec.STRING.fieldOf("rarity").forGetter(CardData::rarity),
            CardStat.CODEC.fieldOf("stat").forGetter(CardData::stat),
            Codec.FLOAT.fieldOf("stat_value").forGetter(CardData::statValue),
            Codec.INT.fieldOf("grade").forGetter(CardData::grade),
            Codec.STRING.optionalFieldOf("background").forGetter(CardData::background),
            Codec.STRING.optionalFieldOf("effect").forGetter(CardData::effect),
            CardStat.CODEC.optionalFieldOf("trainer_stat").forGetter(CardData::trainerStat),
            Codec.FLOAT.optionalFieldOf("trainer_stat_value").forGetter(CardData::trainerStatValue)
    ).apply(instance, CardData::new));

    // Le traducteur pour envoyer la carte du Serveur vers l'Écran (le Client)
    public static final StreamCodec<RegistryFriendlyByteBuf, CardData> STREAM_CODEC = StreamCodec.of(
            (buf, data) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, data.pokemonId());
                ByteBufCodecs.BOOL.encode(buf, data.isShiny());
                ByteBufCodecs.STRING_UTF8.encode(buf, data.rarity());
                ByteBufCodecs.fromCodec(CardStat.CODEC).encode(buf, data.stat());
                ByteBufCodecs.FLOAT.encode(buf, data.statValue());
                ByteBufCodecs.VAR_INT.encode(buf, data.grade());
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, data.background());
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).encode(buf, data.effect());
                ByteBufCodecs.optional(ByteBufCodecs.fromCodec(CardStat.CODEC)).encode(buf, data.trainerStat());
                ByteBufCodecs.optional(ByteBufCodecs.FLOAT).encode(buf, data.trainerStatValue());
            },
            buf -> new CardData(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.fromCodec(CardStat.CODEC).decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.fromCodec(CardStat.CODEC)).decode(buf),
                    ByteBufCodecs.optional(ByteBufCodecs.FLOAT).decode(buf)
            )
    );

    // Petite méthode pratique pour récupérer la bonne texture du frame
    public String getFrameTexture() {
        return "cobblemon-cards:item/cards/frames/" + this.rarity;
    }

    // Petite méthode pratique pour récupérer la bonne texture du Pokémon
    public String getPokemonTexture() {
        String folder = this.isShiny ? "shiny" : "regular";
        return "cobblemon-cards:item/cards/pokemon/" + folder + "/" + this.pokemonId;
    }
}