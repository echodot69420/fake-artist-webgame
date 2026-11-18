package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;

import java.util.Map;

import static com.github.echodot69420.fakeartist.LobbyFixture.assertError;
import static com.github.echodot69420.fakeartist.LobbyFixture.lastPayload;
import static com.github.echodot69420.fakeartist.LobbyFixture.payloads;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** Host kicking a player from the lobby (FR-16, planned; behind the kick-player feature flag). */
class LobbyHandlerKickTest {

    @Tag("FR-16")
    @Test
    void disabledKickFlagRejectsKickWithoutChangingGame() throws Exception {
        LobbyFixture f = lobby(false);

        f.send(f.host(), kick(f.guestId()));

        assertThat(f.game().players).hasSize(2);
        assertThat(lastPayload(f.host()).path("detail").asText()).isEqualTo("Player kicking is disabled.");
        assertThat(lastPayload(f.host()).path("code").asText()).isEqualTo("FEATURE_DISABLED");
    }

    @Tag("FR-16")
    @Tag("FR-16-02")
    @Test
    void nonHostCannotKickAnotherPlayer() throws Exception {
        LobbyFixture f = lobby(true);

        f.send(f.guest(), kick(f.hostId()));

        assertThat(f.game().players).hasSize(2);
        assertThat(lastPayload(f.guest()).path("detail").asText()).isEqualTo("Only the host can kick players.");
        assertThat(lastPayload(f.guest()).path("code").asText()).isEqualTo("NOT_HOST");
    }

    @Tag("FR-16")
    @Tag("FR-16-03")
    @Test
    void hostCannotKickUnknownPlayer() throws Exception {
        LobbyFixture f = lobby(true);

        f.send(f.host(), kick("missing-player"));

        assertThat(f.game().players).hasSize(2);
        assertThat(lastPayload(f.host()).path("detail").asText()).isEqualTo("Only a non-host player can be kicked.");
        assertThat(lastPayload(f.host()).path("code").asText()).isEqualTo("INVALID_KICK_TARGET");
    }

    @Tag("FR-16")
    @Tag("FR-16-03")
    @Test
    void hostCannotKickThemself() throws Exception {
        LobbyFixture f = lobby(true);

        f.send(f.host(), kick(f.hostId()));

        assertError(f.host(), "INVALID_KICK_TARGET");
        assertThat(f.game().players).hasSize(2);
    }

    @Tag("FR-16")
    @Tag("FR-16-04")
    @Tag("FR-16-05")
    @Test
    void hostKickRemovesTargetAndNotifiesBothSessions() throws Exception {
        LobbyFixture f = lobby(true);

        f.send(f.host(), kick(f.guestId()));

        assertThat(f.game().players).singleElement()
                .satisfies(player -> assertThat(player.player_id()).isEqualTo(f.hostId()));
        assertThat(payloads(f.guest()))
                .anyMatch(payload -> payload.path("type").asText().equals("bye")
                        && payload.path("reason").asText().equals("kicked by host"));
        verify(f.guest()).close(new CloseStatus(4401, "kicked by host"));
        assertThat(payloads(f.host()))
                .anyMatch(payload -> payload.path("type").asText().equals("players")
                        && payload.path("players").size() == 1);
    }

    private static LobbyFixture lobby(boolean kickEnabled) throws Exception {
        return LobbyFixture.withPlayers(2, LobbyFixture.flags(false, kickEnabled));
    }

    private static Map<String, String> kick(String target) {
        return Map.of("type", "kick", "player_id", target);
    }
}
