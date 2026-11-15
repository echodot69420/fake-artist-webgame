package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LobbyHandlerKickTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void disabledKickFlagRejectsKickWithoutChangingGame() throws Exception {
        Fixture fixture = fixture(false);

        sendKick(fixture, fixture.hostSession(), fixture.guestId());

        assertThat(fixture.game().players).hasSize(2);
        assertThat(lastPayload(fixture.hostSession()).path("detail").asText())
                .isEqualTo("Player kicking is disabled.");
    }

    @Test
    void nonHostCannotKickAnotherPlayer() throws Exception {
        Fixture fixture = fixture(true);

        sendKick(fixture, fixture.guestSession(), fixture.hostId());

        assertThat(fixture.game().players).hasSize(2);
        assertThat(lastPayload(fixture.guestSession()).path("detail").asText())
                .isEqualTo("Only the host can kick players.");
    }

    @Test
    void hostCannotKickUnknownOrHostTarget() throws Exception {
        Fixture fixture = fixture(true);

        sendKick(fixture, fixture.hostSession(), "missing-player");

        assertThat(fixture.game().players).hasSize(2);
        assertThat(lastPayload(fixture.hostSession()).path("detail").asText())
                .isEqualTo("Only a non-host player can be kicked.");
    }

    @Test
    void enabledHostKickRemovesTargetAndNotifiesBothSessions() throws Exception {
        Fixture fixture = fixture(true);

        sendKick(fixture, fixture.hostSession(), fixture.guestId());

        assertThat(fixture.game().players).singleElement()
                .satisfies(player -> assertThat(player.player_id()).isEqualTo(fixture.hostId()));
        assertThat(payloads(fixture.guestSession()))
                .anyMatch(payload -> payload.path("type").asText().equals("bye")
                        && payload.path("reason").asText().equals("kicked by host"));
        verify(fixture.guestSession()).close(new CloseStatus(4401, "kicked by host"));
        assertThat(payloads(fixture.hostSession()))
                .anyMatch(payload -> payload.path("type").asText().equals("players")
                        && payload.path("players").size() == 1);
    }

    private Fixture fixture(boolean kickEnabled) throws Exception {
        ObjectProvider<LobbyHandler> provider = mock(ObjectProvider.class);
        GameController controller = new GameController(provider);
        Map<String, Object> hosted = controller.host(new HostRequest("Host", "Space"));
        String code = (String) hosted.get("code");
        Game game = controller.getGame(code);
        game.players.add(new GamePlayer("guest-id", "Guest", false));

        FeatureFlags flags = new FeatureFlags();
        flags.setKickPlayer(kickEnabled);
        LobbyHandler handler = new LobbyHandler(mapper, controller, flags);
        WebSocketSession hostSession = session(code, game.players.get(0).player_id());
        WebSocketSession guestSession = session(code, "guest-id");
        handler.afterConnectionEstablished(hostSession);
        handler.afterConnectionEstablished(guestSession);
        clearInvocations(hostSession, guestSession);
        return new Fixture(handler, game, hostSession, guestSession,
                game.players.get(0).player_id(), "guest-id");
    }

    private void sendKick(Fixture fixture, WebSocketSession session, String targetId) {
        fixture.handler().handleTextMessage(session,
                new TextMessage("{\"type\":\"kick\",\"player_id\":\"" + targetId + "\"}"));
    }

    private WebSocketSession session(String code, String playerId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create(
                "ws://localhost/ws/lobby?code=" + code + "&player_id=" + playerId));
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private JsonNode lastPayload(WebSocketSession session) throws Exception {
        List<JsonNode> sent = payloads(session);
        return sent.get(sent.size() - 1);
    }

    private List<JsonNode> payloads(WebSocketSession session) throws Exception {
        var messages = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session, org.mockito.Mockito.atLeastOnce()).sendMessage(messages.capture());
        return messages.getAllValues().stream()
                .map(message -> read(message.getPayload()))
                .toList();
    }

    private JsonNode read(String payload) {
        try {
            return mapper.readTree(payload);
        } catch (Exception exception) {
            throw new AssertionError("Expected JSON WebSocket payload", exception);
        }
    }

    private record Fixture(LobbyHandler handler, Game game, WebSocketSession hostSession,
                           WebSocketSession guestSession, String hostId, String guestId) {}
}
