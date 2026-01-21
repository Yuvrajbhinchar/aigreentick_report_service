package com.aigreentick.services.report.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UltraFastMessageHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> getLatestMessagesUltraFast(
            Long userId,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                cm.id,
                cm.contact_id,
                cm.chat_id,
                cm.report_id,
                cm.created_at,
                
                cc.id AS cc_id,
                cc.name AS cc_name,
                cc.mobile AS cc_mobile,
                cc.email AS cc_email,
                cc.country_id AS cc_country_id,
                cc.unread_count,
                cc.last_chat_time,
                
                c.text AS chat_text,
                c.type AS chat_type,
                c.time AS chat_time,
                c.status AS chat_status,
                
                r.id AS report_id_val,
                r.status AS report_status
                
            FROM contacts_messages cm
            
            INNER JOIN (
                SELECT contact_id, MAX(id) AS last_message_id
                FROM contacts_messages
                WHERE user_id = ?
                GROUP BY contact_id
            ) lm ON lm.last_message_id = cm.id
            
            INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
            LEFT JOIN chats c ON c.id = cm.chat_id
            LEFT JOIN reports r ON r.id = cm.report_id
            
            WHERE cm.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);

        // Search filter
        if (search != null && !search.isBlank()) {
            sql.append(" AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Unread filter - NOW USES INDEX!
        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND cc.unread_count > 0 ");
        }

        // Active filter - NOW USES INDEX!
        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            sql.append(" AND cc.last_chat_time >= ? ");
            params.add(activeSince);
        }

        // Date filters
        if (fromDate != null) {
            sql.append(" AND cm.created_at >= ? ");
            params.add(Timestamp.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append(" AND cm.created_at <= ? ");
            params.add(Timestamp.valueOf(toDate));
        }

        sql.append(" ORDER BY cm.id DESC LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);

        log.debug("Executing query: {}", sql);
        log.debug("With params: {}", params);

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();

            // Message data
            row.put("id", rs.getLong("id"));
            row.put("contact_id", rs.getLong("contact_id"));
            row.put("chat_id", rs.getObject("chat_id"));
            row.put("report_id", rs.getObject("report_id"));
            row.put("created_at", rs.getTimestamp("created_at").toLocalDateTime());

            // Contact data
            row.put("cc_id", rs.getLong("cc_id"));
            row.put("cc_name", rs.getString("cc_name"));
            row.put("cc_mobile", rs.getString("cc_mobile"));
            row.put("cc_email", rs.getString("cc_email"));
            row.put("cc_country_id", rs.getString("cc_country_id"));
            row.put("unread_count", rs.getInt("unread_count"));
            row.put("last_chat_time", rs.getObject("last_chat_time"));

            // Chat data (already joined)
            row.put("chat_text", rs.getString("chat_text"));
            row.put("chat_type", rs.getString("chat_type"));
            row.put("chat_time", rs.getString("chat_time"));
            row.put("chat_status", rs.getString("chat_status"));

            // Report data (already joined)
            row.put("report_id_val", rs.getObject("report_id_val"));
            row.put("report_status", rs.getString("report_status"));

            return row;
        });
    }

    public long countTotalMessagesUltraFast(
            Long userId,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(DISTINCT cm.contact_id)
            FROM contacts_messages cm
            INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
            WHERE cm.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND cc.unread_count > 0 ");
        }

        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            sql.append(" AND cc.last_chat_time >= ? ");
            params.add(activeSince);
        }

        if (fromDate != null) {
            sql.append(" AND cm.created_at >= ? ");
            params.add(Timestamp.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append(" AND cm.created_at <= ? ");
            params.add(Timestamp.valueOf(toDate));
        }

        Long count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Long.class);
        return count != null ? count : 0L;
    }
}
