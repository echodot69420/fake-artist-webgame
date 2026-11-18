package com.github.echodot69420.fakeartist;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared setup for real-browser tests: starts the app on a random port and drives headless Chromium
 * with Playwright. Each test gets a fresh browser context (own cookies and sessionStorage), so tests
 * never share a player session. Extra contexts from {@link #newPage()} act as additional players.
 * All browser tests are tagged {@code browser} so they can be skipped with {@code -DexcludedGroups=browser}.
 */
@Tag("browser")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class BrowserTestBase {
    private static Playwright playwright;
    private static Browser browser;

    @LocalServerPort
    private int port;

    private final List<BrowserContext> contexts = new ArrayList<>();
    /** The first player's page, opened before each test. */
    protected Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch();
    }

    @AfterAll
    static void closeBrowser() {
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void openPage() {
        page = newPage();
    }

    @AfterEach
    void closeContexts() {
        contexts.forEach(BrowserContext::close);
        contexts.clear();
    }

    /** A page in a new, isolated browser context: a separate player. */
    protected Page newPage() {
        return newPage(new Browser.NewContextOptions());
    }

    protected Page newPage(Browser.NewContextOptions options) {
        BrowserContext context = browser.newContext(options.setBaseURL(url("")));
        context.setDefaultTimeout(10_000);
        contexts.add(context);
        return context.newPage();
    }

    protected String url(String path) {
        return "http://localhost:" + port + path;
    }

    // ---------- home page actions ----------

    /** Hosts a game through the home page and waits for the lobby to load. */
    protected static void hostViaUi(Page page, String username, String theme) {
        page.navigate("/");
        page.fill("#username", username);
        page.fill("#theme", theme);
        page.click("#host-btn");
        page.waitForURL("**/lobby");
    }

    /** Fills in the join form and clicks Join, without waiting for the outcome. */
    protected static void submitJoin(Page page, String username, String code) {
        page.navigate("/");
        page.fill("#username", username);
        page.fill("#game-code", code);
        page.click("#join-btn");
    }
}
