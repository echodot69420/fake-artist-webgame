package com.github.echodot69420.fakeartist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class Game {
    final String code;
    final String theme;
    final List<GamePlayer> players = new ArrayList<>();
    final Map<String, String> topics = new HashMap<>();
    boolean started;
    boolean collectingTopics;

    Game(String code, String theme, String username) {
        this.code = code;
        this.theme = theme;
        players.add(new GamePlayer(UUID.randomUUID().toString().replace("-", ""), username, true));
    }

    Map<String, Object> asMap() {
        return Map.of("code", code, "theme", theme, "players", List.copyOf(players), "started", started);
    }
}
