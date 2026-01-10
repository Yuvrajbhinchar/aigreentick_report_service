package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.CampaignHistoryDTO.BroadcastReportDTO;
import com.aigreentick.services.report.service.BroadCastHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/broadcast-history")
@RequiredArgsConstructor
@Slf4j
public class CampaignHistoryController {

    private final BroadCastHistoryService broadCastHistoryService;

    @GetMapping
    public Page<BroadcastReportDTO> getBroadcastReports(
            @RequestParam Long userId,

            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String state,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {

        log.debug(
                "Fetching broadcast history | userId={}, search={}, type={}, state={}, from={}, to={}, page={}, size={}",
                userId, search, type, state, from, to, page, size
        );

        return broadCastHistoryService.getReports(
                userId,
                search,
                type,
                state,
                from,
                to,
                page,
                size
        );
    }
}
