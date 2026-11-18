package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** End-to-end HTTP tests of the game API happy paths over a real server port. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameApiIT {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    // FR-15: /health reports status, service name, and counts.
    @Tag("FR-15")
    @Test
    void healthEndpointReturnsApplicationStatus() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl("/health"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok");
        assertThat(response.getBody()).containsKeys("service", "games", "players");
    }

    // FR-01-02, FR-01-05, FR-02-06, FR-02-07: host then join returns the shared game.
    @Tag("FR-01")
    @Tag("FR-01-02")
    @Tag("FR-01-05")
    @Tag("FR-02")
    @Tag("FR-02-06")
    @Tag("FR-02-07")
    @Test
    void hostAndConnectFlowCreatesPlayersForGame() {
        ResponseEntity<Map> hostResponse = post("/host", Map.of(
                "username", "Alpha",
                "theme", "Space"
        ));

        assertThat(hostResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(hostResponse.getBody()).containsKeys("code", "theme", "players");

        String code = (String) hostResponse.getBody().get("code");
        assertThat(code).hasSize(4);

        ResponseEntity<Map> connectResponse = post("/connect", Map.of(
                "username", "Bravo",
                "code", code
        ));

        assertThat(connectResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(connectResponse.getBody()).containsEntry("code", code);
        assertThat((List<?>) connectResponse.getBody().get("players")).hasSize(2);

        ResponseEntity<Map> gameResponse = restTemplate.getForEntity(baseUrl("/game/" + code), Map.class);
        assertThat(gameResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(gameResponse.getBody()).containsEntry("code", code);
        assertThat((List<?>) gameResponse.getBody().get("players")).hasSize(2);
    }

    // FR-01-03, FR-01-05: a valid host request creates a lobby game.
    @Tag("FR-01")
    @Tag("FR-01-03")
    @Tag("FR-01-05")
    @Test
    void hostEndpointCreatesGameForValidInput() {
        ResponseEntity<Map> response = post("/host", Map.of(
                "username", "HostPlayer",
                "theme", "Space"
        ));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("theme", "Space");
        assertThat((String) response.getBody().get("code")).hasSize(4);

        List<Map<String, Object>> players = (List<Map<String, Object>>) response.getBody().get("players");
        assertThat(players).hasSize(1);
        assertThat(players.get(0))
                .containsEntry("username", "HostPlayer")
                .containsEntry("is_host", true);
    }

    private String baseUrl(String path) {
        return "http://localhost:" + port + path;
    }

    private ResponseEntity<Map> post(String path, Map<String, String> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
        return restTemplate.postForEntity(baseUrl(path), request, Map.class);
    }
}
