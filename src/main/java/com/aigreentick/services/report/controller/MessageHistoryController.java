package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.massagesHistoryDTO.MessageHistoryResponse;
import com.aigreentick.services.report.service.MessageHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageHistoryController {

    private final MessageHistoryService service;


    @GetMapping
    public Map<String, Object> getMessages(
            @RequestParam Long userId,               // 👈 explicit
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int perPage
    ) {
        Page<MessageHistoryResponse> result =
                service.getMessages(
                        userId,
                        PageRequest.of(page, perPage, Sort.by(Sort.Direction.DESC, "id"))
                );

        return Map.of(
                "users", result
        );
    }
}


