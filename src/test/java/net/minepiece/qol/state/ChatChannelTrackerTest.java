package net.minepiece.qol.state;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static net.minepiece.qol.state.ChatChannelTracker.Channel.*;

class ChatChannelTrackerTest {
    @Test void followsSpanishServerConfirmations() {
        ChatChannelTracker tracker = new ChatChannelTracker();
        tracker.onServerMessage("莱 Chat de isla activado");
        assertEquals(ISLAND, tracker.channel());
        tracker.onServerMessage("瓦 Chat de isla desactivado");
        assertEquals(PUBLIC, tracker.channel());
        tracker.onServerMessage("莱 El chat del grupo ahora está activado. Tus mensajes solo se enviarán a los miembros de tu grupo.");
        assertEquals(PARTY, tracker.channel());
        tracker.onServerMessage("莱 Ahora estás siguiendo el canal english.");
        assertEquals("en", tracker.languageCode());
        assertEquals(PARTY, tracker.channel());
        tracker.onServerMessage("瓦 El chat de la partida ahora está desactivado. Tus mensajes se enviarán públicamente.");
        assertEquals(PUBLIC, tracker.channel());
        assertEquals("en", tracker.languageCode());
    }

    @Test void spanishConfirmationsHandleFormattingAndDoNotMatchPlayerQuotes() {
        ChatChannelTracker tracker = new ChatChannelTracker();
        tracker.onServerMessage("§a莱 Chat de isla activado");
        tracker.onServerMessage("§a莱 Ahora estás siguiendo el canal spanish.");
        tracker.onServerMessage("Jugador: 瓦 Chat de isla desactivado");
        tracker.onServerMessage("Jugador: 莱 Ahora estás siguiendo el canal english.");
        assertEquals(ISLAND, tracker.channel());
        assertEquals("es", tracker.languageCode());
        tracker.onServerMessage("莱 Island chat enabled");
        tracker.onServerMessage("瓦 Chat de isla desactivado");
        assertEquals(PUBLIC, tracker.channel());
    }

    @Test void followsAllServerConfirmations() {
        ChatChannelTracker tracker = new ChatChannelTracker();
        tracker.onServerMessage("莱 Island chat enabled");
        assertEquals(ISLAND, tracker.channel());
        tracker.onServerMessage("瓦 Island chat disabled");
        assertEquals(PUBLIC, tracker.channel());
        tracker.onServerMessage("莱 Party chat is now enabled. Your messages will only be sent to members of your party.");
        assertEquals(PARTY, tracker.channel());
        tracker.onServerMessage("瓦 The game chat is now disabled. Your messages will be sent publicly.");
        assertEquals(PUBLIC, tracker.channel());
    }

    @Test void languageChangesIndependentlyOfChannelAndIgnoresUnrelatedChat() {
        ChatChannelTracker tracker = new ChatChannelTracker();
        tracker.onServerMessage("莱 Island chat enabled");
        tracker.onServerMessage("§a莱 You are now following the channel english.");
        assertEquals("en", tracker.languageCode());
        tracker.onServerMessage("莱 You are now following the channel italian.");
        assertEquals("it", tracker.languageCode());
        assertEquals(ISLAND, tracker.channel());
        tracker.onServerMessage("Player: 瓦 Island chat disabled");
        tracker.onServerMessage("Player: 莱 You are now following the channel french.");
        assertEquals(ISLAND, tracker.channel());
        assertEquals("it", tracker.languageCode());
        tracker.onServerMessage("莱 You are now following the channel unknownlanguage.");
        assertEquals("", tracker.languageCode());
        assertEquals("unknownlanguage", tracker.language());
    }

    @Test void disconnectClearsStateAndDuplicateMessagesDoNotToggleIt() {
        ChatChannelTracker tracker = new ChatChannelTracker();
        assertEquals(UNKNOWN, tracker.channel());
        tracker.onServerMessage("莱 Island chat enabled");
        tracker.onServerMessage("莱 Island chat enabled");
        assertEquals(ISLAND, tracker.channel());
        tracker.onServerMessage("莱 Party chat is now enabled. Your messages will only be sent to members of your party.");
        tracker.onServerMessage("瓦 Island chat disabled");
        assertEquals(PARTY, tracker.channel());
        tracker.onServerMessage("莱 You are now following the channel italian.");
        tracker.reset();
        assertEquals(UNKNOWN, tracker.channel());
        assertEquals("", tracker.language());
    }
}
