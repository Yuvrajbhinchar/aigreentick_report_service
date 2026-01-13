package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.ChatMessageProjection;
import com.aigreentick.services.report.dto.chatsDTO.ChatMessageResponse;
import com.aigreentick.services.report.repository.ChatPaginationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class ChatPaginationService {

    private final ChatPaginationRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Page<ChatMessageResponse> getChats(
            Long userId,
            Long contactId,
            int page,
            int perPage,
            String search,
            String fromDate,
            String toDate
    ) {
        Pageable pageable =
                PageRequest.of(page, perPage, Sort.by("createdAt").descending());

        return repository.findChats(userId, contactId, pageable)
                .map(this::mapRow);
    }

    private ChatMessageResponse mapRow(ChatMessageProjection row) {
        ChatMessageResponse res = new ChatMessageResponse();

        res.setId(row.getId());
        res.setCategory(row.getCategory());
        res.setReplyMessageId(row.getReplyMessageId());
        res.setMessageId(row.getMessageId());
        res.setReplyFrom(row.getReplyFrom());
        res.setTemplateType(row.getTemplateType());
        res.setChatType(row.getChatType());
        res.setType(row.getType());
        res.setStatus(row.getStatus());
        res.setMessage(row.getMessage());
        res.setSendFrom(row.getSendFrom());
        res.setSendTo(row.getSendTo());
        res.setCreatedAt(row.getCreatedAt());

        res.setPayload(parseJson(row.getPayload()));
        res.setResponse(parseJson(row.getResponse()));
        res.setButtons(cleanArray(parseJsonArray(row.getButtons())));
        res.setChatButtons(cleanArray(parseJsonArray(row.getChatButtons())));
        res.setCarouselCards(cleanArray(parseJsonArray(row.getCarouselCards())));

        return res;
    }

    private Object parseJson(String json) {
        try {
            return json == null ? null : objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return null;
        }
    }

    private List<Map<String, Object>> parseJsonArray(String json) {
        try {
            return json == null
                    ? List.of()
                    : objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            return List.of();
        }
    }

    private List<?> cleanArray(List<Map<String, Object>> list) {
        if (list == null) return List.of();

        return list.stream()
                .filter(map ->
                        map.values().stream().anyMatch(v -> v != null)
                )
                .toList();
    }


}

