package com.aigreentick.services.report.dto.campaignHistoryDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationLink {
    private String url;
    private String label;
    private Boolean active;
}