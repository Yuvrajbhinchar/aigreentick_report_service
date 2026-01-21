package com.aigreentick.services.report.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MessageHistoryRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public List<Map<String, Object>> fetchMessagesHistory(
            Long userId,
            int limit,
            int offset,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        String sql = """
            SELECT
                cm.id,
                cm.contact_id,
                cm.chat_id,
                cm.report_id,
                cm.created_at,

                cc.id AS cc_id,
                cc.name,
                cc.mobile,
                cc.email,
                cc.country_id,

                c.text AS chat_text,
                c.type AS chat_type,
                c.time AS chat_time,
                c.status AS chat_status,

                r.id AS r_id,
                r.status AS r_status,

                unread_tbl.unread_count,
                last_msg.last_chat_time

            FROM contacts_messages cm

            INNER JOIN (
                SELECT contact_id, MAX(id) AS last_message_id
                FROM contacts_messages
                WHERE user_id = :userId
                GROUP BY contact_id
            ) lm ON lm.last_message_id = cm.id

            INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
            LEFT JOIN chats c ON c.id = cm.chat_id
            LEFT JOIN reports r ON r.id = cm.report_id

            LEFT JOIN (
                SELECT contact_id, COUNT(*) AS unread_count
                FROM chats
                WHERE LOWER(TRIM(type)) = 'recieve'
                AND TRIM(status) = '0'
                GROUP BY contact_id
            ) unread_tbl ON unread_tbl.contact_id = cc.id

            LEFT JOIN (
                SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
                FROM chats
                GROUP BY contact_id
            ) last_msg ON last_msg.contact_id = cc.id

            WHERE cm.user_id = :userId
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("limit", limit);
        params.put("offset", offset);

        if (search != null && !search.isBlank()) {
            sql += " AND (cc.mobile LIKE :search OR cc.name LIKE :search) ";
            params.put("search", "%" + search + "%");
        }

        if ("unread".equalsIgnoreCase(filter)) {
            sql += " AND unread_tbl.unread_count IS NOT NULL ";
        }

        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            sql += " AND last_msg.last_chat_time >= " + activeSince;
        }

        if (fromDate != null) {
            sql += " AND cm.created_at >= :fromDate ";
            params.put("fromDate", fromDate);
        }

        if (toDate != null) {
            sql += " AND cm.created_at <= :toDate ";
            params.put("toDate", toDate);
        }

        sql += " ORDER BY cm.id DESC LIMIT :limit OFFSET :offset ";

        return jdbc.query(sql, params, (rs, rowNum) -> {
            Map<String, Object> item = new LinkedHashMap<>();

            item.put("id", rs.getLong("id"));
            item.put("contact_id", rs.getLong("contact_id"));

            Map<String, Object> contact = new LinkedHashMap<>();
            contact.put("id", rs.getLong("cc_id"));
            contact.put("name", rs.getString("name"));
            contact.put("mobile", rs.getString("mobile"));
            contact.put("email", rs.getString("email"));
            contact.put("country_id", rs.getLong("country_id"));
            item.put("contact", contact);

            if (rs.getObject("chat_id") != null) {
                Map<String, Object> chat = new LinkedHashMap<>();
                chat.put("text", rs.getString("chat_text"));
                chat.put("type", rs.getString("chat_type"));
                chat.put("time", rs.getString("chat_time"));
                chat.put("status", rs.getString("chat_status"));
                item.put("chat", chat);
            } else {
                item.put("chat", null);
            }

            if (rs.getObject("report_id") != null) {
                Map<String, Object> report = new LinkedHashMap<>();
                report.put("id", rs.getLong("r_id"));
                report.put("status", rs.getString("r_status"));
                item.put("report", report);
            } else {
                item.put("report", null);
            }

            item.put("unread_count", rs.getInt("unread_count"));
            item.put("last_chat_time", rs.getObject("last_chat_time"));
            item.put("created_at", rs.getTimestamp("created_at"));

            return item;
        });
    }
}
