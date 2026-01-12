package com.aigreentick.services.report.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class TemplateComponentDTO {

    private Long id;
    private String type;
    private String format;
    private String text;
    private String imageUrl;

    private List<Object> buttons;
    private List<Object> carouselCards;
}
