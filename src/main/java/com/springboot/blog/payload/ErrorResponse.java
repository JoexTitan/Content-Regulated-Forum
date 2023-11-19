package com.springboot.blog.payload;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse implements Serializable {

    private final LocalDateTime timestamp;
    private final String message;
    private final String errorCode;
    private final List<SubError> subErrors;

    public ErrorResponse(LocalDateTime timestamp, String message, String errorCode, List<SubError> subErrors) {
        this.timestamp = timestamp;
        this.message = message;
        this.errorCode = errorCode;
        this.subErrors = subErrors;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public List<SubError> getSubErrors() {
        return subErrors;
    }
}

