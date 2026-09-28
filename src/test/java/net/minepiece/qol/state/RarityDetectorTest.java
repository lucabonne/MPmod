package net.minepiece.qol.state;

import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RarityDetectorTest {
    @Test void primordialIsRecognizedWithoutConfusingOtherRarities() throws Exception {
        Method detect = RarityDetector.class.getDeclaredMethod("findSingleRarity", List.class);
        detect.setAccessible(true);
        assertEquals(RarityDetector.Rarity.PRIMORDIAL, detect.invoke(null, List.of(Text.literal("^ Primordial"))));
        assertEquals(RarityDetector.Rarity.EPIC, detect.invoke(null, List.of(Text.literal("恨繁"))));
        assertEquals(RarityDetector.Rarity.MYTHIC, detect.invoke(null, List.of(Text.literal("愈潮"))));
        assertNull(detect.invoke(null, List.of(Text.literal("^ Primordial"), Text.literal("^ Mythic"))));
    }
}
