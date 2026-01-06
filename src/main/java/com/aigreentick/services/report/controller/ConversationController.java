package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@Slf4j
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService service;

    @GetMapping("/inbox")
    public Map<String, Object> inbox(
            @RequestParam Integer userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return service.getInbox(userId, search, filter, page, size);
    }
}
