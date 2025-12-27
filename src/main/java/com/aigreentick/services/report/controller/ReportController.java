package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.CampaignDeliveryReportDTO;
import com.aigreentick.services.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/campaign-delivery")
    public List<CampaignDeliveryReportDTO> campaignDeliveryReport(
            @RequestParam Long userId,
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ) {
        return reportService.getCampaignDeliveryReport(userId, from, to);
    }
}
