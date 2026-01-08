package com.aigreentick.services.report.dto;

import java.time.LocalDateTime;

public interface MessageHistoryProjection {

    Long getId();
    Long getContactId();

    String getName();
    String getMobile();
    String getEmail();
    String getCountryId();

    String getChatText();
    String getChatType();
    String getChatTime();
    String getChatStatus();

    Long getReportId();
    String getReportStatus();

    Integer getUnreadCount();
    Long getLastChatTime();

    LocalDateTime getCreatedAt();
}
