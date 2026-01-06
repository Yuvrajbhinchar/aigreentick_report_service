package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-history")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService service;

    @GetMapping
    public Object list(
            @RequestParam Long userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return service.getHistory(userId, search, filter, page, size);
    }
}

