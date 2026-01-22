package com.aigreentick.services.report.dto.chatHistory;

import java.time.LocalDateTime;

public interface ContactMessageProjection {
    Long getId();
    Integer getContactId();
    String getContactName();
    String getContactMobile();
    String getContactEmail();
    String getContactCountryId();
    Long getChatId();
    String getChatText();
    String getChatType();
    String getChatTime();
    String getChatStatus();
    LocalDateTime getChatCreatedAt();
    Long getReportId();
    String getReportStatus();
    LocalDateTime getReportCreatedAt();
    Integer getUnreadCount();
    Long getLastChatTime();
    LocalDateTime getCreatedAt();
}
