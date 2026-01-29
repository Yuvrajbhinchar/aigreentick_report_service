package com.aigreentick.services.report.service;

import com.aigreentick.services.report.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository repository;

    /**
     * ULTRA-OPTIMIZED: Single query execution with embedded last_chat
     * No need for separate fetchLastChats call
     */
    public Map<String, Object> getInbox(
            Integer userId,
            String search,
            String filter,
            int page,
            int size
    ) {
        long startTime = System.currentTimeMillis();
        log.info("=== ConversationService getInbox START - User: {}, Page: {}, Filter: {} ===",
                userId, page, filter);

        int offset = page * size;

        // SINGLE QUERY: Get contacts with last_chat embedded + total count
        ConversationRepository.ConversationResult result =
                repository.fetchContactsOptimized(userId, search, filter, offset, size);

        List<Map<String, Object>> contacts = result.getData();
        long total = result.getTotalCount();

        if (contacts.isEmpty()) {
            log.info("=== No contacts found - returning empty response in {}ms ===",
                    System.currentTimeMillis() - startTime);
            return buildEmptyResponse(page, size);
        }

        // Process contacts - last_chat is already embedded, just add total_msg_count
        for (Map<String, Object> contact : contacts) {
            // total_msg_count = unread_count (matching the old behavior)
            Object unread = contact.get("unread_count");
            long totalMsgCount = unread == null ? 0L : ((Number) unread).longValue();
            contact.put("total_msg_count", totalMsgCount);

            // Remove total_count from individual records (it was only needed for pagination)
            contact.remove("total_count");
        }

        // Build pagination metadata
        Map<String, Object> users = new LinkedHashMap<>();
        users.put("current_page", page + 1);
        users.put("data", contacts);
        users.put("per_page", size);
        users.put("total", total);
        users.put("last_page", (int) Math.ceil((double) total / size));

        long totalTime = System.currentTimeMillis() - startTime;
        log.info("=== ConversationService getInbox END - Total time: {}ms, Records: {}, Total: {} ===",
                totalTime, contacts.size(), total);

        return Map.of(
                "users", users,
                "channel", List.of()
        );
    }

    /**
     * Build empty response structure
     */
    private Map<String, Object> buildEmptyResponse(int page, int size) {
        Map<String, Object> users = new LinkedHashMap<>();
        users.put("current_page", page + 1);
        users.put("data", Collections.emptyList());
        users.put("per_page", size);
        users.put("total", 0L);
        users.put("last_page", 0);

        return Map.of(
                "users", users,
                "channel", List.of()
        );
    }

    /**
     * OPTIONAL: Get single contact with last chat
     * Useful for individual contact retrieval
     */
    public Map<String, Object> getContactWithLastChat(Integer userId, Integer contactId) {
        log.debug("Fetching single contact: {} for user: {}", contactId, userId);

        // Reuse the optimized query with specific contact filter
        // This could be further optimized with a WHERE contact_id = ? clause
        ConversationRepository.ConversationResult result =
                repository.fetchContactsOptimized(userId, null, null, 0, 1);

        if (result.getData().isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> contact = result.getData().get(0);

        // Add total_msg_count
        Object unread = contact.get("unread_count");
        long totalMsgCount = unread == null ? 0L : ((Number) unread).longValue();
        contact.put("total_msg_count", totalMsgCount);
        contact.remove("total_count");

        return contact;
    }
}