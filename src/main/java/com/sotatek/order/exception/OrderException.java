package com.sotatek.order.exception;

public abstract class OrderException extends RuntimeException {

    private final String code;

    protected OrderException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected OrderException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
