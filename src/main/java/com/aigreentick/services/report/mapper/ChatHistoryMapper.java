package com.aigreentick.services.report.mapper;

import com.aigreentick.services.report.dto.chatHistoryDTO.ReportDTO;
import com.aigreentick.services.report.dto.chatHistoryDTO.ChatDTO;
import com.aigreentick.services.report.dto.chatHistoryDTO.ConversationDTO;
import com.aigreentick.services.report.dto.chatHistoryDTO.ContactDTO;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class ChatHistoryMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public ConversationDTO mapToConversationDTO(Map<String, Object> row) {
        ContactDTO contactDTO = ContactDTO.builder()
                .id(getInteger(row, "contact_id"))
                .name(getString(row, "contact_name"))
                .mobile(getString(row, "contact_mobile"))
                .email(getString(row, "contact_email"))
                .countryId(getString(row, "contact_country_id"))
                .build();

        ChatDTO chatDTO = null;
        if (row.get("chat_text") != null) {
            chatDTO = ChatDTO.builder()
                    .text(getString(row, "chat_text"))
                    .type(getString(row, "chat_type"))
                    .time(getString(row, "chat_time"))
                    .status(getString(row, "chat_status"))
                    .build();
        }

        ReportDTO reportDTO = null;
        if (row.get("report_id") != null) {
            reportDTO = ReportDTO.builder()
                    .id(getLong(row, "report_id"))
                    .status(getString(row, "report_status"))
                    .build();
        }

        return ConversationDTO.builder()
                .id(getInteger(row, "cm_id"))
                .contactId(getInteger(row, "contact_id"))
                .contact(contactDTO)
                .chat(chatDTO)
                .report(reportDTO)
                .unreadCount(getInteger(row, "unread_count"))
                .lastChatTime(getLong(row, "last_chat_time"))
                .createdAt(getFormattedDateTime(row, "cm_created_at"))
                .build();
    }

    private String getString(Map<String, Object> row, String key) {
        Object value = row.get(key);
        return value != null ? value.toString() : null;
    }

    private Integer getInteger(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof BigInteger) return ((BigInteger) value).intValue();
        return Integer.parseInt(value.toString());
    }

    private Long getLong(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Integer) return ((Integer) value).longValue();
        if (value instanceof BigInteger) return ((BigInteger) value).longValue();
        return Long.parseLong(value.toString());
    }

    private String getFormattedDateTime(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value == null) return null;

        if (value instanceof Timestamp) {
            return ((Timestamp) value).toLocalDateTime().format(FORMATTER);
        }
        if (value instanceof LocalDateTime) {
            return ((LocalDateTime) value).format(FORMATTER);
        }
        return value.toString();
    }
}