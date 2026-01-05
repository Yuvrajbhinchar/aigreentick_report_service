package com.aigreentick.services.report.dto;

public record ChatDto(
        Long id,
        Integer contactId,
        String text,
        String type,
        String status,
        String time
) {}
