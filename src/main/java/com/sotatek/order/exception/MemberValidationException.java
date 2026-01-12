package com.sotatek.order.exception;

public class MemberValidationException extends OrderException {

    public MemberValidationException(String message) {
        super("MEMBER_VALIDATION_ERROR", message);
    }
}
