package com.aigreentick.services.report.dto;

import com.aigreentick.services.report.dto.chatHistoryDTO.ChatDTO;

public record ConversationDto(
        Integer contactId,
        String name,
        String mobile,
        Long lastChatTime,
        Long unreadCount,
        ChatDTO lastChat
) {}
