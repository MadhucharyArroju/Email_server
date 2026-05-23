package com.example.document.exception;

/**
 * Thrown when a file cannot be written to or read from disk.
 * Mapped to HTTP 500 by {@link GlobalExceptionHandler}.
 */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileStorageException(String message) {
        super(message);
    }
}
