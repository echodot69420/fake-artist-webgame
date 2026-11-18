package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "fakeartist.features.round-results=true")
class LobbyHandlerFeatureFlagConfigurationTests {
    @Autowired
    private FeatureFlags featureFlags;

    // No requirement: a config override enables the round-results message.
    @Test
    void configurationOverrideEnablesRoundResultsMessage() throws Exception {
        assertThat(featureFlags.roundResults()).isTrue();

        LobbyHandlerFeatureFlagTests.Fixture fixture =
                LobbyHandlerFeatureFlagTests.establishSessions(featureFlags);
        LobbyHandlerFeatureFlagTests.completeTopicSubmission(fixture);

        assertThat(LobbyHandlerFeatureFlagTests.payloadsFrom(fixture.sessions()))
                .anyMatch(payload -> payload.contains("\"type\":\"round_results\""));
    }
}
