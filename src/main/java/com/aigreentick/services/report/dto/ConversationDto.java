package com.aigreentick.services.report.dto;

public record ConversationDto(
        Integer contactId,
        String name,
        String mobile,
        Long lastChatTime,
        Long unreadCount,
        ChatDTO lastChat
) {}
