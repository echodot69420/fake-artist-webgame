package com.github.echodot69420.fakeartist;

import com.github.echodot69420.fakeartist.errors.WebSocketErrorHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Opening a lobby WebSocket: the server rejects unknown games and non-member players. */
class LobbyHandlerConnectionTest {
    private final ObjectMapper mapper = new ObjectMapper();

    // FR-07-01, FR-07-02: a socket for a missing game is closed with a reason.
    @Tag("FR-07")
    @Tag("FR-07-01")
    @Tag("FR-07-02")
    @Test
    void unknownGameConnectionReceivesActionableBye() throws Exception {
        GameController controller = mock(GameController.class);
        when(controller.getGameIfPresent("ABCD")).thenReturn(null);
        WebSocketSession session = LobbyFixture.session("ABCD", "player-1");

        newHandler(controller).afterConnectionEstablished(session);

        JsonNode payload = sentPayload(session);
        assertThat(payload.path("type").asText()).isEqualTo("bye");
        assertThat(payload.path("reason").asText()).isEqualTo("game not found");
        assertThat(payload.path("game_exists").asBoolean()).isFalse();
        verify(session).close(new CloseStatus(4404, "game not found"));
    }

    // FR-07-02, NFR-01: a socket for a non-member leaves the game unchanged.
    @Tag("FR-07")
    @Tag("FR-07-02")
    @Tag("NFR-01")
    @Test
    void unknownPlayerConnectionDoesNotMutateGameState() throws Exception {
        Game game = new Game("ABCD", "Space", "Host");
        int playerCountBeforeConnection = game.players.size();
        GameController controller = mock(GameController.class);
        when(controller.getGameIfPresent("ABCD")).thenReturn(game);
        WebSocketSession session = LobbyFixture.session("ABCD", "missing-player");

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

    private JsonNode sentPayload(WebSocketSession session) throws Exception {
        var message = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(message.capture());
        return mapper.readTree(message.getValue().getPayload());
    }
}
