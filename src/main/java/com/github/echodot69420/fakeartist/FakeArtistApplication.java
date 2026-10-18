package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootApplication
@EnableScheduling
@EnableWebSocket
public class FakeArtistApplication implements WebSocketConfigurer {
    private final LobbyHandler lobbyHandler;

    public FakeArtistApplication(LobbyHandler lobbyHandler) { this.lobbyHandler = lobbyHandler; }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FakeArtistApplication.class);
        app.setDefaultProperties(Map.of("server.port", "8000"));
        app.run(args);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lobbyHandler, "/ws/lobby").setAllowedOrigins("*");
    }
}

record Item(String name, double price, boolean in_stock) {}
record HostRequest(@Size(min = 2, max = 16) String username, @Size(min = 1, max = 20) String theme) {}
record ConnectRequest(@Size(min = 2, max = 16) String username, @Size(min = 1, max = 4) String code) {}
record QuitRequest(@Size(min = 1, max = 4) String code, @Size(min = 1) String player_id) {}
record GamePlayer(String player_id, String username, boolean is_host) {}

final class Game {
    final String code;
    final String theme;
    final List<GamePlayer> players = new ArrayList<>();
    boolean started;

    Game(String code, String theme, String username) {
        this.code = code;
        this.theme = theme;
        players.add(new GamePlayer(UUID.randomUUID().toString().replace("-", ""), username, true));
    }

    Map<String, Object> asMap() {
        return Map.of("code", code, "theme", theme, "players", List.copyOf(players), "started", started);
    }
}

@RestController
class GameController {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final Map<Integer, Item> items = new LinkedHashMap<>();
    private final AtomicInteger nextItemId = new AtomicInteger(1);
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final ObjectProvider<LobbyHandler> lobbyHandler;

    GameController(ObjectProvider<LobbyHandler> lobbyHandler) { this.lobbyHandler = lobbyHandler; }

    @GetMapping("/")
    ResponseEntity<Resource> homePage() { return ResponseEntity.ok(new FileSystemResource(Path.of("index.html"))); }

    @GetMapping("/lobby")
    ResponseEntity<Resource> lobbyPage() { return ResponseEntity.ok(new FileSystemResource(Path.of("lobby.html"))); }

    @GetMapping("/items")
    synchronized List<Map<String, Object>> listItems() { return items.entrySet().stream().map(e -> itemMap(e.getKey(), e.getValue())).toList(); }

    @GetMapping("/items/{itemId}")
    synchronized Map<String, Object> getItem(@PathVariable int itemId) {
        Item item = items.get(itemId);
        if (item == null) throw new ApiException(HttpStatus.NOT_FOUND, "Item not found");
        return itemMap(itemId, item);
    }

    @PostMapping("/items")
    synchronized ResponseEntity<Map<String, Object>> createItem(@RequestBody Item item) {
        int id = nextItemId.getAndIncrement(); items.put(id, item);
        return ResponseEntity.status(HttpStatus.CREATED).body(itemMap(id, item));
    }

    @DeleteMapping("/items/{itemId}")
    synchronized ResponseEntity<Void> deleteItem(@PathVariable int itemId) {
        if (items.remove(itemId) == null) throw new ApiException(HttpStatus.NOT_FOUND, "Item not found");
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/host")
    synchronized Map<String, Object> host(@Valid @RequestBody HostRequest request) {
        Game game = new Game(newCode(), cleanTheme(request.theme()), cleanUsername(request.username()));
        games.put(game.code, game); return game.asMap();
    }

    @PostMapping("/connect")
    synchronized Map<String, Object> connect(@Valid @RequestBody ConnectRequest request) {
        Game game = getGame(cleanCode(request.code())); String username = cleanUsername(request.username());
        if (game.started) throw new ApiException(HttpStatus.CONFLICT, "Game already started.");
        if (game.players.size() >= 10) throw new ApiException(HttpStatus.CONFLICT, "Game is full.");
        if (game.players.stream().anyMatch(p -> p.username().equalsIgnoreCase(username))) throw new ApiException(HttpStatus.CONFLICT, "That username is taken in this game.");
        game.players.add(new GamePlayer(UUID.randomUUID().toString().replace("-", ""), username, false));
        lobbyHandler.getObject().broadcastPlayers(game.code); return game.asMap();
    }

    @PostMapping("/quit")
    synchronized Map<String, Object> quit(@Valid @RequestBody QuitRequest request) {
        String code = cleanCode(request.code()); Game game = getGame(code); removePlayer(game, request.player_id()); lobbyHandler.getObject().broadcastPlayers(code);
        Game remaining = games.get(code);
        return Map.of("code", code, "game_exists", remaining != null, "players", remaining == null ? List.of() : List.copyOf(remaining.players));
    }

    @GetMapping("/game/{code}")
    synchronized Map<String, Object> game(@PathVariable String code) { return getGame(cleanCode(code)).asMap(); }

    @GetMapping("/health")
    Map<String, Object> health() { return Map.of("status", "ok", "service", "fake-artist-goes-to-new-york", "games", games.size(), "players", games.values().stream().mapToInt(g -> g.players.size()).sum()); }

    @GetMapping("/models")
    Map<String, Object> models() { return Map.of("object", "list", "data", List.of()); }

    Game getGame(String code) { Game game = games.get(code); if (game == null) throw new ApiException(HttpStatus.NOT_FOUND, "Game not found."); return game; }
    Game getGameIfPresent(String code) { return games.get(code); }

    synchronized void removePlayer(Game game, String playerId) {
        int index = -1; for (int i = 0; i < game.players.size(); i++) if (game.players.get(i).player_id().equals(playerId)) { index = i; break; }
        if (index < 0) throw new ApiException(HttpStatus.NOT_FOUND, "Player is not in this game.");
        boolean wasHost = game.players.get(index).is_host(); game.players.remove(index);
        if (game.players.isEmpty()) games.remove(game.code);
        else if (wasHost) { GamePlayer next = game.players.get(0); game.players.set(0, new GamePlayer(next.player_id(), next.username(), true)); }
    }

    private String newCode() {
        for (int attempt = 0; attempt < 1000; attempt++) { StringBuilder code = new StringBuilder(4); for (int i = 0; i < 4; i++) code.append(CODE_ALPHABET.charAt(ThreadLocalRandom.current().nextInt(CODE_ALPHABET.length()))); if (!games.containsKey(code.toString())) return code.toString(); }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No game codes available right now.");
    }
    private String cleanUsername(String value) {
    String username = value == null ? "" : value;
    if (!username.matches("[A-Za-z0-9]{1,16}")) {
        throw new ApiException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "Username must be 1-16 alphanumeric characters.");
    }
    return username;
}



    private String cleanTheme(String value) { String theme = value == null ? "" : value.trim(); if (theme.isEmpty() || theme.length() > 20) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Theme must be 1-20 characters."); return theme; }
    private String cleanCode(String value) { String code = value == null ? "" : value.trim().toUpperCase(); if (!code.matches("[A-Z0-9]{4}")) throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Game code must be 4 characters."); return code; }
    private Map<String, Object> itemMap(int id, Item item) { return Map.of("id", id, "name", item.name(), "price", item.price(), "in_stock", item.in_stock()); }
}

@Component
class LobbyHandler extends TextWebSocketHandler {
    private final ObjectMapper mapper; private final GameController controller;
    private final Map<String, Map<String, WebSocketSession>> connections = new ConcurrentHashMap<>();
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>(); private final Map<String, Boolean> away = new ConcurrentHashMap<>();

    LobbyHandler(ObjectMapper mapper, GameController controller) { this.mapper = mapper; this.controller = controller; }

    @Override public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        String code = queryValue(session, "code").toUpperCase(), playerId = queryValue(session, "player_id"); Game game = controller.getGameIfPresent(code);
        if (game == null) { closeWithBye(session, "game not found", false); return; }
        if (game.players.stream().noneMatch(p -> p.player_id().equals(playerId))) { closeWithBye(session, "player not in this game", false); return; }
        connections.computeIfAbsent(code, ignored -> new ConcurrentHashMap<>()).put(playerId, session); lastSeen.put(key(code, playerId), System.currentTimeMillis()); away.put(key(code, playerId), false); broadcastPlayers(code);
    }

    @Override protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String code = queryValue(session, "code").toUpperCase(), playerId = queryValue(session, "player_id"), key = key(code, playerId); lastSeen.put(key, System.currentTimeMillis());
        JsonNode node; try { node = mapper.readTree(message.getPayload()); } catch (Exception e) { send(session, Map.of("type", "error", "detail", "expected JSON")); return; }
        String type = node.path("type").asText();
        if ("ping".equals(type)) { away.put(key, false); send(session, Map.of("type", "pong", "at", System.currentTimeMillis() / 1000.0)); }
        else if ("away".equals(type)) away.put(key, true); else if ("quit".equals(type)) drop(code, playerId, "quit"); else send(session, Map.of("type", "error", "detail", "unknown message type: " + type));
    }

    @Override public void afterConnectionClosed(WebSocketSession session, CloseStatus status) { drop(queryValue(session, "code").toUpperCase(), queryValue(session, "player_id"), "disconnected"); }

    @Scheduled(fixedDelay = 15000) public void sweep() { long now = System.currentTimeMillis(); connections.forEach((code, players) -> players.forEach((playerId, session) -> { long timeout = Boolean.TRUE.equals(away.get(key(code, playerId))) ? 120000 : 15000; if (now - lastSeen.getOrDefault(key(code, playerId), now) > timeout) drop(code, playerId, "heartbeat timeout"); })); }

    void broadcastPlayers(String code) {
        Game game = controller.getGameIfPresent(code); Map<String, Object> payload = new LinkedHashMap<>(); payload.put("type", "players"); payload.put("code", code); payload.put("theme", game == null ? "" : game.theme); payload.put("game_exists", game != null); payload.put("players", game == null ? List.of() : List.copyOf(game.players)); payload.put("started", game != null && game.started);
        connections.getOrDefault(code, Map.of()).values().forEach(session -> send(session, payload));
    }

    private void drop(String code, String playerId, String reason) {
        WebSocketSession session = connections.getOrDefault(code, Map.of()).remove(playerId); if (session == null) return; lastSeen.remove(key(code, playerId)); away.remove(key(code, playerId)); Game game = controller.getGameIfPresent(code); if (game != null) try { controller.removePlayer(game, playerId); } catch (ApiException ignored) {}
        Game remaining = controller.getGameIfPresent(code); send(session, Map.of("type", "bye", "reason", reason, "game_exists", remaining != null)); try { session.close(new CloseStatus(4401, reason)); } catch (IOException ignored) {} broadcastPlayers(code);
    }
    private void closeWithBye(WebSocketSession session, String reason, boolean gameExists) throws IOException { send(session, Map.of("type", "bye", "reason", reason, "game_exists", gameExists)); session.close(new CloseStatus(4404, reason)); }
    private void send(WebSocketSession session, Map<String, Object> payload) { try { if (session.isOpen()) session.sendMessage(new TextMessage(mapper.writeValueAsString(payload))); } catch (IOException ignored) {} }
    private String queryValue(WebSocketSession session, String name) { String query = session.getUri() == null ? "" : session.getUri().getRawQuery(); if (query != null) for (String part : query.split("&")) { String[] pair = part.split("=", 2); if (pair.length == 2 && pair[0].equals(name)) return URLDecoder.decode(pair[1], StandardCharsets.UTF_8); } return ""; }
    private String key(String code, String playerId) { return code + ":" + playerId; }
}

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ApiException.class) ResponseEntity<Map<String, String>> api(ApiException e) { return ResponseEntity.status(e.status).body(Map.of("detail", e.getMessage())); }
    @ExceptionHandler(Exception.class) ResponseEntity<Map<String, String>> validation(Exception e) { return ResponseEntity.badRequest().body(Map.of("detail", "Invalid request.")); }
}

final class ApiException extends RuntimeException {
    final HttpStatus status; ApiException(HttpStatus status, String message) { super(message); this.status = status; }
}