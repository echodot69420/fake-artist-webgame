package com.github.echodot69420.fakeartist;

import org.springframework.http.HttpStatus;

final class ApiException extends RuntimeException {
    final HttpStatus status;

    ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
