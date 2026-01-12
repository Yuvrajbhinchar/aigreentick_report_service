package com.aigreentick.services.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TemplateDTO {

    private Long id;
    private Long userId;
    private String name;
    private String language;
    private String status;
    private String category;
    private String waId;
    private String templateType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<TemplateComponentDTO> components;
}
