package com.github.echodot69420.fakeartist;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Service
class GameService {
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final Map<String, Game> games = new ConcurrentHashMap<>();
    private final ObjectProvider<LobbyHandler> lobbyHandler;

    GameService(ObjectProvider<LobbyHandler> lobbyHandler) {
        this.lobbyHandler = lobbyHandler;
    }

    synchronized Map<String, Object> host(HostRequest request) {
        Game game = new Game(newCode(), cleanTheme(request.theme()), cleanUsername(request.username()));
        games.put(game.code, game);
        return game.asMap();
    }

    synchronized Map<String, Object> connect(ConnectRequest request) {
        Game game = getGame(cleanCode(request.code()));
        String username = cleanUsername(request.username());
        if (game.started || game.collectingTopics) throw new ApiException(HttpStatus.CONFLICT, "Game already started.");
        if (game.players.size() >= 10) throw new ApiException(HttpStatus.CONFLICT, "Game is full.");
        if (game.players.stream().anyMatch(player -> player.username().equalsIgnoreCase(username))) {
            throw new ApiException(HttpStatus.CONFLICT, "That username is taken in this game.");
        }
        game.players.add(new GamePlayer(UUID.randomUUID().toString().replace("-", ""), username, false));
        lobbyHandler.getObject().broadcastPlayers(game.code);
        return game.asMap();
    }

    synchronized Map<String, Object> quit(QuitRequest request) {
        String code = cleanCode(request.code());
        Game game = getGame(code);
        removePlayer(game, request.player_id());
        lobbyHandler.getObject().broadcastPlayers(code);
        Game remaining = games.get(code);
        return Map.of("code", code, "game_exists", remaining != null,
                "players", remaining == null ? List.of() : List.copyOf(remaining.players));
    }

    synchronized void removePlayer(Game game, String playerId) {
        int index = -1;
        for (int i = 0; i < game.players.size(); i++) {
            if (game.players.get(i).player_id().equals(playerId)) {
                index = i;
                break;
            }
        }
        if (index < 0) throw new ApiException(HttpStatus.NOT_FOUND, "Player is not in this game.");
        boolean wasHost = game.players.get(index).is_host();
        game.players.remove(index);
        if (game.players.isEmpty()) {
            games.remove(game.code);
        } else if (wasHost) {
            GamePlayer next = game.players.get(0);
            game.players.set(0, new GamePlayer(next.player_id(), next.username(), true));
        }
    }

    Game getGame(String code) {
        Game game = games.get(code);
        if (game == null) throw new ApiException(HttpStatus.NOT_FOUND, "Game not found.");
        return game;
    }

    Game getGameIfPresent(String code) {
        return games.get(code);
    }

    int gameCount() {
        return games.size();
    }

    int playerCount() {
        return games.values().stream().mapToInt(game -> game.players.size()).sum();
    }

    String cleanCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase();
        if (!code.matches("[A-Z0-9]{4}")) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Game code must be 4 characters.");
        }
        return code;
    }

    private String newCode() {
        for (int attempt = 0; attempt < 1000; attempt++) {
            StringBuilder code = new StringBuilder(4);
            for (int i = 0; i < 4; i++) {
                code.append(CODE_ALPHABET.charAt(ThreadLocalRandom.current().nextInt(CODE_ALPHABET.length())));
            }
            if (!games.containsKey(code.toString())) return code.toString();
        }
        throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "No game codes available right now.");
    }

    private String cleanUsername(String value) {
        String username = value == null ? "" : value;
        if (!username.matches("[A-Za-z0-9]{1,16}")) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Username must be 1-16 alphanumeric characters.");
        }
        return username;
    }

    private String cleanTheme(String value) {
        String theme = value == null ? "" : value.trim();
        if (theme.isEmpty() || theme.length() > 20) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "Theme must be 1-20 characters.");
        }
        return theme;
    }
}
