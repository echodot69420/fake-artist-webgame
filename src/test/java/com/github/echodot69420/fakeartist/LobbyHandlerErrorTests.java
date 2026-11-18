package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.echodot69420.fakeartist.errors.WebSocketErrorHandler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * LobbyHandler + WebSocketErrorHandler integration: boundary values for topics and start,
 * kick authorization, and protocol errors. Traces to FR-14, FR-16-02, FR-16-03 and NFR-07.
 */
class LobbyHandlerErrorTests {
    private final ObjectMapper mapper = new ObjectMapper();

    // ---------- topic (1-40 after trim) ----------

    @Test // FR-09-02, FR-14
    void emptyTopicIsRejected() throws Exception {
        Fixture f = collectingTopics();
        send(f, f.host(), "{\"type\":\"topic\",\"value\":\"\"}");
        assertError(f.host(), "INVALID_TOPIC");
        assertThat(f.game().topics).isEmpty();
    }

    @Test // FR-09-02, FR-14
    void whitespaceOnlyTopicIsRejected() throws Exception {
        Fixture f = collectingTopics();
        send(f, f.host(), "{\"type\":\"topic\",\"value\":\"   \"}");
        assertError(f.host(), "INVALID_TOPIC");
        assertThat(f.game().topics).isEmpty();
    }

    @Test // FR-09-02, FR-14: lower bound
    void oneCharacterTopicIsAccepted() throws Exception {
        Fixture f = collectingTopics();
        send(f, f.host(), topic("A"));
        assertThat(f.game().topics).containsValue("A");
        assertNoError(f.host());
    }

    @Test // FR-09-02, FR-14: upper bound
    void fortyCharacterTopicIsAccepted() throws Exception {
        Fixture f = collectingTopics();
        send(f, f.host(), topic("T".repeat(40)));
        assertThat(f.game().topics).containsValue("T".repeat(40));
        assertNoError(f.host());
    }

    @Test // FR-09-02, FR-14: just over the upper bound
    void fortyOneCharacterTopicIsRejected() throws Exception {
        Fixture f = collectingTopics();
        send(f, f.host(), topic("T".repeat(41)));
        assertError(f.host(), "INVALID_TOPIC");
        assertThat(f.game().topics).isEmpty();
    }

    @Test // FR-09-01, FR-14
    void topicBeforeStartIsRejected() throws Exception {
        Fixture f = fixture(3);
        send(f, f.host(), topic("Cat"));
        assertError(f.host(), "TOPIC_WINDOW_CLOSED");
    }

    // ---------- start (3+ players, host only) ----------

    @Test // FR-08-02, FR-14: just under the minimum
    void startWithTwoPlayersIsRejected() throws Exception {
        Fixture f = fixture(2);
        send(f, f.host(), "{\"type\":\"start\"}");
        assertError(f.host(), "NOT_ENOUGH_PLAYERS");
        assertThat(f.game().collectingTopics).isFalse();
    }

    @Test // FR-08-02, FR-08-03, FR-14: minimum
    void startWithThreePlayersIsAccepted() throws Exception {
        Fixture f = fixture(3);
        send(f, f.host(), "{\"type\":\"start\"}");
        assertThat(f.game().collectingTopics).isTrue();
        assertNoError(f.host());
    }

    @Test // FR-08-01, FR-14
    void nonHostCannotStart() throws Exception {
        Fixture f = fixture(3);
        send(f, f.guest(), "{\"type\":\"start\"}");
        assertError(f.guest(), "NOT_HOST");
        assertThat(f.game().collectingTopics).isFalse();
    }

    // ---------- kick ----------

    @Test // FR-16-03
    void hostKickingThemselfIsRejected() throws Exception {
        Fixture f = fixture(2, true);
        send(f, f.host(), kick(f.hostId()));
        assertError(f.host(), "INVALID_KICK_TARGET");
        assertThat(f.game().players).hasSize(2);
    }

    // ---------- protocol (NFR-07) ----------

    @Test // FR-14, NFR-07
    void malformedJsonIsRejectedAndSessionStaysOpen() throws Exception {
        Fixture f = fixture(2);
        send(f, f.host(), "not json");
        assertError(f.host(), "MALFORMED_REQUEST");
        verify(f.host(), never()).close(any(CloseStatus.class));
    }

    @Test // FR-14, NFR-01
    void unknownTypeIsRejected() throws Exception {
        Fixture f = fixture(2);
        send(f, f.host(), "{\"type\":\"dance\"}");
        assertError(f.host(), "UNKNOWN_MESSAGE_TYPE");
    }

    @Test // FR-14, NFR-01
    void missingTypeIsRejected() throws Exception {
        Fixture f = fixture(2);
        send(f, f.host(), "{}");
        assertError(f.host(), "UNKNOWN_MESSAGE_TYPE");
    }

    @Test // NFR-07: the connection keeps working after an error
    void connectionSurvivesAnError() throws Exception {
        Fixture f = fixture(2);
        send(f, f.host(), "{\"type\":\"dance\"}");
        send(f, f.host(), "{\"type\":\"ping\"}");

        List<JsonNode> sent = payloads(f.host());
        assertThat(sent.get(sent.size() - 1).path("type").asText()).isEqualTo("pong");
        verify(f.host(), never()).close(any(CloseStatus.class));
    }

    // ---------- fixture ----------

    private Fixture fixture(int players) throws Exception {
        return fixture(players, false);
    }

    private Fixture collectingTopics() throws Exception {
        Fixture f = fixture(3);
        send(f, f.host(), "{\"type\":\"start\"}");
        assertThat(f.game().collectingTopics).isTrue();
        f.sessions().forEach(session -> clearInvocations(session));
        return f;
    }

    private Fixture fixture(int players, boolean kickEnabled) throws Exception {
        ObjectProvider<LobbyHandler> provider = mock(ObjectProvider.class);
        GameController controller = new GameController(provider);
        String code = (String) controller.host(new HostRequest("Host", "Space")).get("code");
        Game game = controller.getGame(code);
        for (int i = 2; i <= players; i++) game.players.add(new GamePlayer("p" + i, "Player" + i, false));

        FeatureFlags flags = new FeatureFlags();
        flags.setKickPlayer(kickEnabled);
        LobbyHandler handler = new LobbyHandler(mapper, controller, flags, new WebSocketErrorHandler(mapper));
        List<WebSocketSession> sessions = new ArrayList<>();
        for (GamePlayer player : game.players) {
            WebSocketSession session = session(code, player.player_id());
            handler.afterConnectionEstablished(session);
            sessions.add(session);
        }
        sessions.forEach(session -> clearInvocations(session));
        return new Fixture(handler, game, sessions);
    }

    private WebSocketSession session(String code, String playerId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(URI.create(
                "ws://localhost/ws/lobby?code=" + code + "&player_id=" + playerId));
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private void send(Fixture f, WebSocketSession session, String payload) {
        f.handler().handleTextMessage(session, new TextMessage(payload));
    }

    private String topic(String value) throws Exception {
        return mapper.writeValueAsString(Map.of("type", "topic", "value", value));
    }

    private String kick(String target) throws Exception {
        return mapper.writeValueAsString(Map.of("type", "kick", "player_id", target));
    }

    private void assertError(WebSocketSession session, String code) throws Exception {
        List<JsonNode> sent = payloads(session);
        JsonNode last = sent.get(sent.size() - 1);
        assertThat(last.path("type").asText()).isEqualTo("error");
        assertThat(last.path("code").asText()).isEqualTo(code);
        assertThat(last.path("detail").asText()).isNotBlank();
    }

    private void assertNoError(WebSocketSession session) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        assertThat(captor.getAllValues())
                .noneMatch(message -> message.getPayload().contains("\"type\":\"error\""));
    }

    private List<JsonNode> payloads(WebSocketSession session) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session, atLeastOnce()).sendMessage(captor.capture());
        List<JsonNode> result = new ArrayList<>();
        for (TextMessage message : captor.getAllValues()) result.add(mapper.readTree(message.getPayload()));
        return result;
    }

    private record Fixture(LobbyHandler handler, Game game, List<WebSocketSession> sessions) {
        WebSocketSession host() { return sessions.get(0); }
        WebSocketSession guest() { return sessions.get(1); }
        String hostId() { return game.players.get(0).player_id(); }
        String guestId() { return game.players.get(1).player_id(); }
    }
}
