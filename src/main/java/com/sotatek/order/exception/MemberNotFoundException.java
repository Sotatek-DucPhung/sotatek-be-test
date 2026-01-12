package com.sotatek.order.exception;

public class MemberNotFoundException extends OrderException {

    public MemberNotFoundException(Long memberId) {
        super("MEMBER_NOT_FOUND", "Member not found: id=" + memberId);
    }
}
