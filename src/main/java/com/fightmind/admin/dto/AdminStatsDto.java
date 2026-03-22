package com.fightmind.admin.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminStatsDto {
    private long totalUsers;
    private long totalMessages;
    private long cachedResponses;
    private long messagesWithImages;
}
