package com.aigreentick.services.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryRow {

    private Long id;
    private Long contactId;
    private Long chatId;
    private Long reportId;
    private LocalDateTime createdAt;

    private String name;
    private String mobile;
    private String email;
    private Long countryId;

    private String chatText;
    private String chatType;
    private String chatTime;
    private String chatStatus;

    private Long reportStatus;

    private Long unreadCount;
    private Long lastChatTime;
}
