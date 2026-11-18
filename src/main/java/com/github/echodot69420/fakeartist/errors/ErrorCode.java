package com.github.echodot69420.fakeartist.errors;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  // lookup
  NO_CODES_AVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "No game codes available right now."),
  GAME_NOT_FOUND(HttpStatus.NOT_FOUND, "Game not found."),
  PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "Player is not in this game."),
  // join
  GAME_FULL(HttpStatus.CONFLICT, "Game is full."),
  GAME_STARTED(HttpStatus.CONFLICT, "Game already started."),
  USERNAME_TAKEN(HttpStatus.CONFLICT, "That username is taken in this game."),
  // input validation
  INVALID_CODE(HttpStatus.UNPROCESSABLE_ENTITY, "Game code must be 4 characters."),
  INVALID_USERNAME(HttpStatus.UNPROCESSABLE_ENTITY, "Username must be 1-16 alphanumeric characters."),
  INVALID_THEME(HttpStatus.UNPROCESSABLE_ENTITY, "Theme must be 1-20 characters."),
  INVALID_TOPIC(HttpStatus.UNPROCESSABLE_ENTITY, "Topic must be 1-40 characters."),
  // authorization / game rules
  NOT_HOST(HttpStatus.FORBIDDEN, "Only the host can do that."),
  NOT_ENOUGH_PLAYERS(HttpStatus.CONFLICT, "Need 3+ players to start the game."),
  TOPIC_WINDOW_CLOSED(HttpStatus.CONFLICT, "The topic selection window is closed."),
  INVALID_KICK_TARGET(HttpStatus.UNPROCESSABLE_ENTITY, "Only a non-host player can be kicked."),
  FEATURE_DISABLED(HttpStatus.FORBIDDEN, "This feature is disabled."),
  // protocol
  RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Not found."),
  MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "Request must be valid JSON."),
  UNKNOWN_MESSAGE_TYPE(HttpStatus.BAD_REQUEST, "Unknown message type."),
  INTERNAL(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again.");

  final HttpStatus status;
  final String defaultMessage;
  ErrorCode(HttpStatus status, String defaultMessage) {
    this.status = status;
    this.defaultMessage = defaultMessage;
  }

  public String defaultMessage() {
    return defaultMessage;
  }
}