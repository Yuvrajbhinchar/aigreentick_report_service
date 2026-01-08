package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.ChatMessageResponse;
import com.aigreentick.services.report.service.ChatPaginationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatPaginationController {

    private final ChatPaginationService service;


    @GetMapping
    public Map<String, Object> getChats(
            @RequestParam Long userId,
            @RequestParam Long contactId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int perPage,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate
    ) {
        if (contactId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "contact_id is required"
            );
        }

        Page<ChatMessageResponse> messages =
                service.getChats(
                        userId,
                        contactId,
                        page,
                        perPage,
                        search,
                        fromDate,
                        toDate
                );

        return Map.of(
                "messages", messages
        );
    }
}

