package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.CampaignHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class CampaignHistoryController {

    private final CampaignHistoryService campaignHistoryService;

    @GetMapping
    public Map<String, Object> getReports(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Long userId = 41L; // replace with auth user

        return campaignHistoryService.getReports(userId, search, type, state, from, to, page, size);
    }
}