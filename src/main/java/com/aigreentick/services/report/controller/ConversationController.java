package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Conversation (Inbox) Controller
 * Matches PHP SendMessageController behavior exactly
 */
@RestController
@RequestMapping("/api/chats")
@Slf4j
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService service;

    /**
     * MAIN INBOX ENDPOINT
     * GET /api/chats/inbox
     *
     * Query Parameters:
     * - userId (required): User ID
     * - search (optional): Search by name or mobile
     * - filter (optional): "unread" or "active" (last 24h)
     * - from_date (optional): Start date for filtering
     * - to_date (optional): End date for filtering
     * - page (optional, default 0): Page number (0-based)
     * - size (optional, default 15): Records per page
     *
     * Response matches PHP format:
     * {
     *   "users": {
     *     "current_page": 1,
     *     "data": [...],
     *     "per_page": 15,
     *     "total": 100,
     *     "last_page": 7,
     *     "links": [...],
     *     ...
     *   },
     *   "channel": [...]
     * }
     */
    @GetMapping("/inbox")
    public Map<String, Object> inbox(
            @RequestParam Integer userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(name = "from_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(name = "per_page", required = false) Integer perPage // Support both size and per_page
    ) {
        log.info("=== /inbox called - userId: {}, page: {}, size: {}, search: {}, filter: {}, fromDate: {}, toDate: {} ===",
                userId, page, size, search, filter, fromDate, toDate);

        // Support both 'size' and 'per_page' parameters (Laravel convention)
        int finalSize = perPage != null ? perPage : size;

        // Convert LocalDate to LocalDateTime for repository
        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        return service.getInbox(userId, search, filter, fromDateTime, toDateTime, page, finalSize);
    }

    /**
     * Alternative endpoint matching PHP route exactly
     * POST /api/v1/sendMessage (if needed)
     */
    @PostMapping("/send-message")
    public Map<String, Object> sendMessage(
            @RequestParam Integer userId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String filter,
            @RequestParam(name = "from_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "per_page", defaultValue = "15") int perPage
    ) {
        log.info("=== /send-message (POST) called - userId: {}, page: {}, perPage: {} ===",
                userId, page, perPage);

        LocalDateTime fromDateTime = fromDate != null ? fromDate.atStartOfDay() : null;
        LocalDateTime toDateTime = toDate != null ? toDate.atTime(23, 59, 59) : null;

        return service.getInbox(userId, search, filter, fromDateTime, toDateTime, page, perPage);
    }

    /**
     * Utility endpoint to clear channel cache (admin use)
     */
    @PostMapping("/admin/clear-cache")
    public Map<String, Object> clearCache(@RequestParam(required = false) Integer userId) {
        if (userId != null) {
            ConversationService.invalidateChannelCache(userId);
            return Map.of("message", "Cache cleared for user: " + userId);
        } else {
            ConversationService.clearAllCache();
            return Map.of("message", "All cache cleared");
        }
    }
}