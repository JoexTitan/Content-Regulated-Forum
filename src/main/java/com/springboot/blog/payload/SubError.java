package com.springboot.blog.payload;

import java.io.Serializable;

public class SubError implements Serializable {
    private final String field;
    private final String message;

    public SubError(String field, String message) {
        this.field = field;
        this.message = message;
    }

    public SubError(String field, String message, String errorCode) {
        this.field = field;
        this.message = message;
    }

    public String getField() {
        return field;
    }

    public String getMessage() {
        return message;
    }
}