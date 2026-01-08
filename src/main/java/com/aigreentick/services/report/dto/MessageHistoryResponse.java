package com.aigreentick.services.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageHistoryResponse {

    private Long id;
    private Long contactId;
    private ContactDTO contact;
    private ChatDTO chat;
    private ReportDTO report;
    private Integer unreadCount;
    private Long lastChatTime;
    private LocalDateTime createdAt;


}



