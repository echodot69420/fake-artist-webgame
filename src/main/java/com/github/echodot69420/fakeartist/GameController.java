package com.github.echodot69420.fakeartist;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
class GameController {
    private final GameService games;

    @Autowired
    GameController(GameService games) {
        this.games = games;
    }

    GameController(ObjectProvider<LobbyHandler> lobbyHandler) {
        this(new GameService(lobbyHandler));
    }

    @PostMapping("/host")
    Map<String, Object> host(@Valid @RequestBody HostRequest request) {
        return games.host(request);
    }

    @PostMapping("/connect")
    Map<String, Object> connect(@Valid @RequestBody ConnectRequest request) {
        return games.connect(request);
    }

    @PostMapping("/quit")
    Map<String, Object> quit(@Valid @RequestBody QuitRequest request) {
        return games.quit(request);
    }

    @GetMapping("/game/{code}")
    Map<String, Object> game(@PathVariable String code) {
        return games.getGame(games.cleanCode(code)).asMap();
    }

    @GetMapping("/health")
    Map<String, Object> health() {
        return Map.of("status", "ok", "service", "fake-artist-goes-to-new-york",
                "games", games.gameCount(), "players", games.playerCount());
    }

    @GetMapping("/models")
    Map<String, Object> models() {
        return Map.of("object", "list", "data", List.of());
    }

    Game getGame(String code) {
        return games.getGame(code);
    }

    Game getGameIfPresent(String code) {
        return games.getGameIfPresent(code);
    }

    synchronized void removePlayer(Game game, String playerId) {
        games.removePlayer(game, playerId);
    }
}
