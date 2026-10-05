package com.howlite.cobblemoncards.component;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.api.pokemon.stats.Stat;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

public enum CardStat implements StringRepresentable {
    // Stats Utilitaires / Joueur
    MINING_SPEED("mining_speed"),
    MOVEMENT_SPEED("movement_speed"),
    ATTACK_DAMAGE("attack_damage"),
    ATTACK_SPEED("attack_speed"),
    LUCK("luck"),
    ARMOR("armor"),
    MAX_HEALTH("max_health"),
    CARD_DROP_CHANCE("card_drop_chance"),

    // Stats de Spawn Cobblemon (Boost de rencontre)
    NORMAL_SPAWN("normal_spawn"),
    FIRE_SPAWN("fire_spawn"),
    WATER_SPAWN("water_spawn"),
    GRASS_SPAWN("grass_spawn"),
    ELECTRIC_SPAWN("electric_spawn"),
    ICE_SPAWN("ice_spawn"),
    FIGHTING_SPAWN("fighting_spawn"),
    POISON_SPAWN("poison_spawn"),
    GROUND_SPAWN("ground_spawn"),
    FLYING_SPAWN("flying_spawn"),
    PSYCHIC_SPAWN("psychic_spawn"),
    BUG_SPAWN("bug_spawn"),
    ROCK_SPAWN("rock_spawn"),
    GHOST_SPAWN("ghost_spawn"),
    DRAGON_SPAWN("dragon_spawn"),
    STEEL_SPAWN("steel_spawn"),
    FAIRY_SPAWN("fairy_spawn"),
    DARK_SPAWN("dark_spawn"),

    // Stats Dresseur (boosts globaux du joueur)
    // Ces stats sont volontairement absentes du tirage normal : elles s'obtiennent uniquement
    // via un grade >= 9 ou la petite chance sur les cartes Legendary / Mythic / Shiny.
    EXP_BOOST("exp_boost"),
    CATCH_BOOST("catch_boost"),
    SHINY_CHANCE("shiny_chance"),

    // Influences de spawn par groupe d'oeuf (suffixe _egg, 16 au total)
    MONSTER_EGG("monster_egg"),
    WATER_1_EGG("water_1_egg"),
    BUG_EGG("bug_egg"),
    FLYING_EGG("flying_egg"),
    FIELD_EGG("field_egg"),
    FAIRY_EGG("fairy_egg"),
    GRASS_EGG("grass_egg"),
    HUMAN_LIKE_EGG("human_like_egg"),
    WATER_3_EGG("water_3_egg"),
    MINERAL_EGG("mineral_egg"),
    AMORPHOUS_EGG("amorphous_egg"),
    WATER_2_EGG("water_2_egg"),
    DITTO_EGG("ditto_egg"),
    DRAGON_EGG("dragon_egg"),
    UNDISCOVERED_EGG("undiscovered_egg"),

    // Influences de spawn par rendement d'EV (suffixe _yield, 6 au total)
    HP_YIELD("hp_yield"),
    ATTACK_YIELD("attack_yield"),
    DEFENCE_YIELD("defence_yield"),
    SPECIAL_ATTACK_YIELD("special_attack_yield"),
    SPECIAL_DEFENCE_YIELD("special_defence_yield"),
    SPEED_YIELD("speed_yield");

    public static final Codec<CardStat> CODEC = StringRepresentable.fromEnum(CardStat::values);

    private final String name;

    CardStat(String name) {
        this.name = name;
    }

    public MutableComponent getTranslatedName() {
        return Component.translatable("stat.cobblemon-cards." + this.name);
    }

    @Override
    public String getSerializedName() {
        return name;
    }

    /**
     * Resolves the Cobblemon {@link EggGroup} boosted by an {@code *_egg} stat.
     * Kept next to the constants so the stat -> Cobblemon mapping lives in one place, mirroring
     * how {@code BinderSpawnModifier} derives {@code ElementalTypes.get(...)} from the serialized name.
     *
     * @return the matching egg group, or {@code null} if this stat is not an egg-group stat.
     */
    public EggGroup getEggGroup() {
        return switch (this) {
            case MONSTER_EGG -> EggGroup.MONSTER;
            case WATER_1_EGG -> EggGroup.WATER_1;
            case BUG_EGG -> EggGroup.BUG;
            case FLYING_EGG -> EggGroup.FLYING;
            case FIELD_EGG -> EggGroup.FIELD;
            case FAIRY_EGG -> EggGroup.FAIRY;
            case GRASS_EGG -> EggGroup.GRASS;
            case HUMAN_LIKE_EGG -> EggGroup.HUMAN_LIKE;
            case WATER_3_EGG -> EggGroup.WATER_3;
            case MINERAL_EGG -> EggGroup.MINERAL;
            case AMORPHOUS_EGG -> EggGroup.AMORPHOUS;
            case WATER_2_EGG -> EggGroup.WATER_2;
            case DITTO_EGG -> EggGroup.DITTO;
            case DRAGON_EGG -> EggGroup.DRAGON;
            case UNDISCOVERED_EGG -> EggGroup.UNDISCOVERED;
            default -> null;
        };
    }

    /**
     * Resolves the Cobblemon {@link Stat} whose EV yield is boosted by a {@code *_yield} stat.
     *
     * @return the matching stat, or {@code null} if this stat is not an EV-yield stat.
     */
    public Stat getEvYieldStat() {
        return switch (this) {
            case HP_YIELD -> Stats.HP;
            case ATTACK_YIELD -> Stats.ATTACK;
            case DEFENCE_YIELD -> Stats.DEFENCE;
            case SPECIAL_ATTACK_YIELD -> Stats.SPECIAL_ATTACK;
            case SPECIAL_DEFENCE_YIELD -> Stats.SPECIAL_DEFENCE;
            case SPEED_YIELD -> Stats.SPEED;
            default -> null;
        };
    }
}