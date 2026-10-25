package com.github.echodot69420.fakeartist;

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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FakeArtistApplicationTests {
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void healthEndpointReturnsApplicationStatus() {
        ResponseEntity<Map> response = restTemplate.getForEntity(baseUrl("/health"), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("status", "ok");
        assertThat(response.getBody()).containsKeys("service", "games", "players");
    }

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

    // Performance threshold: 20/20 valid `/host` requests must return HTTP 200 in under 500 ms each on the test environment.
    @Test
    void hostEndpointRespondsWithinPerformanceThreshold() {
        post("/host", Map.of(
                "username", "Warmup",
                "theme", "Space"
        ));

        for (int i = 0; i < 20; i++) {
            long start = System.nanoTime();
            ResponseEntity<Map> response = post("/host", Map.of(
                    "username", "PerfUser" + i,
                    "theme", "Space"
            ));
            long elapsedNanos = System.nanoTime() - start;

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(elapsedNanos)
                    .as("request %d response time", i + 1)
                    .isLessThan(500_000_000L);
        }
    }

    @Test
    void duplicateUsernameIsRejectedWhenJoiningGame() {
        ResponseEntity<Map> hostResponse = post("/host", Map.of(
                "username", "HostUser",
                "theme", "Ocean"
        ));
        String code = (String) hostResponse.getBody().get("code");

        ResponseEntity<Map> firstJoin = post("/connect", Map.of(
                "username", "GuestUser",
                "code", code
        ));
        assertThat(firstJoin.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map> duplicateJoin = post("/connect", Map.of(
                "username", "GuestUser",
                "code", code
        ));

        assertThat(duplicateJoin.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(duplicateJoin.getBody()).containsEntry("detail", "That username is taken in this game.");
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
