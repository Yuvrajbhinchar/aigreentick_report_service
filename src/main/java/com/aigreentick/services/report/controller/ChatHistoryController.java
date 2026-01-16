package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.chatHistoryDTO.ChatHistoryDTO;
import com.aigreentick.services.report.service.ChatHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ChatHistoryController {

    private final ChatHistoryService chatHistoryService;

    @GetMapping("/get-messages-history")
    public ResponseEntity<ChatHistoryDTO> getMessagesHistory(
            @RequestParam(required = false) Integer userId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "MM/dd/yyyy") LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "MM/dd/yyyy") LocalDateTime toDate,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "15") Integer perPage
    ) {
        if (userId == null) {
            userId = 41;
        }

        ChatHistoryDTO response = chatHistoryService.getMessagesHistory(
                userId,
                fromDate,
                toDate,
                search,
                page,
                perPage
        );

        return ResponseEntity.ok(response);
    }
}
