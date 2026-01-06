package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.ChatHistoryRowDTO;
import com.aigreentick.services.report.repository.ContactMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ContactMessageRepository repository;

    public Page<ChatHistoryRowDTO> getHistory(
            Long userId,
            String search,
            String filter,
            int page,
            int size
    ) {

        if (filter == null) {
            filter = "";
        }

        long activeAfter = System.currentTimeMillis() / 1000 - (24 * 60 * 60);

        Pageable pageable = PageRequest.of(page, size);

        return repository.findChatHistory(
                userId,
                search,
                filter,
                activeAfter,
                pageable
        );
    }

}


