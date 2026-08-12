package org.sharingbridge.notification.service;

import org.springframework.http.HttpStatus;

public class NotifyException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public NotifyException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
