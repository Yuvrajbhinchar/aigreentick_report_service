package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.MessageHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class MessageHistoryController {

    private final MessageHistoryService service;

    @GetMapping("/get-messages-history")
    public Map<String, Object> getMessagesHistory(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "15") int perPage,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) LocalDateTime fromDate,
            @RequestParam(required = false) LocalDateTime toDate
    ) {

        List<Map<String, Object>> users =
                service.getMessagesHistory(
                        userId,
                        perPage,
                        page,
                        search,
                        filter,
                        fromDate,
                        toDate
                );

        return Map.of(
                "users", users
        );
    }
}
