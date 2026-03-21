package com.fightmind.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for all custom application exceptions.
 * Allows GlobalExceptionHandler to catch them all generically if needed.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ApiException(String message, Throwable cause, HttpStatus status) {
        super(message, cause);
        this.status = status;
    }
}
