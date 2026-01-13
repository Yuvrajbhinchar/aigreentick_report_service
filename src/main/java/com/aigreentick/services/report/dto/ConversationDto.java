package com.aigreentick.services.report.dto;

import com.aigreentick.services.report.dto.walletDTO.ChatDTO;

public record ConversationDto(
        Integer contactId,
        String name,
        String mobile,
        Long lastChatTime,
        Long unreadCount,
        ChatDTO lastChat
) {}
