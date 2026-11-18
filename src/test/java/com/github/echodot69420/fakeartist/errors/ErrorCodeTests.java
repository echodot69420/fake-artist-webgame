package com.github.echodot69420.fakeartist.errors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Contract tests for the shared error list. Traces to FR-14 and NFR-14. */
class ErrorCodeTests {

    // E-1 (FR-14): every error has a message a user can read.
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCodeHasANonBlankDefaultMessage(ErrorCode code) {
        assertThat(code.defaultMessage()).isNotBlank();
    }

    // E-2 (NFR-14): every error maps to an HTTP error status.
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCodeMapsToAnErrorStatus(ErrorCode code) {
        assertThat(code.status.is4xxClientError() || code.status.is5xxServerError()).isTrue();
    }

    // E-3 (NFR-14): only genuine server-side failures are 5xx.
    @Test
    void onlyServerFailuresAre5xx() {
        assertThat(Arrays.stream(ErrorCode.values()).filter(code -> code.status.is5xxServerError()))
                .containsExactlyInAnyOrderElementsOf(
                        EnumSet.of(ErrorCode.INTERNAL, ErrorCode.NO_CODES_AVAILABLE));
    }
}
