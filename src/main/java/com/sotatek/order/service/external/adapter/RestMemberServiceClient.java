package com.sotatek.order.service.external.adapter;

import com.sotatek.order.service.external.MemberServiceClient;
import com.sotatek.order.service.external.dto.MemberDto;
import com.sotatek.order.exception.ExternalServiceException;
import com.sotatek.order.exception.MemberNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "external.mock.enabled", havingValue = "false")
@Slf4j
@RequiredArgsConstructor
public class RestMemberServiceClient implements MemberServiceClient {

    private final RestTemplate restTemplate;

    @Value("${external.member-service.url}")
    private String baseUrl;

    @Override
    @CircuitBreaker(name = "memberService")
    @Retry(name = "memberService")
    public MemberDto getMember(Long memberId) {
        String url = baseUrl + "/api/members/" + memberId;

        try {
            MemberDto member = restTemplate.getForObject(url, MemberDto.class);
            if (member == null) {
                throw new ExternalServiceException("Member service returned empty response: memberId=" + memberId);
            }
            return member;
        } catch (RestClientResponseException ex) {
            if (ex.getRawStatusCode() == 404) {
                throw new MemberNotFoundException(memberId);
            }
            log.error("Member service error: status={}, body={}", ex.getRawStatusCode(), ex.getResponseBodyAsString());
            throw new ExternalServiceException("Member service error: status=" + ex.getRawStatusCode(), ex);
        } catch (RestClientException ex) {
            log.error("Member service call failed: {}", ex.getMessage());
            throw new ExternalServiceException("Member service call failed: " + ex.getMessage(), ex);
        }
    }
}
