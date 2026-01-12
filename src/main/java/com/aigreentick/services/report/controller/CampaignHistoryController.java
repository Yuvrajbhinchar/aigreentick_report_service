package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.CampaignHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class CampaignHistoryController {

    private final CampaignHistoryService campaignHistoryService;

    @GetMapping
    public Map<String, Object> getReports() {

        Long userId = 41L; // replace with auth user

        return Map.of(
                "status", true,
                "data", Map.of(
                        "current_page", 1,
                        "data", campaignHistoryService.getReports(userId)
                )
        );
    }
}
