package com.github.echodot69420.fakeartist.errors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** R-P5 and R-L3: the catch-all handler for real bugs. Traces to NFR-12. */
@ExtendWith(OutputCaptureExtension.class)
class RestErrorHandlerUnexpectedErrorTests {

    @RestController
    static class BrokenController {
        @GetMapping("/boom")
        String boom() {
            throw new IllegalStateException("secret-internal-state");
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new BrokenController())
            .setControllerAdvice(new RestErrorHandler())
            .build();

    // R-P5 (NFR-12): unexpected exceptions return 500 INTERNAL without leaking their message.
    @Test
    void unexpectedExceptionReturnsInternalWithoutLeakingMessage() throws Exception {
        mvc.perform(get("/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL"))
                .andExpect(jsonPath("$.detail").value(ErrorCode.INTERNAL.defaultMessage()))
                .andExpect(content().string(not(containsString("secret-internal-state"))));
    }

    // R-L3 (NFR-12): real bugs are logged at ERROR with a stack trace.
    @Test
    void unexpectedExceptionIsLoggedAtErrorWithStackTrace(CapturedOutput output) throws Exception {
        mvc.perform(get("/boom"));

        assertThat(output).contains("ERROR").contains("Unhandled error")
                .contains("java.lang.IllegalStateException");
    }
}
