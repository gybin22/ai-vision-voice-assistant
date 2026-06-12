package com.example.assistant.exception;

public class CostLimitExceededException extends RuntimeException {
    private final String code;

    public CostLimitExceededException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
