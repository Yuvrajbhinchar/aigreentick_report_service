package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.*;
import com.aigreentick.services.report.entity.Broadcast;
import com.aigreentick.services.report.entity.Template;
import com.aigreentick.services.report.repository.BroadcastRepository;
import com.aigreentick.services.report.repository.ReportRepository;
import com.aigreentick.services.report.repository.TemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CampaignHistoryService {

    private final BroadcastRepository broadcastRepo;
    private final TemplateRepository templateRepo;
    private final ReportRepository reportRepo;

    public List<BroadcastResponseDTO> getReports(Long userId) {

        List<Broadcast> broadcasts =
                broadcastRepo.findByUserIdOrderByIdDesc(userId);

        return broadcasts.stream().map(b -> {

            Template template =
                    templateRepo.findById(b.getTemplateId()).orElse(null);

            return BroadcastResponseDTO.builder()
                    .id(b.getId())
                    .campname(b.getCampname())
                    .sentCount(reportRepo.countByBroadcastAndStatus(b.getId(), "sent"))
                    .readCount(reportRepo.countByBroadcastAndStatus(b.getId(), "read"))
                    .template(template == null ? null : mapTemplate(template))
                    .build();

        }).toList();
    }

    private TemplateDTO mapTemplate(Template t) {


        return TemplateDTO.builder()
                .id(t.getId())
                .name(t.getName())
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
