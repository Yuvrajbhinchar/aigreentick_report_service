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

    public Map<String, Object> getInbox(
            Integer userId,
            String search,
            String filter,
            int page,
            int size
    ) {

        int offset = page * size;

        List<Map<String, Object>> contacts =
                repository.fetchContacts(userId, search, filter, offset, size);

        long total = repository.countContacts(userId);

        List<Integer> contactIds = contacts.stream()
                .map(r -> ((Number) r.get("id")).intValue())
                .toList();

        Map<Integer, Map<String, Object>> lastChats =
                repository.fetchLastChats(contactIds);

        for (Map<String, Object> row : contacts) {
            Integer contactId = ((Number) row.get("id")).intValue();

            Object unread = row.get("unread_count");
            long totalMsgCount =
                    unread == null ? 0L : ((Number) unread).longValue();

            row.put("last_chat", lastChats.get(contactId));
            row.put("total_msg_count", totalMsgCount);
        }

        Map<String, Object> users = new LinkedHashMap<>();
        users.put("current_page", page + 1);
        users.put("data", contacts);
        users.put("per_page", size);
        users.put("total", total);
        users.put("last_page", (int) Math.ceil((double) total / size));

        return Map.of(
                "users", users,
                "channel", List.of()
        );
    }
}