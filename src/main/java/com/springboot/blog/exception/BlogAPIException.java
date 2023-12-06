package com.springboot.blog.exception;

import org.springframework.http.HttpStatus;

public class BlogAPIException extends RuntimeException {

    private HttpStatus status;
    private String message;
    private int errorCode;

    public BlogAPIException(String message) {
        super(message);
        this.status = HttpStatus.INTERNAL_SERVER_ERROR; // default status
        this.message = message;
    }

    public BlogAPIException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.message = message;
    }

    public BlogAPIException(HttpStatus status, String message, int errorCode) {
        super(message);
        this.status = status;
        this.message = message;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    @Override
    public String getMessage() {
        return message;
    }
}

