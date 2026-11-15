package net.minepiece.qol.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PictureRotationTest {
    private static final long SECOND = 1_000_000_000L;

    @Test void holdsUntilIntervalAndNeverRepeatsAdjacentImages() {
        var rotation = new PictureRotation(new Random(42));
        var files = List.of("one.png", "two.png", "three.png");
        long now = 100;
        String current = rotation.current(files, 10, now);
        assertTrue(files.contains(current));
        assertEquals(current, rotation.current(files, 10, now + 10 * SECOND - 1));
        var seen = new HashSet<String>();
        seen.add(current);
        for (int i = 0; i < 30; i++) {
            now += 10 * SECOND;
            String next = rotation.current(files, 10, now);
            assertTrue(files.contains(next));
            assertNotEquals(current, next);
            seen.add(next);
            current = next;
        }
        assertEquals(3, seen.size());
    }

    @Test void changingIntervalStartsANewCountdownWithoutChangingTheImage() {
        var rotation = new PictureRotation(new Random(42));
        var files = List.of("one.png", "two.png");
        String first = rotation.current(files, 10, 0);
        assertEquals(first, rotation.current(files, 3, 8 * SECOND));
        assertEquals(first, rotation.current(files, 3, 11 * SECOND - 1));
        assertNotEquals(first, rotation.current(files, 3, 11 * SECOND));
    }

    @Test void newFileSelectionUsesOnlyTheNewImagesAndResetsTheCountdown() {
        var rotation = new PictureRotation(new Random(42));
        rotation.current(List.of("one.png", "two.png"), 10, 0);
        var replacement = List.of("three.png", "four.png");
        String current = rotation.current(replacement, 10, 20 * SECOND);
        assertTrue(replacement.contains(current));
        assertEquals(current, rotation.current(replacement, 10, 30 * SECOND - 1));
        assertNotEquals(current, rotation.current(replacement, 10, 30 * SECOND));
    }

    @Test void emptyAndSingleImageSelectionsRemainStable() {
        var rotation = new PictureRotation(new Random(42));
        assertNull(rotation.current(List.of(), 10, 0));
        assertEquals("one.png", rotation.current(List.of("one.png"), 1, 0));
        assertEquals("one.png", rotation.current(List.of("one.png"), 1, 100 * SECOND));
        assertNull(rotation.current(List.of(), 10, 101 * SECOND));
    }

    @Test void randomSelectionCanStartAtAnyImage() {
        var files = List.of("one.png", "two.png", "three.png");
        var starts = new HashSet<String>();
        for (int seed = 0; seed < 50; seed++) {
            starts.add(new PictureRotation(new Random(seed)).current(files, 10, 0));
        }
        assertEquals(new HashSet<>(files), starts);
    }
}
