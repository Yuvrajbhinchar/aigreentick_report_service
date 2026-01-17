package com.aigreentick.services.report.dto.chatHistoryDTO;


import com.aigreentick.services.report.dto.chatHistoryDTO.ReportDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {
    private Integer id;

    @JsonProperty("contact_id")
    private Integer contactId;

    private ContactDTO contact;

    private ChatDTO chat;

    private ReportDTO report;

    @JsonProperty("unread_count")
    private Integer unreadCount;

    @JsonProperty("last_chat_time")
    private Long lastChatTime;

    @JsonProperty("created_at")
    private String createdAt;
}