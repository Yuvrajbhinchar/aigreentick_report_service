package com.aigreentick.services.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BroadcastResponseDTO {

    private Long id;
    private String source;
    private Long userId;
    private Long templateId;
    private Long countryId;
    private String campname;
    private String isMedia;
    private Integer total;
    private String status;
    private String numbers;
    private String requests;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private long sentCount;
    private long readCount;
    private long dlvdCount;
    private long failedCount;
    private long otherCount;
    private long process;

    private TemplateDTO template;
}
