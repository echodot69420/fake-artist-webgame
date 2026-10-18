package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;

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

    @Test
    void hostCreatesGameWithTrimmedValuesAndHostPlayer() {
        Map<String, Object> response = controller.host(new HostRequest("  Alice  ", "  Space  "));

        assertThat(response).containsEntry("theme", "Space");
        assertThat((String) response.get("code")).hasSize(4);
        assertThat(controller.getGame((String) response.get("code")).players)
                .singleElement()
                .satisfies(player -> {
                    assertThat(player.username()).isEqualTo("Alice");
                    assertThat(player.is_host()).isTrue();
                });
    }

    @Test
    void connectAcceptsCaseInsensitiveCodeAndBroadcastsPlayers() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));
        String code = (String) hosted.get("code");

        Map<String, Object> response = controller.connect(new ConnectRequest("Bob", code.toLowerCase()));

        assertThat(response.get("code")).isEqualTo(code);
        assertThat(controller.getGame(code).players).hasSize(2);
        verify(lobbyHandler).broadcastPlayers(code);
    }

    @Test
    void duplicateUsernameIsRejectedIgnoringCase() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));

        assertThatThrownBy(() -> controller.connect(new ConnectRequest("ALICE", (String) hosted.get("code"))))
                .isInstanceOf(ApiException.class)
                .hasMessage("That username is taken in this game.")
                .extracting(exception -> ((ApiException) exception).status)
                .isEqualTo(HttpStatus.CONFLICT);
    }

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

    @Test
    void removingLastPlayerDeletesGame() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));
        String code = (String) hosted.get("code");
        String playerId = controller.getGame(code).players.get(0).player_id();

        controller.removePlayer(controller.getGame(code), playerId);

        assertThat(controller.getGameIfPresent(code)).isNull();
    }

    @Test
    void connectRejectsAnEleventhPlayer() {
        Map<String, Object> hosted = controller.host(new HostRequest("Host", "Space"));
        String code = (String) hosted.get("code");
        for (int i = 1; i < 10; i++) {
            controller.connect(new ConnectRequest("Player" + i, code));
        }

        assertThatThrownBy(() -> controller.connect(new ConnectRequest("Player10", code)))
                .isInstanceOf(ApiException.class)
                .hasMessage("Game is full.")
                .extracting(exception -> ((ApiException) exception).status)
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void connectRejectsUnknownGame() {
        assertThatThrownBy(() -> controller.connect(new ConnectRequest("Alice", "ZZZZ")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Game not found.")
                .extracting(exception -> ((ApiException) exception).status)
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void removePlayerRejectsUnknownPlayer() {
        Map<String, Object> hosted = controller.host(new HostRequest("Alice", "Space"));
        String code = (String) hosted.get("code");

        assertThatThrownBy(() -> controller.removePlayer(controller.getGame(code), "missing-player"))
                .isInstanceOf(ApiException.class)
                .hasMessage("Player is not in this game.")
                .extracting(exception -> ((ApiException) exception).status)
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void hostRejectsEmptyTheme() {
        // Equivalence Partitioning: empty themes are invalid; representative invalid partition.
        assertThatThrownBy(() -> controller.host(new HostRequest("Alice", "   ")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Theme must be 1-20 characters.")
                .extracting(exception -> ((ApiException) exception).status)
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void hostAcceptsThemeAtMaximumLength() {
        // Boundary Value Analysis: 20 characters is the largest valid theme.
        assertThat(controller.host(new HostRequest("Alice", "12345678901234567890")))
                .containsEntry("theme", "12345678901234567890");
    }

    @Test
    void hostRejectsThemeAboveMaximumLength() {
        // Boundary Value Analysis: 21 characters is the first invalid value above the limit.
        assertThatThrownBy(() -> controller.host(new HostRequest("Alice", "123456789012345678901")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Theme must be 1-20 characters.")
                .extracting(exception -> ((ApiException) exception).status)
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void hostRejectsUsernameShorterThanMinimum() {
        // Boundary Value Analysis: one character is below the one-character username minimum.
        assertThatThrownBy(() -> controller.host(new HostRequest("", "Space")))
                .isInstanceOf(ApiException.class)
                .hasMessage("Username must be 1-16 alphanumeric characters.")
                .extracting(exception -> ((ApiException) exception).status)
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
    @Test
    void hostAcceptsOneCharacterUsername() {
        Map<String, Object> response =
            controller.host(new HostRequest("A", "Space"));

        assertThat(controller.getGame((String) response.get("code")).players)
                .singleElement()
                .satisfies(player -> assertThat(player.username()).isEqualTo("A"));
    }

}
