package net.minepiece.qol.state;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AuctionPriceMemoryTest {
    private final Map<String, List<AuctionPriceMemory.Day>> data = new LinkedHashMap<>();
    private int saves;
    private final AuctionPriceMemory memory = new AuctionPriceMemory(data, () -> saves++);
    private static final LocalDate MON = LocalDate.of(2026, 11, 16);

    @Test
    void keepsTheWeeksLowestAndOnlySavesOnChanges() {
        memory.observe("sword", 120, 100, MON);
        memory.observe("sword", 120, 100, MON);        // same tooltip drawn again
        assertEquals(1, saves);
        memory.observe("sword", 90, 100, MON.plusDays(2));
        memory.observe("sword", 95, 100, MON.plusDays(3));
        var low = memory.lowest("sword", MON.plusDays(3)).orElseThrow();
        assertEquals(90, low.price(), 1e-9);
        assertEquals(1, low.daysAgo());
    }

    @Test
    void forgetsLowsOlderThanAWeek() {
        memory.observe("sword", 50, 100, MON);
        memory.observe("sword", 80, 100, MON.plusDays(8));
        assertEquals(80, memory.lowest("sword", MON.plusDays(8)).orElseThrow().price(), 1e-9);
        assertTrue(memory.lowest("shield", MON).isEmpty());
    }

    @Test
    void trendComparesTheWeeksOldestAndNewestAverage() {
        memory.observe("sword", 100, 100, MON);
        assertTrue(memory.weeklyTrend("sword", MON).isEmpty());   // one day is no trend
        memory.observe("sword", 100, 112, MON.plusDays(4));
        assertEquals(0.12, memory.weeklyTrend("sword", MON.plusDays(4)).orElseThrow(), 1e-9);
    }

    @Test
    void ignoresBrokenPrices() {
        memory.observe("sword", Double.NaN, 100, MON);
        memory.observe("sword", 0, 100, MON);
        memory.observe("", 10, 100, MON);
        assertEquals(0, saves);
    }
}
