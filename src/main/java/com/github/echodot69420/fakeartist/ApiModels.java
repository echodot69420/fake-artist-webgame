package com.github.echodot69420.fakeartist;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record HostRequest(
        @Size(min = 1, max = 16) @Pattern(regexp = "[A-Za-z0-9]+") String username,
        @Size(min = 1, max = 20) String theme) {}

record ConnectRequest(
        @Size(min = 1, max = 16) @Pattern(regexp = "[A-Za-z0-9]+") String username,
        @Size(min = 1, max = 4) String code) {}

record QuitRequest(@Size(min = 1, max = 4) String code, @Size(min = 1) String player_id) {}

record GamePlayer(String player_id, String username, boolean is_host) {}
