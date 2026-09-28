package net.minepiece.qol.parse;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ProfileXpParserTest {
    @Test
    void readsScreenshotProgressBarWithoutSlash() {
        ProfileXpParser.Snapshot snapshot = ProfileXpParser.parseTooltip(List.of(
            "Information", "Progression", "· Ascension: 0", "· Level: 63 (15.54%)",
            "47784 ▰▰▱▱▱ 307450", "Statistics", "Health +2665", "Strength +343", "Defense +266"
        )).orElseThrow();
        assertEquals(63, snapshot.level());
        assertEquals(0, snapshot.ascension());
        assertEquals(47784, snapshot.currentXp());
        assertEquals(307450, snapshot.requiredXp());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Level", "Niveau", "Nivel", "Nível", "Stufe", "Livello", "Poziom", "Tingkat", "Seviye"})
    void readsTranslatedLevelLabelsAndGroupedNumbers(String levelLabel) {
        ProfileXpParser.Snapshot snapshot = ProfileXpParser.parseTooltip(List.of(
            "Ascension: 2", levelLabel + ": 63 (15,54%)", "47,784 / 307,450"
        )).orElseThrow();
        assertEquals(2, snapshot.ascension());
        assertEquals(47784, snapshot.currentXp());
        assertEquals(307450, snapshot.requiredXp());
    }

    @Test
    void rejectsStatsPercentagesAndInconsistentAmounts() {
        assertTrue(ProfileXpParser.parseTooltip(List.of("Critical Chance +19%", "100 / 1000")).isEmpty());
        assertTrue(ProfileXpParser.parseTooltip(List.of("Level: 63 (15.54%)", "10 / 1000")).isEmpty());
        assertTrue(ProfileXpParser.parseTooltip(List.of("Level: 63 (15.54%)", "Health +2665")).isEmpty());
        assertTrue(ProfileXpParser.parseTooltip(List.of("Level: 63 (0%)", "0 / 0")).isEmpty());
        assertTrue(ProfileXpParser.parseBar("Level: 63 (150%)").isEmpty());
        assertTrue(ProfileXpParser.parseBar("XP 10% | danger 50%").isEmpty());
    }

    @Test
    void readsBossBarPercentageWithAndWithoutLevel() {
        assertEquals(new ProfileXpParser.BarProgress(63, 15.54, true),
            ProfileXpParser.parseBar("Level: 63 | XP 15.54%").orElseThrow());
        assertEquals(new ProfileXpParser.BarProgress(null, 15.54, false),
            ProfileXpParser.parseBar("15.54%").orElseThrow());
    }
}
