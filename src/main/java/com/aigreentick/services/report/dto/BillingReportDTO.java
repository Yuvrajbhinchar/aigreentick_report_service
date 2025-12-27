package com.aigreentick.services.report.dto;

public record BillingReportDTO(
        Long userId,
        String userName,

        Long deliveredCount,
        Double ratePerMessage,
        Double totalAmount
) {}
