package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.echodot69420.fakeartist.errors.ApiException;
import com.github.echodot69420.fakeartist.errors.ErrorCode;
import com.github.echodot69420.fakeartist.errors.WebSocketErrorHandler;
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
import java.util.concurrent.ThreadLocalRandom;

@Component
class LobbyHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper;
    private final GameController controller;
    private final FeatureFlags featureFlags;
    private final WebSocketErrorHandler errors;
    private final Map<String, Map<String, WebSocketSession>> connections = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private final Map<String, Boolean> away = new ConcurrentHashMap<>();

    LobbyHandler(ObjectMapper mapper, GameController controller, FeatureFlags featureFlags,
                 WebSocketErrorHandler errors) {
        this.mapper = mapper;
        this.controller = controller;
        this.featureFlags = featureFlags;
        this.errors = errors;
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
        if (game.collectingTopics) {
            send(session, Map.of("type", "topic_prompt", "theme", game.theme,
                    "submitted", game.topics.containsKey(playerId), "total", game.players.size()));
            broadcastTopicProgress(code, game);
        }
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
        } catch (JsonProcessingException exception) {
            errors.send(session, ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.defaultMessage());
            return;
        }
        String type = node.path("type").asText();
        errors.run(session, type, () -> dispatch(session, code, playerId, key, type, node));
    }

    private void dispatch(WebSocketSession session, String code, String playerId, String key,
                          String type, JsonNode node) {
        if ("ping".equals(type)) {
            away.put(key, false);
            send(session, Map.of("type", "pong", "at", System.currentTimeMillis() / 1000.0));
        } else if ("away".equals(type)) {
            away.put(key, true);
        } else if ("quit".equals(type)) {
            drop(code, playerId, "quit");
        } else if ("start".equals(type)) {
            startGame(code, playerId, session);
        } else if ("topic".equals(type)) {
            submitTopic(code, playerId, session, node.path("value").asText());
        } else if ("kick".equals(type)) {
            kickPlayer(code, playerId, session, node.path("player_id").asText());
        } else {
            throw new ApiException(ErrorCode.UNKNOWN_MESSAGE_TYPE);
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
        payload.put("collecting_topics", game != null && game.collectingTopics);
        payload.put("kick_enabled", featureFlags.kickPlayer());
        connections.getOrDefault(code, Map.of()).values().forEach(session -> send(session, payload));
    }

    private synchronized void kickPlayer(String code, String playerId, WebSocketSession session, String targetId) {
        if (!featureFlags.kickPlayer()) {
            throw new ApiException(ErrorCode.FEATURE_DISABLED, "Player kicking is disabled.");
        }
        Game game = controller.getGameIfPresent(code);
        if (game == null) {
            throw new ApiException(ErrorCode.GAME_NOT_FOUND);
        }
        GamePlayer requester = game.players.stream()
                .filter(player -> player.player_id().equals(playerId))
                .findFirst()
                .orElse(null);
        if (requester == null || !requester.is_host()) {
            throw new ApiException(ErrorCode.NOT_HOST, "Only the host can kick players.");
        }
        GamePlayer target = game.players.stream()
                .filter(player -> player.player_id().equals(targetId))
                .findFirst()
                .orElse(null);
        if (target == null || target.is_host() || target.player_id().equals(playerId)) {
            throw new ApiException(ErrorCode.INVALID_KICK_TARGET);
        }
        if (connections.getOrDefault(code, Map.of()).containsKey(targetId)) {
            drop(code, targetId, "kicked by host");
            return;
        }
        game.topics.remove(targetId);
        controller.removePlayer(game, targetId);
        broadcastPlayers(code);
    }

    private synchronized void startGame(String code, String playerId, WebSocketSession session) {
        Game game = controller.getGameIfPresent(code);
        if (game == null) {
            throw new ApiException(ErrorCode.GAME_NOT_FOUND);
        }
        GamePlayer player = game.players.stream()
                .filter(candidate -> candidate.player_id().equals(playerId))
                .findFirst()
                .orElse(null);
        if (player == null || !player.is_host()) {
            throw new ApiException(ErrorCode.NOT_HOST, "Only the host can start the game.");
        }
        if (game.players.size() < 3) {
            throw new ApiException(ErrorCode.NOT_ENOUGH_PLAYERS);
        }
        if (!game.started) {
            if (!game.collectingTopics) {
                game.collectingTopics = true;
                broadcastPlayers(code);
                broadcastTopicPrompt(code, game);
            }
        }
    }

    private synchronized void submitTopic(String code, String playerId, WebSocketSession session, String value) {
        Game game = controller.getGameIfPresent(code);
        if (game == null) {
            throw new ApiException(ErrorCode.GAME_NOT_FOUND);
        }
        if (!game.collectingTopics || game.started) {
            throw new ApiException(ErrorCode.TOPIC_WINDOW_CLOSED);
        }
        if (game.players.stream().noneMatch(player -> player.player_id().equals(playerId))) {
            throw new ApiException(ErrorCode.PLAYER_NOT_FOUND);
        }
        String topic = value == null ? "" : value.trim();
        if (topic.isEmpty() || topic.length() > 40) {
            throw new ApiException(ErrorCode.INVALID_TOPIC);
        }
        game.topics.put(playerId, topic);
        broadcastTopicProgress(code, game);
        if (game.players.stream().allMatch(player -> game.topics.containsKey(player.player_id()))) {
            game.collectingTopics = false;
            sendTopicResultsAndRoleToPlayers(code, game);
            game.started = true;
            broadcastPlayers(code);
        }
    }

    private void sendTopicResultsAndRoleToPlayers(String code, Game game) {
        Map<String, WebSocketSession> gameConnections = connections.getOrDefault(code, Map.of());
        int fakeArtistIndex = ThreadLocalRandom.current().nextInt(game.players.size());
        GamePlayer fakeArtist = game.players.get(fakeArtistIndex);
        int selectedTopicIndex = ThreadLocalRandom.current().nextInt(game.players.size() - 1);
        if (selectedTopicIndex >= fakeArtistIndex) selectedTopicIndex++;
        String selectedTopic = game.topics.get(game.players.get(selectedTopicIndex).player_id());

        game.players.forEach(player -> {
            String message = player.player_id().equals(fakeArtist.player_id())
                    ? "You are the fake artist"
                    : "The chosen topic is: " + selectedTopic;
            WebSocketSession session = gameConnections.get(player.player_id());
            if (session != null) {
                send(session, Map.of("type", "player_popup", "message", message));
                if (featureFlags.roundResults()) {
                    send(session, Map.of("type", "round_results", "message", message));
                }
            }
        });
    }

    private void broadcastTopicPrompt(String code, Game game) {
        connections.getOrDefault(code, Map.of()).forEach((playerId, session) -> {
            send(session, Map.of("type", "topic_prompt", "theme", game.theme,
                    "submitted", game.topics.containsKey(playerId),
                    "total", game.players.size()));
        });
        broadcastTopicProgress(code, game);
    }

    private void broadcastTopicProgress(String code, Game game) {
        long submitted = game.players.stream()
                .filter(player -> game.topics.containsKey(player.player_id()))
                .count();
        connections.getOrDefault(code, Map.of()).values().forEach(session ->
                send(session, Map.of("type", "topic_progress", "submitted", submitted,
                        "total", game.players.size())));
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
        if (game != null && game.collectingTopics) {
            game.topics.remove(playerId);
            if (game.players.stream().allMatch(player -> game.topics.containsKey(player.player_id()))) {
                game.collectingTopics = false;
                game.started = true;
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
        if (remaining != null && remaining.collectingTopics) broadcastTopicProgress(code, remaining);
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
