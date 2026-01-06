package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.ChatDto;
import com.aigreentick.services.report.dto.ConversationDto;
import com.aigreentick.services.report.repository.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationService {

    private final ConversationRepository repository;

    public Page<ConversationDto> getInbox(
            Integer userId,
            String search,
            String filter,
            int page,
            int size
    ) {

        int offset = page * size;

        Page<Object[]> contacts =
                repository.fetchContacts(userId, search, filter, offset, size);

        List<Integer> contactIds = contacts.getContent().stream()
                .map(r -> ((Number) r[0]).intValue())
                .toList();

        Map<Integer, ChatDto> lastChats =
                repository.fetchLastChats(contactIds);

        return contacts.map(r -> {
            Integer contactId = ((Number) r[0]).intValue();

            return new ConversationDto(
                    contactId,
                    (String) r[1],                          // name
                    (String) r[2],                          // mobile
                    ((Number) r[3]).longValue(),            // lastChatTime
                    r[4] == null ? 0L : ((Number) r[4]).longValue(),
                    lastChats.get(contactId)                // ✅ ChatDto
            );
        });
    }

}
