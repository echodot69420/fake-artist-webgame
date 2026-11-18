package com.github.echodot69420.fakeartist;

import com.github.echodot69420.fakeartist.errors.WebSocketErrorHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LobbyHandlerConnectionTests {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void unknownGameConnectionReceivesActionableBye() throws Exception {
        GameController controller = mock(GameController.class);
        when(controller.getGameIfPresent("ABCD")).thenReturn(null);
        WebSocketSession session = session("ABCD", "player-1");

        newHandler(controller).afterConnectionEstablished(session);

        JsonNode payload = sentPayload(session);
        assertThat(payload.path("type").asText()).isEqualTo("bye");
        assertThat(payload.path("reason").asText()).isEqualTo("game not found");
        assertThat(payload.path("game_exists").asBoolean()).isFalse();
        verify(session).close(new CloseStatus(4404, "game not found"));
    }

    @Test
    void unknownPlayerConnectionDoesNotMutateGameState() throws Exception {
        Game game = new Game("ABCD", "Space", "Host");
        int playerCountBeforeConnection = game.players.size();
        GameController controller = mock(GameController.class);
        when(controller.getGameIfPresent("ABCD")).thenReturn(game);
        WebSocketSession session = session("ABCD", "missing-player");

        newHandler(controller).afterConnectionEstablished(session);

        assertThat(game.players).hasSize(playerCountBeforeConnection)
                .extracting(GamePlayer::username)
                .containsExactly("Host");
        JsonNode payload = sentPayload(session);
        assertThat(payload.path("type").asText()).isEqualTo("bye");
        assertThat(payload.path("reason").asText()).isEqualTo("player not in this game");
        assertThat(payload.path("game_exists").asBoolean()).isFalse();
        verify(session).close(new CloseStatus(4404, "player not in this game"));
    }

    private LobbyHandler newHandler(GameController controller) {
        FeatureFlags flags = new FeatureFlags();
        return new LobbyHandler(mapper, controller, flags, new WebSocketErrorHandler(mapper));
    }

    private WebSocketSession session(String code, String playerId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create(
                "ws://localhost/ws/lobby?code=" + code + "&player_id=" + playerId));
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private JsonNode sentPayload(WebSocketSession session) throws Exception {
        var message = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(message.capture());
        return mapper.readTree(message.getValue().getPayload());
    }
}
