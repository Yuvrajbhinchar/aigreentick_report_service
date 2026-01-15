package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.campaignHistoryDTO.BroadcastResponseDTO;
import com.aigreentick.services.report.dto.campaignHistoryDTO.TemplateComponentDTO;
import com.aigreentick.services.report.dto.campaignHistoryDTO.TemplateDTO;
import com.aigreentick.services.report.entity.Broadcast;
import com.aigreentick.services.report.entity.Template;
import com.aigreentick.services.report.repository.BroadcastRepository;
import com.aigreentick.services.report.repository.ReportRepository;
import com.aigreentick.services.report.repository.TemplateRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignHistoryService {

    private final BroadcastRepository broadcastRepo;
    private final TemplateRepository templateRepo;
    private final ReportRepository reportRepo;
    private final EntityManager entityManager;

    public Map<String, Object> getReports(Long userId, String search, String type,
                                          String state, LocalDate from, LocalDate to,
                                          int page, int size) {

        log.info("=== CampaignHistoryService getReports started at: {} ===",
                LocalDateTime.now());

        try {
            Pageable pageable = PageRequest.of(page - 1, size);

            // Build criteria query
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<Broadcast> query = cb.createQuery(Broadcast.class);
            Root<Broadcast> root = query.from(Broadcast.class);

            List<Predicate> predicates = new ArrayList<>();

            // User filter
            predicates.add(cb.equal(root.get("userId"), userId));

            // Search filter (campname or mobile in reports)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim() + "%";

                Subquery<Long> reportSubquery = query.subquery(Long.class);
                Root<com.aigreentick.services.report.entity.Report> reportRoot =
                        reportSubquery.from(com.aigreentick.services.report.entity.Report.class);
                reportSubquery.select(reportRoot.get("broadcast").get("id"))
                        .where(cb.like(reportRoot.get("mobile"), searchPattern));

                predicates.add(cb.or(
                        cb.like(root.get("campname"), searchPattern),
                        cb.in(root.get("id")).value(reportSubquery)
                ));
            }

            // Type filter (platform)
            if (type != null && !type.trim().isEmpty()) {
                Subquery<Long> platformSubquery = query.subquery(Long.class);
                Root<com.aigreentick.services.report.entity.Report> reportRoot =
                        platformSubquery.from(com.aigreentick.services.report.entity.Report.class);
                platformSubquery.select(reportRoot.get("broadcast").get("id"))
                        .where(cb.equal(reportRoot.get("platform"),
                                com.aigreentick.services.report.enums.Platform.valueOf(type)));

                predicates.add(cb.in(root.get("id")).value(platformSubquery));
            }

            // Date range filters
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        cb.function("DATE", LocalDate.class, root.get("createdAt")), from));
            }

            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        cb.function("DATE", LocalDate.class, root.get("createdAt")), to));
            }

            query.where(predicates.toArray(new Predicate[0]));
            query.orderBy(cb.desc(root.get("id")));

            // Execute query with pagination
            List<Broadcast> broadcasts = entityManager.createQuery(query)
                    .setFirstResult((int) pageable.getOffset())
                    .setMaxResults(pageable.getPageSize())
                    .getResultList();

            // Count query for pagination
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<Broadcast> countRoot = countQuery.from(Broadcast.class);
            countQuery.select(cb.count(countRoot));
            countQuery.where(predicates.toArray(new Predicate[0]));
            Long total = entityManager.createQuery(countQuery).getSingleResult();

            // Map to DTOs with counts
            List<BroadcastResponseDTO> dtos = broadcasts.stream()
                    .map(this::mapToBroadcastResponseDTO)
                    .toList();

            // Apply state filter after mapping (since it depends on counts)
            if (state != null && !state.trim().isEmpty()) {
                dtos = filterByState(dtos, state);
                total = (long) dtos.size(); // Update total after filtering
            }

            // Create paginated response
            Page<BroadcastResponseDTO> pageResult = new PageImpl<>(dtos, pageable, total);

            return Map.of(
                    "status", true,
                    "data", Map.of(
                            "current_page", page,
                            "data", pageResult.getContent(),
                            "total", pageResult.getTotalElements(),
                            "per_page", size,
                            "last_page", pageResult.getTotalPages()
                    )
            );

        } catch (Exception e) {
            log.error("Error in getReports: ", e);
            return Map.of(
                    "status", false,
                    "error", e.getMessage()
            );
        }
    }

    private List<BroadcastResponseDTO> filterByState(List<BroadcastResponseDTO> dtos, String state) {
        return dtos.stream()
                .filter(dto -> {
                    return switch (state.toLowerCase()) {
                        case "pending" -> dto.getProcess() > 0 || dto.getOtherCount() > 0;
                        case "failed" -> dto.getFailedCount() > 0;
                        case "completed" -> dto.getProcess() == 0 && dto.getOtherCount() == 0;
                        default -> true;
                    };
                })
                .toList();
    }

    private BroadcastResponseDTO mapToBroadcastResponseDTO(Broadcast b) {
        Template template = templateRepo.findById(b.getTemplateId()).orElse(null);

        // Calculate counts
        long sentCount = reportRepo.countByBroadcastAndStatus(b.getId(), "sent");
        long readCount = reportRepo.countByBroadcastAndStatus(b.getId(), "read");
        long dlvdCount = reportRepo.countByBroadcastAndStatus(b.getId(), "delivered");
        long failedCount = reportRepo.countByBroadcastAndStatus(b.getId(), "failed");
        long otherCount = reportRepo.countByBroadcastAndStatus(b.getId(), "pending");
        long process = countProcessAndQueue(b.getId());

        return BroadcastResponseDTO.builder()
                .id(b.getId())
                .source(b.getSource())
                .userId(b.getUserId())
                .templateId(b.getTemplateId())
                .countryId(b.getCountryId())
                .campname(b.getCampname())
                .isMedia(b.getIsMedia())
                .total(b.getTotal())
                .status(b.getStatus())
                .numbers("") // Cleared for security
                .requests("") // Cleared for security
                .createdAt(b.getCreatedAt())
                .updatedAt(b.getUpdatedAt())
                .sentCount(sentCount)
                .readCount(readCount)
                .dlvdCount(dlvdCount)
                .failedCount(failedCount)
                .otherCount(otherCount)
                .process(process)
                .template(template == null ? null : mapTemplate(template))
                .build();
    }

    private long countProcessAndQueue(Long broadcastId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<com.aigreentick.services.report.entity.Report> root =
                query.from(com.aigreentick.services.report.entity.Report.class);

        query.select(cb.count(root));
        query.where(
                cb.and(
                        cb.equal(root.get("broadcast").get("id"), broadcastId),
                        cb.or(
                                cb.equal(root.get("status"), "process"),
                                cb.equal(root.get("status"), "queue")
                        )
                )
        );

        return entityManager.createQuery(query).getSingleResult();
    }

    private TemplateDTO mapTemplate(Template t) {
        return TemplateDTO.builder()
                .id(t.getId())
                .userId(t.getUserId())
                .name(t.getName())
                .language(t.getLanguage())
                .status(t.getStatus())
                .category(t.getCategory())
                .waId(t.getWaId())
                .templateType(t.getTemplateType())
                .createdAt(t.getCreatedAt())
                .updatedAt(t.getUpdatedAt())
                .components(
                        t.getComponents().stream()
                                .map(c -> TemplateComponentDTO.builder()
                                        .id(c.getId())
                                        .type(c.getType())
                                        .format(c.getFormat())
                                        .text(c.getText())
                                        .imageUrl(c.getImageUrl())
                                        .buttons(List.of())
                                        .carouselCards(List.of())
                                        .build()
                                )
                                .toList()
                )
                .build();
    }
}