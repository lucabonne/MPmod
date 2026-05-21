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
                pair[0] + ": 40",
                pair[1] + ": 100"
            ), 1).orElseThrow();

            assertEquals(-0.60D, result.delta(), 0.0001D);
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
        assertEquals(-0.0465D, result.delta(), 0.0001D);
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
}
