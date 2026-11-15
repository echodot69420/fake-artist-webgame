package com.github.echodot69420.fakeartist;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "fakeartist.features")
class FeatureFlags {
    private boolean roundResults;
    private boolean kickPlayer;

    boolean roundResults() {
        return roundResults;
    }

    void setRoundResults(boolean roundResults) {
        this.roundResults = roundResults;
    }

    boolean kickPlayer() {
        return kickPlayer;
    }

    void setKickPlayer(boolean kickPlayer) {
        this.kickPlayer = kickPlayer;
    }
}
