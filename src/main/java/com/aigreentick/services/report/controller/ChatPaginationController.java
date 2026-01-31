package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.ChatPaginationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Chat Pagination Controller
 * Matches PHP endpoint: /api/v1/get-chat-with-pagination
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ChatPaginationController {

    private final ChatPaginationService service;

    /**
     * MAIN ENDPOINT: Get chat messages with pagination
     * GET /api/v1/get-chat-with-pagination
     *
     * Query Parameters:
     * - userId (required): User ID
     * - contact_id (required): Contact ID
     * - page (optional, default 1): Page number (1-based like PHP/Laravel)
     * - per_page (optional, default 10): Records per page
     * - search (optional): Search text
     * - from_date (optional): Start date filter
     * - to_date (optional): End date filter
     *
     * Response matches PHP format (Laravel pagination):
     * {
     *   "messages": {
     *     "current_page": 1,
     *     "data": [...],
     *     "first_page_url": "...",
     *     "from": 1,
     *     "last_page": 18,
     *     "last_page_url": "...",
     *     "links": [...],
     *     "next_page_url": "...",
     *     "path": "...",
     *     "per_page": 10,
     *     "prev_page_url": null,
     *     "to": 10,
     *     "total": 177
     *   },
     *   "channel": [...]
     * }
     */
    @GetMapping("/get-chat-with-pagination")
    public ResponseEntity<Map<String, Object>> getChatsWithPagination(
            @RequestParam Long userId,
            @RequestParam(name = "contact_id") Long contactId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "per_page", defaultValue = "10") int perPage,
            @RequestParam(required = false) String search,
            @RequestParam(name = "from_date", required = false) String fromDate,
            @RequestParam(name = "to_date", required = false) String toDate
    ) {
        log.info("=== GET /api/v1/get-chat-with-pagination - userId: {}, contactId: {}, page: {} ===",
                userId, contactId, page);

        if (contactId == null) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "contact_id is required"));
        }

        // Convert 1-based page to 0-based for internal use
        int pageIndex = Math.max(0, page - 1);

        Map<String, Object> response = service.getChats(
                userId, contactId, pageIndex, perPage, search, fromDate, toDate
        );

        return ResponseEntity.ok(response);
    }
}