package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.ChatMessageRowDTO;
import com.aigreentick.services.report.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatConversationService {

    private final ContactMessageRepository repository;
    private final ObjectMapper objectMapper;

    public Page<Map<String, Object>> getConversation(
            Long userId,
            Long contactId,
            String search,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size);

        Page<ChatMessageRowDTO> raw =
                repository.findConversation(
                        userId, contactId, search, fromDate, toDate, pageable
                );

        List<Map<String, Object>> mapped = new ArrayList<>();

        for (ChatMessageRowDTO r : raw.getContent()) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("message", r.getMessage());
            m.put("type", r.getType());
            m.put("status", r.getStatus());
            m.put("send_from", r.getSendFrom());
            m.put("send_to", r.getSendTo());
            m.put("created_at", r.getCreatedAt());

            m.put("payload", decodeJson(r.getPayload()));
            m.put("response", decodeJson(r.getResponse()));
            m.put("buttons", decodeArray(r.getButtons()));
            m.put("chat_buttons", decodeArray(r.getChatButtons()));
            m.put("carousel_cards", decodeArray(r.getCarouselCards()));

            mapped.add(m);
        }

        // 🔁 SAME AS PHP: reverse after fetch
        Collections.reverse(mapped);

        return new PageImpl<>(
                mapped,
                pageable,
                raw.getTotalElements()
        );
    }

    private Object decodeJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> decodeArray(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<Map<String, Object>> list =
                    objectMapper.readValue(json, new TypeReference<>() {});
            list.removeIf(m -> m.values().stream().allMatch(Objects::isNull));
            return list;
        } catch (Exception e) {
            return List.of();
        }
    }
}

