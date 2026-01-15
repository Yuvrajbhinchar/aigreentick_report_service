package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.*;
import com.aigreentick.services.report.dto.massagesHistoryDTO.MessageHistoryResponse;
import com.aigreentick.services.report.dto.massagesHistoryDTO.ChatDTO;
import com.aigreentick.services.report.dto.massagesHistoryDTO.ContactDTO;
import com.aigreentick.services.report.dto.massagesHistoryDTO.ReportDTO;
import com.aigreentick.services.report.repository.MessageHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessageHistoryService {

    private final MessageHistoryRepository repository;

    public Page<MessageHistoryResponse> getMessages(Long userId, Pageable pageable) {

        Page<MessageHistoryProjection> page =
                repository.findMessages(userId, pageable);

        return page.map(row -> {
            MessageHistoryResponse res = new MessageHistoryResponse();

            res.setId(row.getId());
            res.setContactId(row.getContactId());
            res.setUnreadCount(
                    row.getUnreadCount() != null ? row.getUnreadCount() : 0
            );
            res.setLastChatTime(row.getLastChatTime());
            res.setCreatedAt(row.getCreatedAt());

            ContactDTO contact = new ContactDTO();
            contact.setId(row.getContactId());
            contact.setName(row.getName());
            contact.setMobile(row.getMobile());
            contact.setEmail(row.getEmail());
            contact.setCountryId(row.getCountryId());
            res.setContact(contact);

            if (row.getChatText() != null) {
                ChatDTO chat = new ChatDTO();
                chat.setText(row.getChatText());
                chat.setType(row.getChatType());
                chat.setTime(row.getChatTime());
                chat.setStatus(row.getChatStatus());
                res.setChat(chat);
            }

            if (row.getReportId() != null) {
                ReportDTO report = new ReportDTO();
                report.setId(row.getReportId());
                report.setStatus(row.getReportStatus());
                res.setReport(report);
            }

            return res;
        });
    }
}