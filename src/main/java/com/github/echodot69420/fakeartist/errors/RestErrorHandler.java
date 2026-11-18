package com.github.echodot69420.fakeartist.errors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class RestErrorHandler {
  private static final Logger log = LoggerFactory.getLogger(RestErrorHandler.class);

  @ExceptionHandler(ApiException.class)
  ResponseEntity<ErrorBody> api(ApiException e){
    log.info("API error: {}", e.code());

    return body(e.code(), e.getMessage());
  }

  private static final Map<String, ErrorCode> FIELD_CODES = Map.of(
    "username",  ErrorCode.INVALID_USERNAME,
    "code",      ErrorCode.INVALID_CODE,
    "theme",     ErrorCode.INVALID_THEME,
    "player_id", ErrorCode.MALFORMED_REQUEST);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ErrorBody> invalid(MethodArgumentNotValidException e) {
    FieldError field = e.getBindingResult().getFieldError();

    ErrorCode code = field == null ? ErrorCode.MALFORMED_REQUEST : FIELD_CODES.getOrDefault(field.getField(), ErrorCode.MALFORMED_REQUEST);
    log.info("Validation failed: field={} code={}", field == null ? "-" : field.getField(), code);
    return body(code, code.defaultMessage);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, HttpMediaTypeNotSupportedException.class})
  ResponseEntity<ErrorBody> malformed(Exception e) {
    return body(ErrorCode.MALFORMED_REQUEST, ErrorCode.MALFORMED_REQUEST.defaultMessage);
  }

  // Unknown paths (e.g. the browser's automatic /favicon.ico request) are a normal 404, not a server bug.
  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ErrorBody> notFound(NoResourceFoundException e) {
    log.debug("No resource: {}", e.getResourcePath());
    return body(ErrorCode.RESOURCE_NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND.defaultMessage());
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ErrorBody> unexpected(Exception e) {
    log.error("Unhandled error", e);

    return body(ErrorCode.INTERNAL, ErrorCode.INTERNAL.defaultMessage);
  }


  private ResponseEntity<ErrorBody> body(ErrorCode code, String detail) {
    return ResponseEntity.status(code.status).body(new ErrorBody(code.name(), detail));
  }

  record ErrorBody(String code, String detail) {}
}
