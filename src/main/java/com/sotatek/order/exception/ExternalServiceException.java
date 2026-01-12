package com.sotatek.order.exception;

public class ExternalServiceException extends OrderException {

    public ExternalServiceException(String message) {
        super("EXTERNAL_SERVICE_UNAVAILABLE", message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super("EXTERNAL_SERVICE_UNAVAILABLE", message, cause);
    }
}
