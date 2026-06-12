package com.example.assistant.exception;

public class ModelCallException extends RuntimeException {
    public ModelCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
