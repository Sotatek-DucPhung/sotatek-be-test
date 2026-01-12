package com.sotatek.order.service.external.adapter;

import com.sotatek.order.service.external.MemberServiceClient;
import com.sotatek.order.service.external.dto.MemberDto;
import com.sotatek.order.exception.ExternalServiceException;
import com.sotatek.order.exception.MemberNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

/**
 * Mock implementation of Member Service Client
 * Returns hardcoded member data for testing and development
 * Use @Primary to make this the default implementation
 */
@Component
@ConditionalOnProperty(name = "external.mock.enabled", havingValue = "true", matchIfMissing = true)
@Primary
@Slf4j
public class MockMemberServiceClient implements MemberServiceClient {

    @Override
    @CircuitBreaker(name = "memberService")
    @Retry(name = "memberService")
    public MemberDto getMember(Long memberId) {
        log.info("[MOCK] Getting member: memberId={}", memberId);

        if (memberId == 7777L) {
            log.warn("[MOCK] Member service unavailable: memberId={}", memberId);
            throw new ExternalServiceException("Member service unavailable");
        }

        if (memberId == 5555L) {
            log.warn("[MOCK] Member service timeout: memberId={}", memberId);
            throw new ResourceAccessException("Member service timeout");
        }

        // Simulate some members being inactive or not found
        if (memberId == 9999L) {
            log.warn("[MOCK] Member not found: memberId={}", memberId);
            throw new MemberNotFoundException(memberId);
        }

        if (memberId == 8888L) {
            log.warn("[MOCK] Member is INACTIVE: memberId={}", memberId);
            return MemberDto.builder()
                    .id(memberId)
                    .name("Inactive Member")
                    .email("inactive@example.com")
                    .status("INACTIVE")
                    .grade("BRONZE")
                    .build();
        }

        // Return mock active member for all other IDs
        log.info("[MOCK] Returning mock member: memberId={}, status=ACTIVE", memberId);
        return MemberDto.builder()
                .id(memberId)
                .name("Mock Member " + memberId)
                .email("member" + memberId + "@example.com")
                .status("ACTIVE")
                .grade("GOLD")
                .build();
    }
}
