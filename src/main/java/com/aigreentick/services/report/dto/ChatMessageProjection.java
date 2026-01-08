package com.aigreentick.services.report.dto;

import java.time.LocalDateTime;

public interface ChatMessageProjection {

    Long getId();
    String getCategory();

    String getReplyMessageId();
    String getMessageId();
    String getReplyFrom();

    String getTemplateType();
    String getChatType();
    String getType();
    String getStatus();

    String getMessage();
    String getPayload();
    String getResponse();

    String getSendFrom();
    String getSendTo();

    LocalDateTime getCreatedAt();

    String getButtons();
    String getChatButtons();
    String getCarouselCards();
}
