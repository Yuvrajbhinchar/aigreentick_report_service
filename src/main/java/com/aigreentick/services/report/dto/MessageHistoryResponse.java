package com.aigreentick.services.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageHistoryResponse {

    private Long id;
    private Long contact_id;
    private ContactDTO contact;
    private ChatDTO chat;
    private ReportDTO report;
    private Long unread_count;
    private Long last_chat_time;
    private String created_at;
}


