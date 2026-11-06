package com.github.echodot69420.fakeartist;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableScheduling
@EnableWebSocket
class WebSocketConfiguration implements WebSocketConfigurer {
    private final LobbyHandler lobbyHandler;

    WebSocketConfiguration(LobbyHandler lobbyHandler) {
        this.lobbyHandler = lobbyHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(lobbyHandler, "/ws/lobby").setAllowedOrigins("*");
    }
}
