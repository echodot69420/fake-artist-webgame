package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
class LobbyHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final GameController controller;
    private final Map<String, Map<String, WebSocketSession>> connections = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private final Map<String, Boolean> away = new ConcurrentHashMap<>();

    LobbyHandler(ObjectMapper mapper, GameController controller) {
        this.mapper = mapper;
        this.controller = controller;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String code = queryValue(session, "code").toUpperCase();
        String playerId = queryValue(session, "player_id");
        Game game = controller.getGameIfPresent(code);
        if (game == null) {
            closeWithBye(session, "game not found", false);
            return;
        }
        if (game.players.stream().noneMatch(player -> player.player_id().equals(playerId))) {
            closeWithBye(session, "player not in this game", false);
            return;
        }
        connections.computeIfAbsent(code, ignored -> new ConcurrentHashMap<>()).put(playerId, session);
        lastSeen.put(key(code, playerId), System.currentTimeMillis());
        away.put(key(code, playerId), false);
        broadcastPlayers(code);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String code = queryValue(session, "code").toUpperCase();
        String playerId = queryValue(session, "player_id");
        String key = key(code, playerId);
        lastSeen.put(key, System.currentTimeMillis());
        JsonNode node;
        try {
            node = mapper.readTree(message.getPayload());
        } catch (Exception exception) {
            send(session, Map.of("type", "error", "detail", "expected JSON"));
            return;
        }
        String type = node.path("type").asText();
        if ("ping".equals(type)) {
            away.put(key, false);
            send(session, Map.of("type", "pong", "at", System.currentTimeMillis() / 1000.0));
        } else if ("away".equals(type)) {
            away.put(key, true);
        } else if ("quit".equals(type)) {
            drop(code, playerId, "quit");
        } else if ("start".equals(type)) {
            startGame(code, playerId, session);
        } else {
            send(session, Map.of("type", "error", "detail", "unknown message type: " + type));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        drop(queryValue(session, "code").toUpperCase(), queryValue(session, "player_id"), "disconnected");
    }

    @Scheduled(fixedDelay = 15000)
    public void sweep() {
        long now = System.currentTimeMillis();
        connections.forEach((code, players) -> players.forEach((playerId, session) -> {
            String key = key(code, playerId);
            long timeout = Boolean.TRUE.equals(away.get(key)) ? 120000 : 15000;
            if (now - lastSeen.getOrDefault(key, now) > timeout) drop(code, playerId, "heartbeat timeout");
        }));
    }

    void broadcastPlayers(String code) {
        Game game = controller.getGameIfPresent(code);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "players");
        payload.put("code", code);
        payload.put("theme", game == null ? "" : game.theme);
        payload.put("game_exists", game != null);
        payload.put("players", game == null ? List.of() : List.copyOf(game.players));
        payload.put("started", game != null && game.started);
        connections.getOrDefault(code, Map.of()).values().forEach(session -> send(session, payload));
    }

    private synchronized void startGame(String code, String playerId, WebSocketSession session) {
        Game game = controller.getGameIfPresent(code);
        if (game == null) {
            send(session, Map.of("type", "error", "detail", "Game not found."));
            return;
        }
        GamePlayer player = game.players.stream()
                .filter(candidate -> candidate.player_id().equals(playerId))
                .findFirst()
                .orElse(null);
        if (player == null || !player.is_host()) {
            send(session, Map.of("type", "error", "detail", "Only the host can start the game."));
            return;
        }
        if (game.players.size() < 3) {
            send(session, Map.of("type", "error", "detail", "Need 3+ players to start the game."));
            return;
        }
        if (!game.started) {
            game.started = true;
            broadcastPlayers(code);
        }
    }

    private void drop(String code, String playerId, String reason) {
        WebSocketSession session = connections.getOrDefault(code, Map.of()).remove(playerId);
        if (session == null) return;
        lastSeen.remove(key(code, playerId));
        away.remove(key(code, playerId));
        Game game = controller.getGameIfPresent(code);
        if (game != null) {
            try {
                controller.removePlayer(game, playerId);
            } catch (ApiException ignored) {
                // The player may already have left through the HTTP endpoint.
            }
        }
        Game remaining = controller.getGameIfPresent(code);
        send(session, Map.of("type", "bye", "reason", reason, "game_exists", remaining != null));
        try {
            session.close(new CloseStatus(4401, reason));
        } catch (IOException ignored) {
            // The session is already being closed.
        }
        broadcastPlayers(code);
    }

    private void closeWithBye(WebSocketSession session, String reason, boolean gameExists) throws IOException {
        send(session, Map.of("type", "bye", "reason", reason, "game_exists", gameExists));
        session.close(new CloseStatus(4404, reason));
    }

    private void send(WebSocketSession session, Map<String, Object> payload) {
        try {
            if (session.isOpen()) session.sendMessage(new TextMessage(mapper.writeValueAsString(payload)));
        } catch (IOException ignored) {
            // A disconnected client cannot receive the notification.
        }
    }

    private String queryValue(WebSocketSession session, String name) {
        String query = session.getUri() == null ? "" : session.getUri().getRawQuery();
        if (query != null) {
            for (String part : query.split("&")) {
                String[] pair = part.split("=", 2);
                if (pair.length == 2 && pair[0].equals(name)) {
                    return URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                }
            }
        }
        return "";
    }

    private String key(String code, String playerId) {
        return code + ":" + playerId;
    }
}
