package com.howlite.cobblemoncards.compat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.howlite.cobblemoncards.compat.BoosterRewardHelper.Selection;
import static org.junit.jupiter.api.Assertions.*;

class BoosterRewardHelperTest {
    private record RegistryItem(String id, boolean booster) {}

    static Stream<Selection> categories() {
        return Stream.of(Selection.CLASSIC, Selection.GENERATION, Selection.THEMED,
                Selection.CLASSIC_OR_GENERATION, Selection.GENERATION_OR_THEMED);
    }

    @ParameterizedTest
    @MethodSource("categories")
    void emptyRegistryNeverProducesInventedBooster(Selection selection) {
        assertTrue(BoosterRewardHelper.available(selection, ignored -> null, ignored -> true).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("categories")
    void everyCategoryFiltersMissingEntriesNonBoostersAndRegistryAliases(Selection selection) {
        RegistryItem realBooster = new RegistryItem("registered_booster", true);
        RegistryItem ordinaryItem = new RegistryItem("ordinary_item", false);
        List<String> types = selection.boosterTypes();
        List<RegistryItem> actual = BoosterRewardHelper.available(selection, type -> {
            int index = types.indexOf(type);
            return index == 0 || index == 2 ? realBooster : index == 1 ? ordinaryItem : null;
        }, RegistryItem::booster);
        assertEquals(List.of(realBooster), actual);
        assertThrows(UnsupportedOperationException.class, () -> actual.add(ordinaryItem));
    }

    @Test
    void sparseRegistryKeepsOnlyAllowedAvailableTypesInOrder() {
        RegistryItem gen2 = new RegistryItem("gen2", true);
        RegistryItem gen9 = new RegistryItem("gen9", true);
        RegistryItem water = new RegistryItem("water", true);
        Map<String, RegistryItem> registry = Map.of("gen2", gen2, "gen9", gen9, "water", water);
        assertEquals(List.of(gen2, gen9), BoosterRewardHelper.available(
                Selection.GENERATION, registry::get, RegistryItem::booster));
        assertEquals(List.of(water), BoosterRewardHelper.available(
                Selection.THEMED, registry::get, RegistryItem::booster));
        assertTrue(BoosterRewardHelper.available(Selection.CLASSIC, registry::get, RegistryItem::booster).isEmpty());
    }

    @Test
    void duplicateTypesAndAliasesDoNotBiasSelection() {
        RegistryItem shared = new RegistryItem("one_registered_item", true);
        RegistryItem another = new RegistryItem("another_registered_item", true);
        Selection selection = new Selection(List.of("gen1", "gen1", "gen2", "water", "missing"));
        Map<String, RegistryItem> registry = Map.of("gen1", shared, "gen2", shared, "water", another);
        assertEquals(List.of(shared, another), BoosterRewardHelper.available(selection, registry::get, RegistryItem::booster));
    }

    @Test
    void tokenSelectorsOnlyReferToSupportedCategoriesAndTypes() {
        assertEquals(Selection.CLASSIC, Selection.fromToken(null));
        assertEquals(Selection.CLASSIC, Selection.fromToken("base"));
        assertEquals(Selection.CLASSIC, Selection.fromToken("classic"));
        assertEquals(Selection.GENERATION, Selection.fromToken("GENERATION"));
        assertEquals(Selection.THEMED, Selection.fromToken("THEMED"));
        for (String type : Selection.GENERATION.boosterTypes()) {
            assertEquals(List.of(type), Selection.fromToken(type.toUpperCase(java.util.Locale.ROOT)).boosterTypes());
        }
        for (String type : Selection.THEMED.boosterTypes()) {
            assertEquals(List.of(type), Selection.fromToken(type.toUpperCase(java.util.Locale.ROOT)).boosterTypes());
        }
        for (String invalid : List.of("", "gen0", "gen10", "master", "special", "not_a_booster")) {
            assertEquals(Selection.CLASSIC, Selection.fromToken(invalid));
        }
    }

    @Test
    void selectionsCannotBeChangedAfterConstruction() {
        java.util.ArrayList<String> input = new java.util.ArrayList<>(List.of("water"));
        Selection selection = new Selection(input);
        input.add("fire");
        assertEquals(List.of("water"), selection.boosterTypes());
        assertThrows(UnsupportedOperationException.class, () -> selection.boosterTypes().add("fire"));
    }
}
