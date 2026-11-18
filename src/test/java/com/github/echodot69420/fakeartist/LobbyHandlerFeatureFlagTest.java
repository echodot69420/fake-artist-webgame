package com.github.echodot69420.fakeartist;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The experimental round-results message is only sent when its feature flag is on. */
class LobbyHandlerFeatureFlagTest {

    @Tag("NON-REQ")
    @Test // touches FR-09-05 and FR-10-03 incidentally
    void disabledRoundResultsFlagDoesNotSendExperimentalMessage() throws Exception {
        List<String> types = typesSentAfterLastTopic(LobbyFixture.flags(false, false));

        assertThat(types).contains("player_popup").doesNotContain("round_results");
    }

    @Tag("NON-REQ")
    @Test
    void enabledRoundResultsFlagSendsExperimentalMessage() throws Exception {
        List<String> types = typesSentAfterLastTopic(LobbyFixture.flags(true, false));

        assertThat(types).contains("round_results");
    }

    /** Puts a three-player lobby in topic collection with two topics in, then submits the last one. */
    static List<String> typesSentAfterLastTopic(FeatureFlags flags) throws Exception {
        LobbyFixture f = LobbyFixture.withPlayers(3, flags);
        f.game().collectingTopics = true;
        f.game().topics.put(f.game().players.get(0).player_id(), "Rockets");
        f.game().topics.put(f.game().players.get(1).player_id(), "Planets");

        f.send(f.sessions().get(2), "{\"type\":\"topic\",\"value\":\"Comets\"}");

        return f.allPayloads().stream().map(payload -> payload.path("type").asText()).toList();
    }
}
