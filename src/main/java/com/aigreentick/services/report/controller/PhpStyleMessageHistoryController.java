package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.MessageHistoryDTO.MessageHistoryWrapperResponse;
import com.aigreentick.services.report.service.PhpStyleMessageHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v4")
@Slf4j
public class PhpStyleMessageHistoryController {

    private final PhpStyleMessageHistoryService service;

    @GetMapping("/get-messages-history")
    public ResponseEntity<MessageHistoryWrapperResponse> getMessagesHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "15") int perPage,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate
    ) {
        log.info("=== GET /api/v1/get-messages-history - userId: {}, page: {}, perPage: {}, filter: {} ===",
                userId, page, perPage, filter);

        MessageHistoryWrapperResponse response = service.getMessagesHistory(
                userId, perPage, page, search, filter, fromDate, toDate
        );

        return ResponseEntity.ok(response);
    }
}