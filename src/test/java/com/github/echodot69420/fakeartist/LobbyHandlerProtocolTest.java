package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;

import java.util.List;

import static com.github.echodot69420.fakeartist.LobbyFixture.assertError;
import static com.github.echodot69420.fakeartist.LobbyFixture.payloads;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Malformed or unknown lobby WebSocket messages get an error frame and never drop the connection. */
class LobbyHandlerProtocolTest {

    @Tag("FR-14")
    @Tag("NFR-07")
    @Test
    void malformedJsonIsRejectedAndSessionStaysOpen() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(2);
        f.send(f.host(), "not json");
        assertError(f.host(), "MALFORMED_REQUEST");
        verify(f.host(), never()).close(any(CloseStatus.class));
    }

    @Tag("FR-14")
    @Tag("NFR-01")
    @Test
    void unknownTypeIsRejected() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(2);
        f.send(f.host(), "{\"type\":\"dance\"}");
        assertError(f.host(), "UNKNOWN_MESSAGE_TYPE");
    }

    @Tag("FR-14")
    @Tag("NFR-01")
    @Test
    void missingTypeIsRejected() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(2);
        f.send(f.host(), "{}");
        assertError(f.host(), "UNKNOWN_MESSAGE_TYPE");
    }

    @Tag("NFR-07")
    @Test // the connection keeps working after an error
    void connectionSurvivesAnError() throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(2);
        f.send(f.host(), "{\"type\":\"dance\"}");
        f.send(f.host(), "{\"type\":\"ping\"}");

        List<JsonNode> sent = payloads(f.host());
        assertThat(sent.get(sent.size() - 1).path("type").asText()).isEqualTo("pong");
        verify(f.host(), never()).close(any(CloseStatus.class));
    }
}
