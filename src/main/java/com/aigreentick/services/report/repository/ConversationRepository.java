package com.aigreentick.services.report.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/**
 * ULTRA-OPTIMIZED: Single query with window function to get all data including counts
 * Matches PHP behavior exactly with complete field mapping
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class ConversationRepository {

    @PersistenceContext
    private EntityManager em;

    private final JdbcTemplate jdbcTemplate;

    /**
     * SINGLE OPTIMIZED QUERY: Get contacts with last_chat + counts in one execution
     * Uses window function COUNT(*) OVER() for total count
     * Matches PHP response structure exactly
     */
    public ConversationResult fetchContactsOptimized(
            Integer userId,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int offset,
            int limit
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                cc.id,
                cc.user_id,
                cc.name,
                cc.mobile,
                cc.country_id,
                cc.email,
                cc.status,
                cc.time,
                cc.allowed_sms,
                cc.allowed_broadcast,
                cc.created_at,
                cc.updated_at,
                cc.deleted_at,
                
                -- Last chat details (complete)
                c.id AS chat_id,
                c.source AS chat_source,
                c.user_id AS chat_user_id,
                c.contact_id AS chat_contact_id,
                c.send_from AS chat_send_from,
                c.send_to AS chat_send_to,
                c.send_from_id AS chat_send_from_id,
                c.send_to_id AS chat_send_to_id,
                c.text AS chat_text,
                c.type AS chat_type,
                c.method AS chat_method,
                c.image_id AS chat_image_id,
                c.time AS chat_time,
                c.template_id AS chat_template_id,
                c.status AS chat_status,
                c.is_media AS chat_is_media,
                c.contact AS chat_contact,
                c.payload AS chat_payload,
                c.response AS chat_response,
                c.message_id AS chat_message_id,
                c.created_at AS chat_created_at,
                c.updated_at AS chat_updated_at,
                c.deleted_at AS chat_deleted_at,
                c.caption AS chat_caption,
                c.reply_from AS chat_reply_from,
                c.reply_message_id AS chat_reply_message_id,
                c.chat_bot_session_id AS chat_bot_session_id,
                
                -- Metadata
                last_msg.last_chat_time,
                COALESCE(unread_tbl.unread_count, 0) AS unread_count,
                
                -- Total count using window function
                COUNT(*) OVER() AS total_count
                
            FROM chat_contacts cc
            
            -- Get last chat time per contact
            INNER JOIN (
                SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
                FROM chats
                WHERE user_id = ?
                GROUP BY contact_id
            ) last_msg ON last_msg.contact_id = cc.id
            
            -- Get the actual last chat message
            INNER JOIN chats c ON c.contact_id = cc.id
                AND CAST(c.time AS UNSIGNED) = last_msg.last_chat_time
                AND c.user_id = ?
            
            -- Get unread count per contact
            LEFT JOIN (
                SELECT contact_id, COUNT(*) AS unread_count
                FROM chats
                WHERE user_id = ?
                  AND LOWER(TRIM(type)) = 'recieve'
                  AND TRIM(status) = '0'
                GROUP BY contact_id
            ) unread_tbl ON unread_tbl.contact_id = cc.id
            
            WHERE cc.user_id = ?
              AND last_msg.last_chat_time IS NOT NULL
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId); // last_msg subquery
        params.add(userId); // chats join
        params.add(userId); // unread subquery
        params.add(userId); // main where

        // Search filter
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (cc.mobile LIKE ? OR cc.name LIKE ?) ");
            String searchPattern = "%" + search.trim() + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        // Filter by unread
        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND unread_tbl.unread_count IS NOT NULL ");
        }

        // Filter by active (last 24 hours)
        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - 86400;
            sql.append(" AND last_msg.last_chat_time >= ? ");
            params.add(activeSince);
        }

        // Date range filters (if provided)
        if (fromDate != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM chats ch WHERE ch.contact_id = cc.id AND ch.created_at >= ?) ");
            params.add(Timestamp.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM chats ch WHERE ch.contact_id = cc.id AND ch.created_at <= ?) ");
            params.add(Timestamp.valueOf(toDate));
        }

        sql.append(" ORDER BY last_msg.last_chat_time DESC LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);

        log.debug("Executing optimized conversation query with {} parameters", params.size());

        List<Map<String, Object>> rows = jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();

                    // Contact fields
                    row.put("id", rs.getInt("id"));
                    row.put("user_id", rs.getInt("user_id"));
                    row.put("name", rs.getString("name"));
                    row.put("mobile", rs.getString("mobile"));
                    row.put("country_id", rs.getString("country_id"));
                    row.put("email", rs.getString("email"));
                    row.put("status", rs.getObject("status"));
                    row.put("time", rs.getObject("time"));
                    row.put("allowed_sms", rs.getBoolean("allowed_sms") ? 1 : 0);
                    row.put("allowed_broadcast", rs.getBoolean("allowed_broadcast") ? 1 : 0);
                    row.put("created_at", formatTimestamp(rs.getTimestamp("created_at")));
                    row.put("updated_at", formatTimestamp(rs.getTimestamp("updated_at")));
                    row.put("deleted_at", formatTimestamp(rs.getTimestamp("deleted_at")));

                    // Last chat (complete nested object matching PHP)
                    Map<String, Object> lastChat = new LinkedHashMap<>();
                    lastChat.put("id", rs.getObject("chat_id"));
                    lastChat.put("source", rs.getString("chat_source"));
                    lastChat.put("user_id", rs.getObject("chat_user_id"));
                    lastChat.put("contact_id", rs.getObject("chat_contact_id"));
                    lastChat.put("send_from", rs.getString("chat_send_from"));
                    lastChat.put("send_to", rs.getString("chat_send_to"));
                    lastChat.put("send_from_id", rs.getString("chat_send_from_id"));
                    lastChat.put("send_to_id", rs.getString("chat_send_to_id"));
                    lastChat.put("text", rs.getString("chat_text"));
                    lastChat.put("type", rs.getString("chat_type"));
                    lastChat.put("method", rs.getString("chat_method"));
                    lastChat.put("image_id", rs.getString("chat_image_id"));
                    lastChat.put("time", rs.getString("chat_time"));
                    lastChat.put("template_id", rs.getObject("chat_template_id"));
                    lastChat.put("status", rs.getString("chat_status"));
                    lastChat.put("is_media", rs.getString("chat_is_media"));
                    lastChat.put("contact", rs.getString("chat_contact"));
                    lastChat.put("payload", rs.getString("chat_payload"));
                    lastChat.put("response", rs.getString("chat_response"));
                    lastChat.put("message_id", rs.getString("chat_message_id"));
                    lastChat.put("created_at", formatTimestamp(rs.getTimestamp("chat_created_at")));
                    lastChat.put("updated_at", formatTimestamp(rs.getTimestamp("chat_updated_at")));
                    lastChat.put("deleted_at", formatTimestamp(rs.getTimestamp("chat_deleted_at")));
                    lastChat.put("caption", rs.getString("chat_caption"));
                    lastChat.put("reply_from", rs.getString("chat_reply_from"));
                    lastChat.put("reply_message_id", rs.getString("chat_reply_message_id"));
                    lastChat.put("chat_bot_session_id", rs.getObject("chat_bot_session_id"));

                    row.put("last_chat", lastChat);

                    // Metadata
                    Object lastChatTimeObj = rs.getObject("last_chat_time");
                    if (lastChatTimeObj instanceof BigInteger) {
                        row.put("last_chat_time", ((BigInteger) lastChatTimeObj).longValue());
                    } else if (lastChatTimeObj instanceof Number) {
                        row.put("last_chat_time", ((Number) lastChatTimeObj).longValue());
                    } else {
                        row.put("last_chat_time", lastChatTimeObj);
                    }

                    row.put("unread_count", rs.getInt("unread_count"));
                    row.put("total_msg_count", rs.getInt("unread_count")); // total_msg_count = unread_count (PHP behavior)

                    // Total count from window function
                    row.put("total_count", rs.getLong("total_count"));

                    return row;
                }
        );

        // Extract total count from first row (all rows have same total_count)
        long totalCount = rows.isEmpty()
                ? 0L
                : ((Number) rows.get(0).get("total_count")).longValue();

        // Remove total_count from individual records
        rows.forEach(row -> row.remove("total_count"));

        return new ConversationResult(rows, totalCount);
    }

    /**
     * Format timestamp to string matching PHP format
     */
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        LocalDateTime ldt = timestamp.toLocalDateTime();
        return ldt.toString().replace("T", " ");
    }

    /**
     * Result wrapper containing both data and total count
     */
    public static class ConversationResult {
        private final List<Map<String, Object>> data;
        private final long totalCount;

        public ConversationResult(List<Map<String, Object>> data, long totalCount) {
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