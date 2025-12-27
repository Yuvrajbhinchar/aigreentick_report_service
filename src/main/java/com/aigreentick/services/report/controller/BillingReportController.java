package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.BillingReportDTO;
import com.aigreentick.services.report.service.BillingReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class BillingReportController {

    private final BillingReportService billingReportService;

    @GetMapping("/billing")
    public List<BillingReportDTO> billingReport(
            @RequestParam LocalDateTime from,
            @RequestParam LocalDateTime to
    ) {
        return billingReportService.getBillingReport(from, to);
    }
}
