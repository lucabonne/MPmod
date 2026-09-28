package net.minepiece.qol.parse;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class TooltipParsersTest {
    @Test
    void parsesAuctionPriceLabelsForSupportedLanguages() {
        String[][] labels = {
            {"Selling price", "Average price"},
            {"Prix de vente", "Prix moyen"},
            {"Precio de venta", "Precio medio"},
            {"Verkaufspreis", "Durchschnittspreis"},
            {"Prezzo di vendita", "Prezzo medio"},
            {"Preço de venda", "Preço médio"},
            {"Cena sprzedaży", "Średnia cena"},
            {"Harga jual", "Harga rata-rata"},
            {"Satış fiyatı", "Ortalama fiyat"}
        };

        for (String[] pair : labels) {
            TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(List.of(
                pair[0] + ": 400",
                pair[1] + ": 1000"
            ), 10).orElseThrow();

            assertEquals(-0.60D, result.delta(), 0.0001D);
            assertEquals(40.0D, result.unitPrice());
            assertEquals(100.0D, result.averageUnitPrice());
            assertEquals(true, TooltipParsers.isAuctionAveragePriceLine("· " + pair[1] + ": 1000"));
        }
    }

    @Test
    void recognizesAuctionSellingPriceLinesForSupportedLanguages() {
        String[] labels = {
            "Selling price",
            "Prix de vente",
            "Precio de venta",
            "Verkaufspreis",
            "Prezzo di vendita",
            "Preço de venda",
            "Cena sprzedaży",
            "Harga jual",
            "Satış fiyatı"
        };

        for (String label : labels) {
            assertEquals(true, TooltipParsers.isAuctionSellingPriceLine("· " + label + ": 750k"));
        }
    }

    @Test
    void parsesSpanishAuctionTooltipWithCompactPrices() {
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(List.of(
            "· Tiempo restante: 6d 23h 25m 31s",
            "· Vendedor: Paspome",
            "· Precio de venta: 750k",
            "· Precio medio: 196.64k",
            "· Total en venta: 9406"
        ), 4).orElseThrow();

        assertEquals(750_000.0D, result.sellingPrice(), 0.0001D);
        assertEquals(196_640.0D, result.averagePrice(), 0.0001D);
        assertEquals(187_500.0D, result.unitPrice(), 0.0001D);
        assertEquals(49_160.0D, result.averageUnitPrice(), 0.0001D);
        assertEquals((750_000.0D - 196_640.0D) / 196_640.0D, result.delta(), 0.0001D);
    }

    @Test
    void comparesStackTotalsIndependentlyOfQuantity() {
        for (int quantity : new int[] {1, 10, 64}) {
            TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(List.of(
                "Selling price: 400", "Average price: 380"
            ), quantity).orElseThrow();
            assertEquals(20.0D / 380.0D, result.delta(), 0.0000001D);
            assertEquals(400.0D / quantity, result.unitPrice());
            assertEquals(380.0D / quantity, result.averageUnitPrice());
        }
    }

    @Test
    void equalTotalsHaveNoDifferenceAndQuantityDefaultsToOne() {
        TooltipParsers.AuctionParseResult result = TooltipParsers.parseAuctionHighlight(List.of(
            "Selling price: 400", "Average price: 400"
        ), 0).orElseThrow();
        assertEquals(0.0D, result.delta());
        assertEquals(0.0D, result.intensity());
        assertEquals(1, result.quantity());
        assertEquals(400.0D, result.unitPrice());
        assertEquals(400.0D, result.averageUnitPrice());
    }

    @Test
    void ignoresMissingOrInvalidAuctionPrices() {
        for (List<String> lines : List.of(
            List.of("Selling price: 400"),
            List.of("Average price: 380"),
            List.of("Selling price: 0", "Average price: 380"),
            List.of("Selling price: 400", "Average price: 0"),
            List.of("Selling price: unknown", "Average price: 380"),
            List.of("Selling price: 400", "Average price: unknown")
        )) {
            assertEquals(true, TooltipParsers.parseAuctionHighlight(lines, 10).isEmpty());
        }
    }

    @Test
    void parsesSpanishPetStatNamesForRolls() {
        List<TooltipParsers.PetStatLine> lines = TooltipParsers.parsePetRolls(List.of(
            "LEGENDARY",
            "Nivel: 184",
            "Estadísticas",
            "(LVL 1) Vida +8489",
            "(LVL 1) Fuerza +418",
            "(LVL 1) Prob. Critico +80",
            "(LVL 1) Daño Crítico +240",
            "(LVL 1) Energía +1462",
            "(LVL 1) Regen. de Energía +82",
            "(LVL 1) Velocidad +148",
            "(LVL 1) Destreza +119",
            "(LVL 1) Defensa +276",
            "(LVL 1) Regeneración +47"
        ));

        assertFalse(lines.isEmpty());
    }

    @Test
    void parsesSpanishPetStatsWithoutPerLineUnlocks() {
        List<TooltipParsers.PetStatLine> lines = TooltipParsers.parsePetRolls(List.of(
            "Ascensión: 5",
            "Nivel: 184 (85.96%)",
            "Estadísticas",
            "Vida +8.489",
            "Fuerza +418",
            "Daño +151",
            "Prob. Critico +80",
            "Daño Crítico +240",
            "Poder +271",
            "Energía +1462",
            "Regen. de Energía +82",
            "Velocidad +148",
            "Destreza +119",
            "Defensa +276",
            "Regeneración +47"
        ), null, "LEGENDARY");

        assertEquals(12, lines.size());
    }

    @Test
    void parsesSpanishFamiliarEffectsAndStopsBeforeMinionEffects() {
        List<TooltipParsers.PetStatLine> lines = TooltipParsers.parsePetRolls(List.of(
            "[Luffy] Pesadilla",
            "Información",
            "Nivel: Max",
            "Efectos del Familiar:",
            "LVL 20 Daño de Fruta de Pesadilla +25%",
            "LVL 20 Prob. Crítico +42%",
            "LVL 20 Velocidad +7.53",
            "LVL 20 Regen. de Energía +9.69",
            "Efectos de Minion:",
            "LVL 1 Destreza +8.2",
            "LEGENDARIO"
        ));

        assertEquals(3, lines.size());
        assertEquals("Critical Chance", lines.get(0).statName());
        assertEquals("Speed", lines.get(1).statName());
        assertEquals("Energy Regeneration", lines.get(2).statName());
    }

    @Test
    void parsesEverySpanishPetRollStatUnderFamiliarEffects() {
        List<TooltipParsers.PetStatLine> lines = TooltipParsers.parsePetRolls(List.of(
            "[Luffy] Pesadilla",
            "Efectos del Familiar:",
            "LVL 20 Daño de Fruta de Pesadilla +25%",
            "LVL 20 Vida +150",
            "LVL 20 Fuerza +15",
            "LVL 20 Daño +7.5",
            "LVL 20 Prob. Crítico +3.75",
            "LVL 20 Daño Crítico +7.5",
            "LVL 20 Poder +30",
            "LVL 20 Energía +150",
            "LVL 20 Regen. de Energía +7.5",
            "LVL 20 Velocidad +7.5",
            "LVL 20 Destreza +7.5",
            "LVL 20 Defensa +15",
            "LVL 20 Regeneración +7.5",
            "Efectos de Minion:",
            "LVL 1 Destreza +8.2",
            "LEGENDARIO"
        ));

        assertEquals(12, lines.size());
        assertEquals("Health", lines.get(0).statName());
        assertEquals("Strength", lines.get(1).statName());
        assertEquals("Damage", lines.get(2).statName());
        assertEquals("Critical Chance", lines.get(3).statName());
        assertEquals("Critical Damage", lines.get(4).statName());
        assertEquals("Power", lines.get(5).statName());
        assertEquals("Energy", lines.get(6).statName());
        assertEquals("Energy Regeneration", lines.get(7).statName());
        assertEquals("Speed", lines.get(8).statName());
        assertEquals("Dexterity", lines.get(9).statName());
        assertEquals("Defense", lines.get(10).statName());
        assertEquals("Regeneration", lines.get(11).statName());
    }

    @Test
    void canonicalizesSpanishPetStatsToEnglishStats() {
        assertEquals("Health", TooltipParsers.canonicalPetStatName("HP"));
        assertEquals("Health", TooltipParsers.canonicalPetStatName("Vida"));
        assertEquals("Strength", TooltipParsers.canonicalPetStatName("Fuerza"));
        assertEquals("Critical Chance", TooltipParsers.canonicalPetStatName("Prob. Critico"));
        assertEquals("Critical Damage", TooltipParsers.canonicalPetStatName("Daño Crítico"));
        assertEquals("Damage", TooltipParsers.canonicalPetStatName("Daño"));
        assertEquals("", TooltipParsers.canonicalPetStatName("Daño de Fruta de Pesadilla"));
        assertEquals("Energy Regeneration", TooltipParsers.canonicalPetStatName("Regen. de Energía"));
    }

    @Test
    void calculatesUpdatedLegendaryPetRollPercentages() {
        assertPetPercent("LEGENDARY", 10, "Power", 5.0D, 0.0D);
        assertPetPercent("LEGENDARY", 10, "Power", 7.5D, 50.0D);
        assertPetPercent("LEGENDARY", 10, "Power", 10.0D, 100.0D);
        assertPetPercent("LEGENDARY", 10, "Regeneration", 5.0D, 0.0D);
        assertPetPercent("LEGENDARY", 10, "Regeneration", 7.5D, 50.0D);
        assertPetPercent("LEGENDARY", 10, "Regeneration", 10.0D, 100.0D);
        assertPetPercent("LEGENDARY", 10, "Energy Regeneration", 5.0D, 0.0D);
        assertPetPercent("LEGENDARY", 10, "Energy Regeneration", 7.5D, 50.0D);
        assertPetPercent("LEGENDARY", 10, "Energy Regeneration", 10.0D, 100.0D);
        assertPetPercent("LEGENDARY", 10, "Critical Damage", 7.5D, 0.0D);
        assertPetPercent("LEGENDARY", 10, "Critical Damage", 11.25D, 50.0D);
        assertPetPercent("LEGENDARY", 10, "Critical Damage", 15.0D, 100.0D);
    }

    @Test
    void calculatesUpdatedMythicPetRollPercentages() {
        assertPetPercent("MYTHIC", 10, "Power", 7.5D, 0.0D);
        assertPetPercent("MYTHIC", 10, "Power", 10.0D, 50.0D);
        assertPetPercent("MYTHIC", 10, "Power", 12.5D, 100.0D);
        assertPetPercent("MYTHIC", 10, "Regeneration", 7.5D, 0.0D);
        assertPetPercent("MYTHIC", 10, "Regeneration", 10.0D, 50.0D);
        assertPetPercent("MYTHIC", 10, "Regeneration", 12.5D, 100.0D);
        assertPetPercent("MYTHIC", 10, "Energy Regeneration", 7.5D, 0.0D);
        assertPetPercent("MYTHIC", 10, "Energy Regeneration", 10.0D, 50.0D);
        assertPetPercent("MYTHIC", 10, "Energy Regeneration", 12.5D, 100.0D);
        assertPetPercent("MYTHIC", 10, "Critical Damage", 11.25D, 0.0D);
        assertPetPercent("MYTHIC", 10, "Critical Damage", 15.0D, 50.0D);
        assertPetPercent("MYTHIC", 10, "Critical Damage", 18.75D, 100.0D);
    }

    @Test
    void scalesUpdatedCriticalDamageRangeToCurrentPetLevel() {
        List<TooltipParsers.PetStatLine> lines = TooltipParsers.parsePetRolls(List.of(
            "LEGENDARY",
            "Level: 20",
            "Pet Effects:",
            "LVL 10 Critical Damage +21.41"
        ));

        assertEquals(1, lines.size());
        assertEquals(42.733333D, lines.getFirst().percent(), 0.0001D);
        assertEquals(43L, Math.round(lines.getFirst().percent()));
    }

    private static void assertPetPercent(String rarity, int level, String stat, double value, double expectedPercent) {
        List<TooltipParsers.PetStatLine> lines = TooltipParsers.parsePetRolls(List.of(
            rarity,
            "Level: " + level,
            "Pet Effects:",
            "LVL " + level + " " + stat + " +" + value
        ));

        assertEquals(1, lines.size());
        assertEquals(expectedPercent, lines.getFirst().percent(), 0.0001D);
    }
}
