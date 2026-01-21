package com.aigreentick.services.report.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

@Repository
@RequiredArgsConstructor
@Slf4j
public class OptimizedMessageHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * STEP 1: Get latest message ID per contact (FAST - indexed query)
     * This matches PHP's subquery approach
     */
    public List<Map<String, Object>> getLatestMessagesWithMetadata(
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
                
                unread_tbl.unread_count,
                last_msg.last_chat_time
                
            FROM contacts_messages cm
            
            INNER JOIN (
                SELECT contact_id, MAX(id) AS last_message_id
                FROM contacts_messages
                WHERE user_id = ?
                GROUP BY contact_id
            ) lm ON lm.last_message_id = cm.id
            
            INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
            
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

        // Unread filter
        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND unread_tbl.unread_count IS NOT NULL ");
        }

        // Active filter (24 hours)
        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            sql.append(" AND last_msg.last_chat_time >= ").append(activeSince).append(" ");
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

        return jdbcTemplate.query(sql.toString(), params.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("contact_id", rs.getLong("contact_id"));
            row.put("chat_id", rs.getObject("chat_id"));
            row.put("report_id", rs.getObject("report_id"));
            row.put("created_at", rs.getTimestamp("created_at").toLocalDateTime());

            row.put("cc_id", rs.getLong("cc_id"));
            row.put("cc_name", rs.getString("cc_name"));
            row.put("cc_mobile", rs.getString("cc_mobile"));
            row.put("cc_email", rs.getString("cc_email"));
            row.put("cc_country_id", rs.getString("cc_country_id"));

            row.put("unread_count", rs.getObject("unread_count") != null ? rs.getInt("unread_count") : 0);
            row.put("last_chat_time", rs.getObject("last_chat_time"));

            return row;
        });
    }

    /**
     * STEP 2: Batch fetch chats for multiple chat_ids (FAST - IN query)
     */
    public Map<Long, Map<String, Object>> getChatsForIds(List<Long> chatIds) {
        if (chatIds == null || chatIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = """
            SELECT
                id,
                text,
                type,
                time,
                status
            FROM chats
            WHERE id IN (%s)
        """.formatted(String.join(",", Collections.nCopies(chatIds.size(), "?")));

        List<Map<String, Object>> chats = jdbcTemplate.query(
                sql,
                chatIds.toArray(),
                (rs, rowNum) -> {
                    Map<String, Object> chat = new LinkedHashMap<>();
                    chat.put("id", rs.getLong("id"));
                    chat.put("text", rs.getString("text"));
                    chat.put("type", rs.getString("type"));
                    chat.put("time", rs.getString("time"));
                    chat.put("status", rs.getString("status"));
                    return chat;
                }
        );

        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (Map<String, Object> chat : chats) {
            result.put((Long) chat.get("id"), chat);
        }
        return result;
    }

    /**
     * STEP 3: Batch fetch reports for multiple report_ids (FAST - IN query)
     */
    public Map<Long, Map<String, Object>> getReportsForIds(List<Long> reportIds) {
        if (reportIds == null || reportIds.isEmpty()) {
            return Collections.emptyMap();
        }

        String sql = """
            SELECT
                id,
                status
            FROM reports
            WHERE id IN (%s)
        """.formatted(String.join(",", Collections.nCopies(reportIds.size(), "?")));

        List<Map<String, Object>> reports = jdbcTemplate.query(
                sql,
                reportIds.toArray(),
                (rs, rowNum) -> {
                    Map<String, Object> report = new LinkedHashMap<>();
                    report.put("id", rs.getLong("id"));
                    report.put("status", rs.getString("status"));
                    return report;
                }
        );

        Map<Long, Map<String, Object>> result = new HashMap<>();
        for (Map<String, Object> report : reports) {
            result.put((Long) report.get("id"), report);
        }
        return result;
    }

    /**
     * Count total records (for pagination)
     */
    public long countTotalMessages(
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
        """);

        List<Object> params = new ArrayList<>();

        if ("unread".equalsIgnoreCase(filter)) {
            sql.append("""
                LEFT JOIN (
                    SELECT contact_id, COUNT(*) AS unread_count
                    FROM chats
                    WHERE LOWER(TRIM(type)) = 'recieve'
                    AND TRIM(status) = '0'
                    GROUP BY contact_id
                ) unread_tbl ON unread_tbl.contact_id = cc.id
            """);
        }

        if ("active".equalsIgnoreCase(filter)) {
            sql.append("""
                LEFT JOIN (
                    SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
                    FROM chats
                    GROUP BY contact_id
                ) last_msg ON last_msg.contact_id = cc.id
            """);
        }

        sql.append(" WHERE cm.user_id = ? ");
        params.add(userId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND unread_tbl.unread_count IS NOT NULL ");
        }

        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            sql.append(" AND last_msg.last_chat_time >= ").append(activeSince).append(" ");
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