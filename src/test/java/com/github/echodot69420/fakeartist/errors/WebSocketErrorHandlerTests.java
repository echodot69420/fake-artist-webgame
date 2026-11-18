package com.github.echodot69420.fakeartist.errors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for WebSocketErrorHandler. Traces to FR-14, NFR-07 and NFR-12. */
@ExtendWith(OutputCaptureExtension.class)
class WebSocketErrorHandlerTests {
    private final ObjectMapper mapper = new ObjectMapper();
    private final WebSocketErrorHandler handler = new WebSocketErrorHandler(mapper);
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        session = mock(WebSocketSession.class);
        when(session.isOpen()).thenReturn(true);
    }

    // W-1 (FR-14): a successful action sends nothing.
    @Test
    void successfulActionSendsNoErrorFrame() throws Exception {
        handler.run(session, "ping", () -> { });

        verify(session, never()).sendMessage(any());
    }

    // W-2 (FR-14): an ApiException becomes an error frame with its code and default message.
    @Test
    void apiExceptionBecomesErrorFrameWithDefaultMessage() throws Exception {
        handler.run(session, "start", () -> { throw new ApiException(ErrorCode.NOT_HOST); });

        JsonNode frame = onlyFrame();
        assertThat(frame.path("type").asText()).isEqualTo("error");
        assertThat(frame.path("code").asText()).isEqualTo("NOT_HOST");
        assertThat(frame.path("detail").asText()).isEqualTo(ErrorCode.NOT_HOST.defaultMessage());
    }

    // W-3 (FR-14): a custom message overrides the default.
    @Test
    void apiExceptionCustomMessageIsUsedAsDetail() throws Exception {
        handler.run(session, "kick", () -> { throw new ApiException(ErrorCode.NOT_HOST, "custom"); });

        assertThat(onlyFrame().path("detail").asText()).isEqualTo("custom");
    }

    // W-4 (NFR-12): unexpected errors become INTERNAL, are logged, and do not leak their message.
    @Test
    void unexpectedRuntimeExceptionBecomesInternalWithoutLeakingMessage(CapturedOutput output) throws Exception {
        handler.run(session, "topic", () -> { throw new IllegalStateException("secret-internal-state"); });

        JsonNode frame = onlyFrame();
        assertThat(frame.path("code").asText()).isEqualTo("INTERNAL");
        assertThat(frame.toString()).doesNotContain("secret-internal-state");
        assertThat(output).contains("ERROR").contains("Unhandled WS error");
    }

    // W-5 (NFR-07): checked exceptions are handled too and never escape run().
    @Test
    void checkedExceptionBecomesInternalAndDoesNotPropagate() throws Exception {
        assertThatCode(() -> handler.run(session, "topic", () -> { throw new IOException("disk"); }))
                .doesNotThrowAnyException();

        assertThat(onlyFrame().path("code").asText()).isEqualTo("INTERNAL");
    }

    // W-6 (NFR-07): a closed session is skipped quietly.
    @Test
    void closedSessionIsNotSentTo() throws Exception {
        when(session.isOpen()).thenReturn(false);

        assertThatCode(() -> handler.send(session, ErrorCode.NOT_HOST, "x")).doesNotThrowAnyException();
        verify(session, never()).sendMessage(any());
    }

    // W-7 (NFR-07): a failed send is swallowed.
    @Test
    void sendFailureIsSwallowed() throws Exception {
        doThrow(new IOException("broken pipe")).when(session).sendMessage(any());

        assertThatCode(() -> handler.run(session, "start",
                () -> { throw new ApiException(ErrorCode.NOT_HOST); }))
                .doesNotThrowAnyException();
    }

    // W-8 (NFR-07): a recoverable error keeps the socket open.
    @Test
    void errorsNeverCloseTheSession() throws Exception {
        handler.run(session, "start", () -> { throw new ApiException(ErrorCode.NOT_HOST); });
        handler.run(session, "topic", () -> { throw new IllegalStateException("bug"); });

        verify(session, never()).close();
        verify(session, never()).close(any(CloseStatus.class));
    }

    // W-9 (FR-14, NFR-14): every code is sent by its enum name.
    @ParameterizedTest
    @EnumSource(ErrorCode.class)
    void everyCodeIsSentByName(ErrorCode code) throws Exception {
        handler.send(session, code, code.defaultMessage());

        assertThat(onlyFrame().path("code").asText()).isEqualTo(code.name());
    }

    private JsonNode onlyFrame() throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        return mapper.readTree(captor.getValue().getPayload());
    }
}
