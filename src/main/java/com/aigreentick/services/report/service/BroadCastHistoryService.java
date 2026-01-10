package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.CampaignHistoryDTO.BroadcastReportDTO;
import com.aigreentick.services.report.entity.Broadcast;
import com.aigreentick.services.report.repository.BroadcastRepository;
import com.aigreentick.services.report.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BroadCastHistoryService {

    private final BroadcastRepository broadcastRepository;
    private final ReportRepository reportRepository;

    /* ============================================================
       MAIN API
       ============================================================ */

    @Transactional(readOnly = true)
    public Page<BroadcastReportDTO> getReports(
            Long userId,
            String search,
            String type,
            String state,
            LocalDate from,
            LocalDate to,
            int page,
            int size
    ) {

        Pageable pageable =
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));

        // ---- LocalDate → LocalDateTime (CRITICAL) ----
        LocalDateTime fromDateTime =
                (from != null) ? from.atStartOfDay() : null;

        LocalDateTime toDateTime =
                (to != null) ? to.plusDays(1).atStartOfDay() : null;

        Page<Broadcast> broadcasts =
                broadcastRepository.findBroadcastsWithFilters(
                        userId,
                        normalize(search),
                        normalize(type),
                        normalize(state),
                        fromDateTime,
                        toDateTime,
                        pageable
                );

        if (broadcasts.isEmpty()) {
            return Page.empty(pageable);
        }

        // ---- Batch status aggregation (NO N+1) ----
        List<Long> broadcastIds = broadcasts.getContent()
                .stream()
                .map(Broadcast::getId)
                .collect(Collectors.toList());

        Map<Long, Map<String, Long>> statusCountsMap =
                fetchAllStatusCountsBatch(broadcastIds);

        // ---- Mapping ----
        return broadcasts.map(broadcast -> {
            BroadcastReportDTO dto =
                    mapToDTO(broadcast, statusCountsMap);


            dto.setUser(broadcast.getUser());
            dto.setTemplate(broadcast.getTemplate());

            return dto;
        });
    }

    /* ============================================================
       BATCH STATUS COUNTS
       ============================================================ */

    private Map<Long, Map<String, Long>> fetchAllStatusCountsBatch(
            List<Long> broadcastIds
    ) {

        if (broadcastIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Object[]> rows =
                reportRepository.getBatchStatusCountsByBroadcastIds(broadcastIds);

        Map<Long, Map<String, Long>> result = new HashMap<>();

        for (Object[] row : rows) {
            Long broadcastId = ((Number) row[0]).longValue();
            String status = (String) row[1];
            Long count = ((Number) row[2]).longValue();

            result
                    .computeIfAbsent(broadcastId, k -> new HashMap<>())
                    .put(status, count);
        }

        return result;
    }

    /* ============================================================
       DTO MAPPING
       ============================================================ */

    private BroadcastReportDTO mapToDTO(
            Broadcast broadcast,
            Map<Long, Map<String, Long>> allStatusCounts
    ) {

        Map<String, Long> counts =
                allStatusCounts.getOrDefault(
                        broadcast.getId(),
                        Collections.emptyMap()
                );

        long processCount =
                counts.getOrDefault("process", 0L) +
                        counts.getOrDefault("queue", 0L);

        BroadcastReportDTO dto = new BroadcastReportDTO();

        dto.setId(broadcast.getId());
        dto.setSource(broadcast.getSource());
        dto.setUserId(broadcast.getUserId());
        dto.setWalletId(broadcast.getWalletId());
        dto.setTemplateId(broadcast.getTemplateId());
        dto.setWhatsapp(broadcast.getWhatsapp());
        dto.setCountryId(broadcast.getCountryId());
        dto.setCampname(broadcast.getCampname());
        dto.setIsMedia(broadcast.getIsMedia());
        dto.setData(broadcast.getData());
        dto.setTotal(broadcast.getTotal());
        dto.setScheduleAt(broadcast.getScheduleAt());
        dto.setStatus(broadcast.getStatus());
        dto.setCreatedAt(broadcast.getCreatedAt());
        dto.setUpdatedAt(broadcast.getUpdatedAt());
        dto.setDeletedAt(broadcast.getDeletedAt());

        // PHP compatibility
        dto.setNumbers("");
        dto.setRequests("");

        // ---- Status counts ----
        dto.setSentCount(counts.getOrDefault("sent", 0L));
        dto.setReadCount(counts.getOrDefault("read", 0L));
        dto.setDlvdCount(counts.getOrDefault("delivered", 0L));
        dto.setFailedCount(counts.getOrDefault("failed", 0L));
        dto.setPendingCount(counts.getOrDefault("pending", 0L));
        dto.setProcessCount(processCount);

        // Explicit (no double counting)
        dto.setOtherCount(0L);

        return dto;
    }

    /* ============================================================
       UTIL
       ============================================================ */

    private String normalize(String value) {
        return (value == null || value.isBlank())
                ? null
                : value.trim();
    }
}
