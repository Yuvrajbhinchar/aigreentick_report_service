package com.aigreentick.services.report.dto;

public record CampaignDeliveryReportDTO(
        Long campaignId,
        String campname,
        Long total,
        Long sent,
        Long delivered,
        Long failed,
        Long pending
) {}
