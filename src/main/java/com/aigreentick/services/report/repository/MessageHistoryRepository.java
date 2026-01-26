package com.aigreentick.services.report.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ULTRA-OPTIMIZED: Single query with COUNT(*) OVER() to get total count
 * Eliminates separate count query entirely
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class MessageHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * SINGLE QUERY OPTIMIZATION: Get data + total count in one query
     * Uses COUNT(*) OVER() window function to get total while paginating
     */
    public MessageHistoryResult getLatestMessagesWithCount(
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
                
                cc.name AS cc_name,
                cc.mobile AS cc_mobile,
                cc.email AS cc_email,
                cc.country_id AS cc_country_id,
                
                c.text AS chat_text,
                c.type AS chat_type,
                c.time AS chat_time,
                c.status AS chat_status,
                
                r.id AS report_id_val,
                r.status AS report_status,
                
                COALESCE(unread_tbl.unread_count, 0) AS unread_count,
                last_msg.last_chat_time,
                
                COUNT(*) OVER() AS total_count
                
            FROM contacts_messages cm
            
            INNER JOIN (
                SELECT contact_id, MAX(id) AS last_message_id
                FROM contacts_messages
                WHERE user_id = ?
                GROUP BY contact_id
            ) lm ON lm.last_message_id = cm.id
            
            INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
            
            LEFT JOIN chats c ON c.id = cm.chat_id AND c.user_id = ?
            
            LEFT JOIN reports r ON r.id = cm.report_id
            
            LEFT JOIN (
                SELECT contact_id, COUNT(*) AS unread_count
                FROM chats
                WHERE user_id = ?
                  AND type = 'recieve'
                  AND status = '0'
                GROUP BY contact_id
            ) unread_tbl ON unread_tbl.contact_id = cc.id
            
            LEFT JOIN (
                SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
                FROM chats
                WHERE user_id = ?
                GROUP BY contact_id
            ) last_msg ON last_msg.contact_id = cc.id
            
            WHERE cm.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId); // subquery
        params.add(userId); // chats join
        params.add(userId); // unread subquery
        params.add(userId); // last_msg subquery
        params.add(userId); // main where

        // Search filter
        if (search != null && !search.isBlank()) {
            sql.append(" AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Filter by unread
        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND unread_tbl.unread_count > 0 ");
        }

        // Filter by active (last 24 hours)
        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            sql.append(" AND last_msg.last_chat_time >= ? ");
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

        log.debug("Executing SINGLE optimized query with total count");

        List<Map<String, Object>> rows = jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();

            // Message data
            row.put("id", rs.getLong("id"));
            row.put("contact_id", rs.getLong("contact_id"));
            row.put("chat_id", rs.getObject("chat_id"));
            row.put("report_id", rs.getObject("report_id"));
            row.put("created_at", rs.getTimestamp("created_at").toLocalDateTime());

            // Contact data
            row.put("cc_name", rs.getString("cc_name"));
            row.put("cc_mobile", rs.getString("cc_mobile"));
            row.put("cc_email", rs.getString("cc_email"));
            row.put("cc_country_id", rs.getString("cc_country_id"));

            // Chat data
            row.put("chat_text", rs.getString("chat_text"));
            row.put("chat_type", rs.getString("chat_type"));
            row.put("chat_time", rs.getString("chat_time"));
            row.put("chat_status", rs.getString("chat_status"));

            // Report data
            row.put("report_id_val", rs.getObject("report_id_val"));
            row.put("report_status", rs.getString("report_status"));

            // Metadata
            row.put("unread_count", rs.getInt("unread_count"));
            row.put("last_chat_time", rs.getObject("last_chat_time"));

            // Total count from window function
            row.put("total_count", rs.getLong("total_count"));

            return row;
        });

        // Extract total count from first row (all rows have same total_count)
        long totalCount = rows.isEmpty() ? 0L : ((Number) rows.get(0).get("total_count")).longValue();

        return new MessageHistoryResult(rows, totalCount);
    }

    /**
     * Result wrapper containing both data and total count
     */
    public static class MessageHistoryResult {
        private final List<Map<String, Object>> data;
        private final long totalCount;

        public MessageHistoryResult(List<Map<String, Object>> data, long totalCount) {
            this.data = data;
            this.totalCount = totalCount;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }

        public long getTotalCount() {
            return totalCount;
        }
    }
}