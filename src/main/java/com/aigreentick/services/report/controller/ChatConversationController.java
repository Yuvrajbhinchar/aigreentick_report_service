package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.ChatConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatConversationController {

    private final ChatConversationService service;

    @GetMapping("/{contactId}")
    public Object getConversation(
            @RequestParam Long userId,
            @PathVariable Long contactId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return service.getConversation(
                userId, contactId, search, fromDate, toDate, page, size
        );
    }
}

