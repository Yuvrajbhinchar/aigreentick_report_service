package com.aigreentick.services.report.dto;

import java.time.LocalDateTime;

public interface ChatHistoryRowDTO {

    Long getId();
    Long getContactId();

    // contact
    String getName();
    String getMobile();
    String getEmail();
    String getCountryId();

    // chat
    String getChatText();
    String getChatType();
    String getChatTime();
    String getChatStatus();

    // report
    Long getReportId();
    String getReportStatus();

    Integer getUnreadCount();
    Long getLastChatTime();

    LocalDateTime getCreatedAt();
}

