package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LobbyHandlerFeatureFlagTests {
    @Test
    void disabledRoundResultsFlagDoesNotSendExperimentalMessage() throws Exception {
        FeatureFlags flags = new FeatureFlags();
        flags.setRoundResults(false);
        Fixture fixture = establishSessions(flags);

        completeTopicSubmission(fixture);

        List<String> payloads = payloadsFrom(fixture.sessions());
        assertThat(payloads).anyMatch(payload -> payload.contains("\"type\":\"player_popup\""));
        assertThat(payloads).noneMatch(payload -> payload.contains("\"type\":\"round_results\""));
    }

    static Fixture establishSessions(FeatureFlags flags) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Game game = new Game("ABCD", "Space", "Host");
        game.players.add(new GamePlayer("guest-1", "GuestOne", false));
        game.players.add(new GamePlayer("guest-2", "GuestTwo", false));
        game.collectingTopics = true;
        game.topics.put(game.players.get(0).player_id(), "Rockets");
        game.topics.put(game.players.get(1).player_id(), "Planets");

        GameController controller = mock(GameController.class);
        when(controller.getGameIfPresent("ABCD")).thenReturn(game);
        LobbyHandler handler = new LobbyHandler(mapper, controller, flags);

        WebSocketSession[] sessions = new WebSocketSession[3];
        for (int i = 0; i < sessions.length; i++) {
            sessions[i] = mock(WebSocketSession.class);
            when(sessions[i].getUri()).thenReturn(URI.create(
                    "ws://localhost/ws/lobby?code=ABCD&player_id=" + game.players.get(i).player_id()));
            when(sessions[i].isOpen()).thenReturn(true);
            handler.afterConnectionEstablished(sessions[i]);
        }
        clearInvocations(sessions);
        return new Fixture(handler, sessions);
    }

    static void completeTopicSubmission(Fixture fixture) {
        fixture.handler().handleTextMessage(fixture.sessions()[2],
                new TextMessage("{\"type\":\"topic\",\"value\":\"Comets\"}"));
    }

    static List<String> payloadsFrom(WebSocketSession[] sessions) throws Exception {
        List<String> payloads = new ArrayList<>();
        for (WebSocketSession session : sessions) {
            var message = org.mockito.ArgumentCaptor.forClass(TextMessage.class);
            verify(session, atLeastOnce()).sendMessage(message.capture());
            payloads.addAll(message.getAllValues().stream().map(TextMessage::getPayload).toList());
        }
        return payloads;
    }

    record Fixture(LobbyHandler handler, WebSocketSession[] sessions) {}
}
