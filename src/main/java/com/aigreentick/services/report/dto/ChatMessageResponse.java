package com.aigreentick.services.report.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChatMessageResponse {

    private Long id;
    private String category;

    private String replyMessageId;
    private String messageId;
    private String replyFrom;

    private String templateType;   // STANDARD / CAROUSEL
    private String chatType;       // chat / broadcast
    private String type;           // sent / recieve
    private String status;

    private String message;

    // decoded JSON fields
    private Object payload;
    private Object response;

    private String sendFrom;
    private String sendTo;

    private LocalDateTime createdAt;

    private List<?> buttons;
    private List<?> chatButtons;
    private List<?> carouselCards;
}
