package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Response-time check for POST /host. Tagged {@code performance} so it can be run or skipped on its own
 * (for example {@code mvn verify -DexcludedGroups=performance} on a slow machine).
 */
@Tag("performance")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HostEndpointPerformanceIT {
    @Autowired
    private TestRestTemplate restTemplate;

    // Threshold: 20/20 valid /host requests return HTTP 200 in under 500 ms each (NFR-11, partial).
    @Tag("NFR-11")
    @Test
    void hostEndpointRespondsWithinPerformanceThreshold() {
        host("Warmup");

        for (int i = 0; i < 20; i++) {
            long start = System.nanoTime();
            ResponseEntity<Map> response = host("PerfUser" + i);
            long elapsedNanos = System.nanoTime() - start;

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(elapsedNanos)
                    .as("request %d response time", i + 1)
                    .isLessThan(500_000_000L);
        }
    }

    private ResponseEntity<Map> host(String username) {
        return restTemplate.postForEntity("/host", Map.of("username", username, "theme", "Space"), Map.class);
    }
}
