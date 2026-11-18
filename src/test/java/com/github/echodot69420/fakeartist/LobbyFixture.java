package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.echodot69420.fakeartist.errors.WebSocketErrorHandler;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Shared test fixture for {@link LobbyHandler} tests: a real {@link GameController} with one hosted game,
 * a handler wired to it, and one mocked, connected WebSocket session per player.
 * Session invocations are cleared after setup so tests only see messages caused by their own actions.
 */
record LobbyFixture(LobbyHandler handler, GameController controller, Game game, List<WebSocketSession> sessions) {
    static final ObjectMapper MAPPER = new ObjectMapper();

    /** A lobby with {@code players} players (the first is host) and all feature flags off. */
    static LobbyFixture withPlayers(int players) throws Exception {
        return withPlayers(players, new FeatureFlags());
    }

    static LobbyFixture withPlayers(int players, FeatureFlags flags) throws Exception {
        ObjectProvider<LobbyHandler> provider = mock(ObjectProvider.class);
        GameController controller = new GameController(provider);
        String code = (String) controller.host(new HostRequest("Host", "Space")).get("code");
        Game game = controller.getGame(code);
        for (int i = 2; i <= players; i++) game.players.add(new GamePlayer("p" + i, "Player" + i, false));

        LobbyHandler handler = new LobbyHandler(MAPPER, controller, flags, new WebSocketErrorHandler(MAPPER));
        List<WebSocketSession> sessions = new ArrayList<>();
        for (GamePlayer player : game.players) {
            WebSocketSession session = session(code, player.player_id());
            handler.afterConnectionEstablished(session);
            sessions.add(session);
        }
        sessions.forEach(session -> clearInvocations(session));
        return new LobbyFixture(handler, controller, game, sessions);
    }

    static FeatureFlags flags(boolean roundResults, boolean kickPlayer) {
        FeatureFlags flags = new FeatureFlags();
        flags.setRoundResults(roundResults);
        flags.setKickPlayer(kickPlayer);
        return flags;
    }

    /** Starts topic collection with three players and clears the start messages. */
    static LobbyFixture collectingTopics() throws Exception {
        LobbyFixture f = withPlayers(3);
        f.send(f.host(), "{\"type\":\"start\"}");
        assertThat(f.game().collectingTopics).isTrue();
        f.sessions().forEach(session -> clearInvocations(session));
        return f;
    }

    WebSocketSession host() { return sessions.get(0); }
    WebSocketSession guest() { return sessions.get(1); }
    String hostId() { return game.players.get(0).player_id(); }
    String guestId() { return game.players.get(1).player_id(); }

    void send(WebSocketSession session, String payload) {
        handler.handleTextMessage(session, new TextMessage(payload));
    }

    void send(WebSocketSession session, Map<String, String> message) throws Exception {
        send(session, MAPPER.writeValueAsString(message));
    }

    /** Payloads from every session, in session order. */
    List<JsonNode> allPayloads() throws Exception {
        List<JsonNode> result = new ArrayList<>();
        for (WebSocketSession session : sessions) result.addAll(payloads(session));
        return result;
    }

    // ---------- session helpers ----------

    static WebSocketSession session(String code, String playerId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create(
                "ws://localhost/ws/lobby?code=" + code + "&player_id=" + playerId));
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    static List<JsonNode> payloads(WebSocketSession session) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        List<JsonNode> result = new ArrayList<>();
        for (TextMessage message : captor.getAllValues()) result.add(MAPPER.readTree(message.getPayload()));
        return result;
    }

    static JsonNode lastPayload(WebSocketSession session) throws Exception {
        List<JsonNode> sent = payloads(session);
        return sent.get(sent.size() - 1);
    }

    static void assertError(WebSocketSession session, String code) throws Exception {
        JsonNode last = lastPayload(session);
        assertThat(last.path("type").asText()).isEqualTo("error");
        assertThat(last.path("code").asText()).isEqualTo(code);
        assertThat(last.path("detail").asText()).isNotBlank();
    }

    static void assertNoError(WebSocketSession session) throws Exception {
        assertThat(payloads(session)).noneMatch(payload -> payload.path("type").asText().equals("error"));
    }
}
