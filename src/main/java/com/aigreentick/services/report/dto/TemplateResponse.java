package com.aigreentick.services.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TemplateResponse {

    private Long id;

    @JsonProperty("user_id")
    private Long userId;

    private String name;

    @JsonProperty("previous_category")
    private String previousCategory;

    private String language;
    private String status;
    private String category;

    @JsonProperty("wa_id")
    private String waId;

    private Object payload;
    private Object response;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;

    @JsonProperty("template_type")
    private String templateType;

    private List<ComponentResponse> components;
}

