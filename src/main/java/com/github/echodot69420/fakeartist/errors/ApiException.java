package com.github.echodot69420.fakeartist.errors;

public final class ApiException extends RuntimeException {
    private final ErrorCode code;

    public ApiException(ErrorCode code, String message){
      super(message);
      this.code = code;
    }

    public ApiException(ErrorCode code) {
      this(code, code.defaultMessage);
    }

    public ErrorCode code() {
      return code;
    }
}
