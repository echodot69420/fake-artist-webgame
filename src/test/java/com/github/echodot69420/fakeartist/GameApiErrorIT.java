package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Error responses of the game HTTP API over the full path (@Valid, RestErrorHandler advice and service):
 * input boundary values, game-state conflicts, malformed requests, and how errors are logged.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class GameApiErrorIT {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private GameService games;

    // ---------- username (1-16, [A-Za-z0-9]) via POST /host ----------

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test // length 0, rejected by @Valid
    void usernameLength0IsRejected() throws Exception {
        expectError(host("", "Space"), 422, "INVALID_USERNAME");
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test // length 1, lower bound
    void usernameLength1IsAccepted() throws Exception {
        host("A", "Space").andExpect(status().isOk());
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test // length 16, upper bound
    void usernameLength16IsAccepted() throws Exception {
        host("A".repeat(16), "Space").andExpect(status().isOk());
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test // length 17, rejected by @Valid
    void usernameLength17IsRejected() throws Exception {
        expectError(host("A".repeat(17), "Space"), 422, "INVALID_USERNAME");
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test // valid length, invalid character
    void usernameWithInvalidCharacterIsRejected() throws Exception {
        expectError(host("!", "Space"), 422, "INVALID_USERNAME");
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test // missing field, rejected by the service
    void missingUsernameIsRejected() throws Exception {
        expectError(postJson("/host", body("theme", "Space")), 422, "INVALID_USERNAME");
    }

    // ---------- theme (1-20 after trim) via POST /host ----------

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-04")
    @Tag("FR-14")
    @Test // length 0, rejected by @Valid
    void themeLength0IsRejected() throws Exception {
        expectError(host("Bob", ""), 422, "INVALID_THEME");
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-04")
    @Tag("FR-14")
    @Test // passes @Size, empty after trim, rejected by the service
    void whitespaceOnlyThemeIsRejected() throws Exception {
        expectError(host("Bob", "   "), 422, "INVALID_THEME");
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-04")
    @Tag("FR-14")
    @Test // length 1, lower bound
    void themeLength1IsAccepted() throws Exception {
        host("Bob", "A").andExpect(status().isOk());
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-04")
    @Tag("FR-14")
    @Test // length 20, upper bound
    void themeLength20IsAccepted() throws Exception {
        host("Bob", "T".repeat(20)).andExpect(status().isOk());
    }

    @Tag("FR-01")
    @Tag("FR-01-01")
    @Tag("FR-04")
    @Tag("FR-14")
    @Test // length 21, rejected by @Valid
    void themeLength21IsRejected() throws Exception {
        expectError(host("Bob", "T".repeat(21)), 422, "INVALID_THEME");
    }

    // ---------- game code (exactly 4, [A-Z0-9]) via POST /connect ----------

    @Tag("FR-02")
    @Tag("FR-02-01")
    @Tag("FR-14")
    @Test // length 3, passes @Size(1,4), rejected by the service
    void codeLength3IsRejected() throws Exception {
        expectError(connect("Bob", "ABC"), 422, "INVALID_CODE");
    }

    @Tag("FR-02")
    @Tag("FR-02-01")
    @Tag("FR-14")
    @Test // length 4, existing game
    void codeLength4IsAccepted() throws Exception {
        connect("Bob", newGame()).andExpect(status().isOk());
    }

    @Tag("FR-02")
    @Tag("FR-02-01")
    @Tag("FR-14")
    @Test // length 5, rejected by @Valid
    void codeLength5IsRejected() throws Exception {
        expectError(connect("Bob", "ABCDE"), 422, "INVALID_CODE");
    }

    @Tag("FR-02")
    @Tag("FR-02-01")
    @Tag("FR-14")
    @Test // lowercase is normalised
    void lowercaseCodeIsAccepted() throws Exception {
        connect("Bob", newGame().toLowerCase()).andExpect(status().isOk());
    }

    @Tag("FR-02")
    @Tag("FR-02-02")
    @Tag("FR-14")
    @Test // valid format, no such game ('0' is never generated, so this code cannot exist)
    void unknownCodeIsNotFound() throws Exception {
        expectError(connect("Bob", "ZZZ0"), 404, "GAME_NOT_FOUND");
    }

    // ---------- game state via POST /connect ----------

    @Tag("FR-02")
    @Tag("FR-02-04")
    @Tag("FR-05")
    @Tag("FR-14")
    @Test // 10th player is the last one allowed
    void tenthPlayerCanJoin() throws Exception {
        String code = gameWithPlayers(9);
        connect("Player10", code).andExpect(status().isOk());
    }

    @Tag("FR-02")
    @Tag("FR-02-04")
    @Tag("FR-05")
    @Tag("FR-14")
    @Test // 11th player is rejected
    void eleventhPlayerIsRejected() throws Exception {
        String code = gameWithPlayers(10);
        expectError(connect("Player11", code), 409, "GAME_FULL");
    }

    @Tag("FR-02")
    @Tag("FR-02-05")
    @Tag("FR-03")
    @Tag("FR-14")
    @Test
    void duplicateUsernameIgnoringCaseIsRejected() throws Exception {
        String code = newGame();
        expectError(connect("HOST", code), 409, "USERNAME_TAKEN");
    }

    @Tag("FR-02")
    @Tag("FR-02-03")
    @Tag("FR-10")
    @Tag("FR-10-05")
    @Tag("FR-14")
    @Test
    void joiningAStartedGameIsRejected() throws Exception {
        String code = newGame();
        games.getGame(code).collectingTopics = true;
        expectError(connect("Bob", code), 409, "GAME_STARTED");
    }

    // ---------- protocol ----------

    @Tag("FR-14")
    @Tag("NFR-01")
    @Test
    void malformedJsonIsRejected() throws Exception {
        expectError(mvc.perform(post("/host").contentType(MediaType.APPLICATION_JSON).content("not json")),
                400, "MALFORMED_REQUEST");
    }

    @Tag("FR-14")
    @Tag("NFR-01")
    @Test
    void emptyBodyIsRejected() throws Exception {
        expectError(mvc.perform(post("/host").contentType(MediaType.APPLICATION_JSON).content("")),
                400, "MALFORMED_REQUEST");
    }

    @Tag("FR-14")
    @Tag("NFR-01")
    @Test
    void nonJsonContentTypeIsRejected() throws Exception {
        expectError(mvc.perform(post("/host").contentType(MediaType.TEXT_PLAIN).content("hello")),
                400, "MALFORMED_REQUEST");
    }

    @Tag("NFR-12")
    @Test // missing static files are a quiet 404
    void missingStaticResourceIsQuiet404(CapturedOutput output) throws Exception {
        expectError(mvc.perform(get("/favicon.ico")), 404, "RESOURCE_NOT_FOUND");
        assertThat(output).doesNotContain("Unhandled error");
    }

    // ---------- logging (NFR-12) ----------

    @Tag("NFR-12")
    @Test // log names the field but never the submitted value
    void validationLogNamesFieldButNotValue(CapturedOutput output) throws Exception {
        host("Secretusername17", "Space").andExpect(status().isOk());
        String rejected = "Rejectedusername1"; // 17 characters
        host(rejected, "Space");

        assertThat(output).contains("field=username").doesNotContain(rejected);
    }

    @Tag("NFR-12")
    @Test // expected errors are logged at INFO without a stack trace
    void expectedErrorIsLoggedWithoutStackTrace(CapturedOutput output) throws Exception {
        String code = gameWithPlayers(10);
        connect("Player11", code);

        assertThat(output).contains("API error: GAME_FULL")
                .doesNotContain("errors.ApiException:")
                .doesNotContain("Unhandled error");
    }

    // ---------- helpers ----------

    private ResultActions host(String username, String theme) throws Exception {
        return postJson("/host", body("username", username, "theme", theme));
    }

    private ResultActions connect(String username, String code) throws Exception {
        return postJson("/connect", body("username", username, "code", code));
    }

    private ResultActions postJson(String path, Map<String, String> body) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)));
    }

    private static Map<String, String> body(String... pairs) {
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) map.put(pairs[i], pairs[i + 1]);
        return map;
    }

    private String newGame() throws Exception {
        String json = host("Host", "Space").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).path("code").asText();
    }

    private String gameWithPlayers(int total) throws Exception {
        String code = newGame();
        for (int i = 2; i <= total; i++) connect("Player" + i, code).andExpect(status().isOk());
        return code;
    }

    private static void expectError(ResultActions result, int status, String code) throws Exception {
        result.andExpect(status().is(status))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
