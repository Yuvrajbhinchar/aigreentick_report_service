package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.ConversationDto;
import com.aigreentick.services.report.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService service;

    @GetMapping("/conversations")
    public Page<ConversationDto> inbox(
            @RequestParam Integer userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size
    ) {
        return service.getInbox(userId, search, filter, page, size);
    }
}
