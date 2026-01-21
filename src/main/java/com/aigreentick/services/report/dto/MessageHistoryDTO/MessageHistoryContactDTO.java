package com.aigreentick.services.report.dto.MessageHistoryDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageHistoryContactDTO {
    private Long id;

    @JsonProperty("contact_id")
    private Long contactId;

    private ContactInfo contact;
    private ChatInfo chat;
    private ReportInfo report;

    @JsonProperty("unread_count")
    private Integer unreadCount;

    @JsonProperty("last_chat_time")
    private Long lastChatTime;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

}