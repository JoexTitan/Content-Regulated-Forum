package com.springboot.blog.exception;

import org.springframework.http.HttpStatus;

public class BlogAPIException extends RuntimeException {

    private final HttpStatus status;
    private final String message;

    // Constructor for cases where only a message is provided
    public BlogAPIException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR; // default status
        this.message = message;
    }

    // Constructor for cases where both status and message are provided
    public BlogAPIException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

