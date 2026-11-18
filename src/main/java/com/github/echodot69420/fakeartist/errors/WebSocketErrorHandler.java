package com.github.echodot69420.fakeartist.errors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

@Component
public class WebSocketErrorHandler {
  private static final Logger log = LoggerFactory.getLogger(WebSocketErrorHandler.class);
  private final ObjectMapper mapper;

  public WebSocketErrorHandler(ObjectMapper mapper) { this.mapper = mapper; }

  /** Runs one client message's handling and turns any failure into an error frame. */
  public void run(WebSocketSession session, String messageType, ThrowingRunnable action) {
    try {
      action.run();
    } catch (ApiException e) {
      log.info("WS error: type={} code={}", messageType, e.code());
      send(session, e.code(), e.getMessage());
    } catch (Exception e) {
      log.error("Unhandled WS error: type={}", messageType, e);
      send(session, ErrorCode.INTERNAL, ErrorCode.INTERNAL.defaultMessage);
    }
  }

  public void send(WebSocketSession session, ErrorCode code, String detail) {
    if (!session.isOpen()) return;
    try {
      String json = mapper.writeValueAsString(
        Map.of("type", "error", "code", code.name(), "detail", detail));
      synchronized (session) { session.sendMessage(new TextMessage(json)); }
    } catch (IOException e) {
      log.debug("Could not deliver WS error frame", e);   // client is already gone; the heartbeat will clean up
    }
  }

  @FunctionalInterface public interface ThrowingRunnable { void run() throws Exception; }
}
