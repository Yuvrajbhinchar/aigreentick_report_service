package com.aigreentick.services.report.mapper;

import com.aigreentick.services.report.dto.campaignHistoryDTO.ReportDTO;
import com.aigreentick.services.report.entity.Report;
import org.springframework.stereotype.Component;

@Component
public class CampaignDetailMapper {

    public ReportDTO toDTO(Report report) {
        return ReportDTO.builder()
                .id(report.getId())
                .userId(report.getUserId())
                .broadcastId(report.getBroadcast().getId())
                .campaignId(report.getCampaignId())
                .groupSendId(report.getGroupSendId())
                .tagLogId(report.getTagLogId())
                .mobile(report.getMobile())
                .type(report.getType())
                .messageId(report.getMessageId())
                .waId(report.getWaId())
                .messageStatus(report.getMessageStatus())
                .status(report.getStatus())
                .payload(report.getPayload())
                .response(report.getResponse())
                .contact(report.getContact())
                .paymentStatus(0)
                .platform(report.getPlatform().name())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .deletedAt(report.getDeletedAt())
                .build();
    }
}