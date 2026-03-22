package com.fightmind.admin.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserSummaryDto {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String provider;
    private Instant createdAt;
}
