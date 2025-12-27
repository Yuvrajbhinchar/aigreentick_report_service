package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.BillingReportDTO;
import com.aigreentick.services.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BillingReportService {

    private final ReportRepository reportRepository;

    public List<BillingReportDTO> getBillingReport(
            LocalDateTime from,
            LocalDateTime to
    ) {
        return reportRepository.findBillingReport(from, to);
    }
}
