package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.echodot69420.fakeartist.LobbyFixture.assertError;
import static com.github.echodot69420.fakeartist.LobbyFixture.assertNoError;
import static org.assertj.core.api.Assertions.assertThat;

/** Topic submission over the lobby WebSocket: phase check and 1-40 character boundaries (after trim). */
class LobbyHandlerTopicTest {

    @Tag("FR-09")
    @Tag("FR-09-01")
    @Tag("FR-14")
    @Test
    void topicBeforeStartIsRejected() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(3);
        f.send(f.host(), topic("Cat"));
        assertError(f.host(), "TOPIC_WINDOW_CLOSED");
    }

    @Tag("FR-09")
    @Tag("FR-09-02")
    @Tag("FR-14")
    @Test
    void emptyTopicIsRejected() throws Exception {
        LobbyFixture f = LobbyFixture.collectingTopics();
        f.send(f.host(), topic(""));
        assertError(f.host(), "INVALID_TOPIC");
        assertThat(f.game().topics).isEmpty();
    }

    @Tag("FR-09")
    @Tag("FR-09-02")
    @Tag("FR-14")
    @Test
    void whitespaceOnlyTopicIsRejected() throws Exception {
        LobbyFixture f = LobbyFixture.collectingTopics();
        f.send(f.host(), topic("   "));
        assertError(f.host(), "INVALID_TOPIC");
        assertThat(f.game().topics).isEmpty();
    }

    @Tag("FR-09")
    @Tag("FR-09-02")
    @Tag("FR-14")
    @Test // lower bound
    void oneCharacterTopicIsAccepted() throws Exception {
        LobbyFixture f = LobbyFixture.collectingTopics();
        f.send(f.host(), topic("A"));
        assertThat(f.game().topics).containsValue("A");
        assertNoError(f.host());
    }

    @Tag("FR-09")
    @Tag("FR-09-02")
    @Tag("FR-14")
    @Test // upper bound
    void fortyCharacterTopicIsAccepted() throws Exception {
        LobbyFixture f = LobbyFixture.collectingTopics();
        f.send(f.host(), topic("T".repeat(40)));
        assertThat(f.game().topics).containsValue("T".repeat(40));
        assertNoError(f.host());
    }

    @Tag("FR-09")
    @Tag("FR-09-02")
    @Tag("FR-14")
    @Test // just over the upper bound
    void fortyOneCharacterTopicIsRejected() throws Exception {
        LobbyFixture f = LobbyFixture.collectingTopics();
        f.send(f.host(), topic("T".repeat(41)));
        assertError(f.host(), "INVALID_TOPIC");
        assertThat(f.game().topics).isEmpty();
    }

    private static Map<String, String> topic(String value) {
        return Map.of("type", "topic", "value", value);
    }
}
