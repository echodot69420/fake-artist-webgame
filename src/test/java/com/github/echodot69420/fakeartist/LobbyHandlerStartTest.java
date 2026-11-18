package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.github.echodot69420.fakeartist.LobbyFixture.assertError;
import static com.github.echodot69420.fakeartist.LobbyFixture.assertNoError;
import static org.assertj.core.api.Assertions.assertThat;

/** Starting round setup over the lobby WebSocket: host-only, minimum of three players. */
class LobbyHandlerStartTest {
    private static final String START = "{\"type\":\"start\"}";

    @Tag("FR-08")
    @Tag("FR-08-01")
    @Tag("FR-14")
    @Test
    void nonHostCannotStart() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(3);
        f.send(f.guest(), START);
        assertError(f.guest(), "NOT_HOST");
        assertThat(f.game().collectingTopics).isFalse();
    }

    @Tag("FR-08")
    @Tag("FR-08-02")
    @Tag("FR-14")
    @Test // just under the minimum
    void startWithTwoPlayersIsRejected() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(2);
        f.send(f.host(), START);
        assertError(f.host(), "NOT_ENOUGH_PLAYERS");
        assertThat(f.game().collectingTopics).isFalse();
    }

    @Tag("FR-08")
    @Tag("FR-08-02")
    @Tag("FR-08-03")
    @Tag("FR-14")
    @Test // minimum
    void startWithThreePlayersIsAccepted() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(3);
        f.send(f.host(), START);
        assertThat(f.game().collectingTopics).isTrue();
        assertNoError(f.host());
    }
}
