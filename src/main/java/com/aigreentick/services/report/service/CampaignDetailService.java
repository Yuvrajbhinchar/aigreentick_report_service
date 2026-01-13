package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.campaignHistoryDTO.ApiResponse;
import com.aigreentick.services.report.dto.campaignHistoryDTO.PaginatedData;
import com.aigreentick.services.report.dto.campaignHistoryDTO.PaginationLink;
import com.aigreentick.services.report.dto.campaignHistoryDTO.ReportDTO;
import com.aigreentick.services.report.entity.Report;
import com.aigreentick.services.report.mapper.CampaignDetailMapper;
import com.aigreentick.services.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CampaignDetailService {

    private final ReportRepository reportRepository;
    private final CampaignDetailMapper reportMapper;

    public ApiResponse<PaginatedData<ReportDTO>> getReportsByBroadcastId(Long broadcastId, int page, int perPage) {
        Pageable pageable = PageRequest.of(page - 1, perPage);
        Page<Report> reportPage = reportRepository.findByBroadcastId(broadcastId, pageable);

        List<ReportDTO> reportDTOs = reportPage.getContent().stream()
                .map(reportMapper::toDTO)
                .collect(Collectors.toList());

        String baseUrl = "https://aigreentick.com/api/v1/report/" + broadcastId;

        PaginatedData<ReportDTO> paginatedData = PaginatedData.<ReportDTO>builder()
                .currentPage(reportPage.getNumber() + 1)
                .data(reportDTOs)
                .firstPageUrl(baseUrl + "?page=1")
                .from(reportPage.isEmpty() ? null : (int) reportPage.getPageable().getOffset() + 1)
                .lastPage(reportPage.getTotalPages())
                .lastPageUrl(baseUrl + "?page=" + reportPage.getTotalPages())
                .links(buildPaginationLinks(baseUrl, reportPage))
                .nextPageUrl(reportPage.hasNext() ? baseUrl + "?page=" + (reportPage.getNumber() + 2) : null)
                .path(baseUrl)
                .perPage(reportPage.getSize())
                .prevPageUrl(reportPage.hasPrevious() ? baseUrl + "?page=" + reportPage.getNumber() : null)
                .to(reportPage.isEmpty() ? null : (int) reportPage.getPageable().getOffset() + reportPage.getNumberOfElements())
                .total(reportPage.getTotalElements())
                .build();

        return ApiResponse.<PaginatedData<ReportDTO>>builder()
                .data(paginatedData)
                .build();
    }

    private List<PaginationLink> buildPaginationLinks(String baseUrl, Page<Report> page) {
        List<PaginationLink> links = new ArrayList<>();

        links.add(PaginationLink.builder()
                .url(page.hasPrevious() ? baseUrl + "?page=" + page.getNumber() : null)
                .label("&laquo; Previous")
                .active(false)
                .build());

        for (int i = 1; i <= page.getTotalPages(); i++) {
            links.add(PaginationLink.builder()
                    .url(baseUrl + "?page=" + i)
                    .label(String.valueOf(i))
                    .active(i == page.getNumber() + 1)
                    .build());
        }

        links.add(PaginationLink.builder()
                .url(page.hasNext() ? baseUrl + "?page=" + (page.getNumber() + 2) : null)
                .label("Next &raquo;")
                .active(false)
                .build());

        return links;
    }
}