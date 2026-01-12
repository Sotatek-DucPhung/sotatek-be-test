package com.sotatek.order.service.external;

import com.sotatek.order.service.external.dto.MemberDto;

/**
 * Adapter interface for Member Service
 * Allows easy swapping between mock and real implementations
 */
public interface MemberServiceClient {

    /**
     * Get member by ID from Member Service
     *
     * @param memberId the member ID
     * @return the member DTO
     * @throws RuntimeException if member not found or service unavailable
     */
    MemberDto getMember(Long memberId);
}
