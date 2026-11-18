package com.github.echodot69420.fakeartist;

import com.github.echodot69420.fakeartist.errors.ApiException;
import com.github.echodot69420.fakeartist.errors.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GameControllerUnitTests {
    private GameController controller;
    private LobbyHandler lobbyHandler;

    @BeforeEach
    void setUp() {
        // Test double: isolate controller behavior from WebSocket broadcasting.
        lobbyHandler = mock(LobbyHandler.class);
        ObjectProvider<LobbyHandler> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(lobbyHandler);
        controller = new GameController(provider);
    }

    // FR-01-03, FR-01-04: hosting trims input and makes the requester host.
    @Test
    void hostCreatesGameWithTrimmedValuesAndHostPlayer() {
        Map<String, Object> response = controller.host(new HostRequest("Alice", "  Space  "));

        assertThat(response).containsEntry("theme", "Space");
        assertThat((String) response.get("code")).hasSize(4);
        assertThat(controller.getGame((String) response.get("code")).players)
                .singleElement()
                .satisfies(player -> {
                    assertThat(player.username()).isEqualTo("Alice");
                    assertThat(player.is_host()).isTrue();
                });
    }

    // FR-02-01, FR-07-03: codes are case-normalised and joins are broadcast.
    @Test
    void connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));
        String code = (String) hosted.get("code");

        Map<String, Object> response = controller.connect(new ConnectRequest("Bob", code.toLowerCase()));

        assertThat(response.get("code")).isEqualTo(code);
        assertThat(controller.getGame(code).players).hasSize(2);
        verify(lobbyHandler).broadcastPlayers(code);
    }

    // FR-11-04: host status passes to the next player.
    @Test
    void removeHostTransfersHostRoleToNextPlayer() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));
        String code = (String) hosted.get("code");
        controller.connect(new ConnectRequest("Bob", code));
        String hostId = controller.getGame(code).players.get(0).player_id();
        String nextPlayerId = controller.getGame(code).players.get(1).player_id();

        controller.removePlayer(controller.getGame(code), hostId);

        assertThat(controller.getGame(code).players).singleElement()
                .satisfies(player -> {
                    assertThat(player.player_id()).isEqualTo(nextPlayerId);
                    assertThat(player.is_host()).isTrue();
                });
    }

    // FR-11-06: an empty game is deleted.
    @Test
    void removingLastPlayerDeletesGame() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));
        String code = (String) hosted.get("code");
        String playerId = controller.getGame(code).players.get(0).player_id();

        controller.removePlayer(controller.getGame(code), playerId);

        assertThat(controller.getGameIfPresent(code)).isNull();
    }

    // FR-02-02: joining a missing game is rejected.
    @Test
    void connectRejectsUnknownGame() {
        assertThatThrownBy(() -> controller.connect(new ConnectRequest("Alice", "ZZZZ")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Game not found.")
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo(ErrorCode.GAME_NOT_FOUND);
    }

    // No requirement: removing an unknown player is rejected.
    @Test
    void removePlayerRejectsUnknownPlayer() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));
        String code = (String) hosted.get("code");

        assertThatThrownBy(() -> controller.removePlayer(controller.getGame(code), "missing-player"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Player is not in this game.")
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo(ErrorCode.PLAYER_NOT_FOUND);
    }

    // FR-01-01, FR-04: an empty theme is rejected.
    @Test
    void hostRejectsEmptyTheme() {
        // Equivalence Partitioning: empty themes are invalid; representative invalid partition.
        assertThatThrownBy(() -> controller.host(new HostRequest("Alice", "   ")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Theme must be 1-20 characters.")
                .extracting(exception -> ((ApiException) exception).code())
                .isEqualTo(ErrorCode.INVALID_THEME);
    }

    // FR-01-01, FR-03: leading whitespace in a username is rejected.
    @Test
void hostRejectsUsernameWithLeadingWhitespace() {
    assertThatThrownBy(() ->
            controller.host(new HostRequest(" Alice", "Space")))
            .isInstanceOf(ApiException.class)
            .hasMessage("Username must be 1-16 alphanumeric characters.")
            .extracting(exception -> ((ApiException) exception).code())
            .isEqualTo(ErrorCode.INVALID_USERNAME);
}




}
