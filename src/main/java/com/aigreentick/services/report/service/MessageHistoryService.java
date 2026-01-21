package com.aigreentick.services.report.service;

import com.aigreentick.services.report.repository.MessageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageHistoryService {

    private final MessageHistoryRepository repository;

    public List<Map<String, Object>> getMessagesHistory(
            Long userId,
            int perPage,
            int page,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        int offset = (page - 1) * perPage;

        return repository.fetchMessagesHistory(
                userId,
                perPage,
                offset,
                search,
                filter,
                fromDate,
                toDate
        );
    }
}
