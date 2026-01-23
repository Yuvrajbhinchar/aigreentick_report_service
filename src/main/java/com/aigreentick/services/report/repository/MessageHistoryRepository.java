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
public class MessageHistoryRepository {

    private final JdbcTemplate jdbcTemplate;

    /**
     * STEP 1: Get only contact IDs (FAST - minimal data)
     */
    public List<Long> getLatestContactIds(
            Long userId,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int limit,
            int offset
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT cm.contact_id
            FROM contacts_messages cm
            INNER JOIN (
                SELECT contact_id, MAX(id) AS last_message_id
                FROM contacts_messages
                WHERE user_id = ?
                GROUP BY contact_id
            ) lm ON lm.last_message_id = cm.id
            INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
            WHERE cm.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.add(userId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

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

        return jdbcTemplate.queryForList(sql.toString(), params.toArray(), Long.class);
    }

    /**
     * STEP 2: Get full message data for specific contact IDs
     */
    public List<Map<String, Object>> getMessagesForContacts(Long userId, List<Long> contactIds) {
        if (contactIds.isEmpty()) return Collections.emptyList();

        String placeholders = String.join(",", Collections.nCopies(contactIds.size(), "?"));

        String sql = String.format("""
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
                
                r.status AS report_status
                
            FROM contacts_messages cm
            INNER JOIN (
                SELECT contact_id, MAX(id) AS last_message_id
                FROM contacts_messages
                WHERE user_id = ? AND contact_id IN (%s)
                GROUP BY contact_id
            ) lm ON lm.last_message_id = cm.id
            INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
            LEFT JOIN chats c ON c.id = cm.chat_id
            LEFT JOIN reports r ON r.id = cm.report_id
            WHERE cm.contact_id IN (%s)
            ORDER BY cm.id DESC
        """, placeholders, placeholders);

        List<Object> params = new ArrayList<>();
        params.add(userId);
        params.addAll(contactIds);
        params.addAll(contactIds);

        return jdbcTemplate.query(sql, params.toArray(), (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("contact_id", rs.getLong("contact_id"));
            row.put("chat_id", rs.getObject("chat_id"));
            row.put("report_id", rs.getObject("report_id"));
            row.put("created_at", rs.getTimestamp("created_at").toLocalDateTime());
            row.put("cc_name", rs.getString("cc_name"));
            row.put("cc_mobile", rs.getString("cc_mobile"));
            row.put("cc_email", rs.getString("cc_email"));
            row.put("cc_country_id", rs.getString("cc_country_id"));
            row.put("chat_text", rs.getString("chat_text"));
            row.put("chat_type", rs.getString("chat_type"));
            row.put("chat_time", rs.getString("chat_time"));
            row.put("chat_status", rs.getString("chat_status"));
            row.put("report_status", rs.getString("report_status"));
            return row;
        });
    }

    /**
     * STEP 3: Get unread counts for specific contacts
     */
    public Map<Long, Integer> getUnreadCountsForContacts(List<Long> contactIds) {
        if (contactIds.isEmpty()) return Collections.emptyMap();

        String placeholders = String.join(",", Collections.nCopies(contactIds.size(), "?"));

        String sql = String.format("""
            SELECT contact_id, COUNT(*) AS unread_count
            FROM chats
            WHERE contact_id IN (%s)
            AND type = 'recieve'
            AND status = '0'
            GROUP BY contact_id
        """, placeholders);

        List<Map<String, Object>> results = jdbcTemplate.query(
                sql,
                contactIds.toArray(),
                (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("contact_id", rs.getLong("contact_id"));
                    map.put("unread_count", rs.getInt("unread_count"));
                    return map;
                }
        );

        Map<Long, Integer> unreadMap = new HashMap<>();
        for (Map<String, Object> result : results) {
            unreadMap.put((Long) result.get("contact_id"), (Integer) result.get("unread_count"));
        }
        return unreadMap;
    }

    /**
     * STEP 4: Get last chat times for specific contacts
     */
    public Map<Long, Long> getLastChatTimesForContacts(List<Long> contactIds) {
        if (contactIds.isEmpty()) return Collections.emptyMap();

        String placeholders = String.join(",", Collections.nCopies(contactIds.size(), "?"));

        String sql = String.format("""
            SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
            FROM chats
            WHERE contact_id IN (%s)
            GROUP BY contact_id
        """, placeholders);

        List<Map<String, Object>> results = jdbcTemplate.query(
                sql,
                contactIds.toArray(),
                (rs, rowNum) -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("contact_id", rs.getLong("contact_id"));
                    map.put("last_chat_time", rs.getLong("last_chat_time"));
                    return map;
                }
        );

        Map<Long, Long> timeMap = new HashMap<>();
        for (Map<String, Object> result : results) {
            timeMap.put((Long) result.get("contact_id"), (Long) result.get("last_chat_time"));
        }
        return timeMap;
    }

    /**
     * Count total contacts
     */
    public long countTotalContacts(
            Long userId,
            String search,
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