package com.github.echodot69420.fakeartist;

import com.github.echodot69420.fakeartist.errors.ErrorCode;
import com.microsoft.playwright.Route;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The home page shows REST errors to the player: the server's {@code detail} goes in the status line,
 * and the {@code code} decides which field the message appears under. Cases the real server can't
 * produce from the UI (client-side checks block them, or the network fails) use stubbed responses.
 */
class HomePageIT extends BrowserTestBase {
    @Autowired
    private GameService games;

    // ---------- errors from the real server ----------

    @Tag("FR-02")
    @Tag("FR-02-05")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test
    void takenUsernameShowsErrorUnderUsername() {
        String code = newGame("Alice");

        submitJoin(page, "alice", code);

        String message = ErrorCode.USERNAME_TAKEN.defaultMessage();
        assertThat(page.locator("#username-error")).hasText(message);
        assertThat(page.locator("#status")).hasText(message);
        assertThat(page.locator("#code-error")).isEmpty();
    }

    @Tag("FR-02")
    @Tag("FR-02-02")
    @Tag("FR-14")
    @Test
    void unknownGameShowsErrorUnderCode() {
        submitJoin(page, "Bob", "ZZZ0"); // '0' is never generated, so this code cannot exist

        String message = ErrorCode.GAME_NOT_FOUND.defaultMessage();
        assertThat(page.locator("#code-error")).hasText(message);
        assertThat(page.locator("#status")).hasText(message);
    }

    @Tag("FR-02")
    @Tag("FR-02-04")
    @Tag("FR-05")
    @Tag("FR-14")
    @Test
    void fullGameShowsServerMessage() {
        String code = newGame("Host");
        for (int i = 2; i <= 10; i++) games.connect(new ConnectRequest("Player" + i, code));

        submitJoin(page, "Player11", code);

        assertThat(page.locator("#status")).hasText(ErrorCode.GAME_FULL.defaultMessage());
        assertThat(page.locator("#join-btn")).isEnabled();
    }

    @Tag("FR-02")
    @Tag("FR-02-03")
    @Tag("FR-14")
    @Test
    void startedGameShowsServerMessage() {
        String code = newGame("Host");
        games.getGame(code).collectingTopics = true;

        submitJoin(page, "Bob", code);

        assertThat(page.locator("#status")).hasText(ErrorCode.GAME_STARTED.defaultMessage());
    }

    // ---------- stubbed responses ----------

    @Tag("FR-14")
    @Test // the client caps themes at 20 characters, so stub the server's rejection
    void serverThemeErrorShowsUnderTheme() {
        page.route("**/host", route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(422)
                .setContentType("application/json")
                .setBody("{\"code\":\"INVALID_THEME\",\"detail\":\"Theme rejected by server.\"}")));

        page.navigate("/");
        page.fill("#username", "Alice");
        page.fill("#theme", "Space");
        page.click("#host-btn");

        assertThat(page.locator("#theme-error")).hasText("Theme rejected by server.");
        assertThat(page.locator("#host-btn")).isEnabled();
    }

    @Tag("FR-14")
    @Test
    void unreachableServerShowsConnectionMessage() {
        page.route("**/connect", Route::abort);

        submitJoin(page, "Bob", "ABCD");

        assertThat(page.locator("#status"))
                .hasText("Can't reach the server. Check your connection and try again.");
    }

    @Tag("FR-14")
    @Tag("NFR-04")
    @Test // a non-JSON error body still gives a readable message, not a raw error
    void malformedErrorBodyShowsFallbackMessage() {
        page.route("**/connect", route -> route.fulfill(new Route.FulfillOptions()
                .setStatus(500)
                .setContentType("text/html")
                .setBody("<h1>Internal Server Error</h1>")));

        submitJoin(page, "Bob", "ABCD");

        assertThat(page.locator("#status")).hasText(ErrorCode.INTERNAL.defaultMessage());
    }

    // ---------- client-side checks ----------

    @Tag("FR-02")
    @Tag("FR-02-01")
    @Test
    void shortCodeIsRejectedWithoutRequest() {
        List<String> connectRequests = new ArrayList<>();
        page.onRequest(request -> {
            if (request.url().endsWith("/connect")) connectRequests.add(request.url());
        });

        submitJoin(page, "Bob", "AB");

        assertThat(page.locator("#code-error")).hasText("Codes are exactly 4 characters.");
        assertTrue(connectRequests.isEmpty(), "no /connect request should be sent");
    }

    @Tag("FR-02")
    @Tag("FR-02-01")
    @Test
    void codeInputIsUppercasedAndFiltered() {
        page.navigate("/");
        page.locator("#game-code").pressSequentially("a-b1");

        assertThat(page.locator("#game-code")).hasValue("AB1");
    }

    @Tag("FR-01")
    @Tag("FR-01-05")
    @Test
    void hostingOpensLobbyForNewGame() {
        hostViaUi(page, "Alice", "Space");

        String code = page.locator("#code-value").textContent();
        assertEquals(4, code.length());
        assertEquals("Alice", games.getGame(code).players.get(0).username());
    }

    private String newGame(String host) {
        return (String) games.host(new HostRequest(host, "Space")).get("code");
    }
}
