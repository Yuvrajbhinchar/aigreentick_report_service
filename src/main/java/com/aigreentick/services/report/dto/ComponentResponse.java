package com.aigreentick.services.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;


@Data
public class ComponentResponse {

    private Long id;

    @JsonProperty("template_id")
    private Long templateId;

    private String type;
    private String format;
    private String text;

    @JsonProperty("image_url")
    private String imageUrl;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;

    @JsonProperty("deleted_at")
    private LocalDateTime deletedAt;

    private List<Object> buttons;

    @JsonProperty("body_text")
    private List<Object> bodyText;

    @JsonProperty("carousel_cards")
    private List<Object> carouselCards;
}
