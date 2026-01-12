package com.sotatek.order.service.external.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for Member from external Member Service
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MemberDto {

    private Long id;
    private String name;
    private String email;
    private MemberStatus status;
    private MemberGrade grade;
}
