package com.github.echodot69420.fakeartist;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.WebSocketRoute;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The lobby page shows live game state, shares the code, and surfaces WebSocket errors. */
class LobbyPageIT extends BrowserTestBase {
    @Autowired
    private GameService games;

    @Tag("FR-06")
    @Test
    void lobbyShowsCodeThemePlayersAndHost() {
        hostViaUi(page, "Alice", "Space Race");
        assertThat(page.locator("#conn-text")).hasText("Connected");

        String code = page.locator("#code-value").textContent();
        assertEquals("Space Race", games.getGame(code).theme);
        assertThat(page.locator("#theme-value")).hasText("Space Race");
        assertThat(page.locator("#player-list li")).hasText(new String[] {"Alice (you)"});
        assertThat(page.locator("#player-list li.host")).hasText("Alice (you)");
        assertThat(page.locator("#start-btn")).isDisabled(); // fewer than 3 players
    }

    @Tag("FR-07")
    @Tag("FR-07-03")
    @Test
    void secondPlayerAppearsWithoutReload() {
        hostViaUi(page, "Alice", "Space");
        assertThat(page.locator("#conn-text")).hasText("Connected");
        String code = page.locator("#code-value").textContent();

        Page bob = newPage();
        submitJoin(bob, "Bob", code);
        bob.waitForURL("**/lobby");

        assertThat(page.locator("#player-list li")).hasText(new String[] {"Alice (you)", "Bob"});
        assertThat(bob.locator("#player-list li")).hasText(new String[] {"Alice", "Bob (you)"});
    }

    @Tag("FR-13")
    @Test
    void copyButtonCopiesCodeAndConfirms() {
        page = newPage(new Browser.NewContextOptions()
                .setPermissions(List.of("clipboard-read", "clipboard-write")));
        hostViaUi(page, "Alice", "Space");
        String code = page.locator("#code-value").textContent();

        page.click("#copy-btn");

        assertThat(page.locator("#copy-btn")).hasText("Copied!");
        assertEquals(code, page.evaluate("() => navigator.clipboard.readText()"));
    }

    @Tag("FR-08")
    @Tag("FR-08-01")
    @Tag("FR-14")
    @Test // the UI disables Start for guests, so the test sends the message on the guest's real socket
    void webSocketErrorShowsBanner() {
        String code = (String) games.host(new HostRequest("Host", "Space")).get("code");
        games.connect(new ConnectRequest("Player2", code));

        AtomicReference<WebSocketRoute> toServer = new AtomicReference<>();
        page.routeWebSocket(Pattern.compile("/ws/lobby"), ws -> toServer.set(ws.connectToServer()));
        submitJoin(page, "Guest", code);
        page.waitForURL("**/lobby");
        assertThat(page.locator("#conn-text")).hasText("Connected");

        toServer.get().send("{\"type\":\"start\"}");

        assertThat(page.locator("#banner")).isVisible();
        assertThat(page.locator("#banner")).hasText("Only the host can start the game.");
    }
}
