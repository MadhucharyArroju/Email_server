package com.example.document.exception;

/**
 * Thrown when an uploaded file fails validation, e.g. unsupported
 * content type, empty file, or exceeding the 100 KB size limit.
 * Mapped to HTTP 400 by {@link GlobalExceptionHandler}.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
