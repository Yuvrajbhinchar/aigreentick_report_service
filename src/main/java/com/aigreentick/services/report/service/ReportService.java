package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.CampaignDeliveryReportDTO;
import com.aigreentick.services.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;

    public List<CampaignDeliveryReportDTO> getCampaignDeliveryReport(
            Long userId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return reportRepository.findCampaignDeliveryReport(userId, from, to);
    }
}
