package com.github.echodot69420.fakeartist;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Feature flags bind from application properties: off by default, and overridable per environment. */
@SpringBootTest
class FeatureFlagsIT {
    @Autowired
    private FeatureFlags featureFlags;

    @Tag("NON-REQ")
    @Test
    void roundResultsIsOffByDefault() {
        assertThat(featureFlags.roundResults()).isFalse();
    }

    @Tag("FR-16")
    @Test
    void kickPlayerIsOffByDefault() {
        assertThat(featureFlags.kickPlayer()).isFalse();
    }

    @Nested
    @TestPropertySource(properties = {
            "fakeartist.features.round-results=true",
            "fakeartist.features.kick-player=true"
    })
    class WhenOverridden {
        @Autowired
        private FeatureFlags overridden;

        @Tag("NFR-13")
        @Test
        void propertiesTurnFlagsOn() {
            assertThat(overridden.roundResults()).isTrue();
            assertThat(overridden.kickPlayer()).isTrue();
        }
    }
}
