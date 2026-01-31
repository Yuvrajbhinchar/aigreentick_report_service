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
 * ULTRA-OPTIMIZED V2:
 * - Separate count query (no COUNT(*) OVER())
 * - Lazy row mapping (connection released faster)
 * - Query timeout protection
 * - Reduced lock time
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class ConversationRepository {

    @PersistenceContext
    private EntityManager em;

    private final JdbcTemplate jdbcTemplate;

    private static final int QUERY_TIMEOUT_SECONDS = 10;

    /**
     * OPTIMIZED: Two separate queries - data + count
     * Connection held for MINIMAL time
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
        long startTime = System.currentTimeMillis();

        // Set query timeout to prevent resource hogging
        int originalTimeout = jdbcTemplate.getQueryTimeout();
        jdbcTemplate.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

        try {
            // QUERY 1: Get raw data (FAST - no window function)
            List<RawConversationRow> rawRows = fetchRawData(
                    userId, search, filter, fromDate, toDate, offset, limit
            );

            if (rawRows.isEmpty()) {
                log.debug("No contacts found for user: {}", userId);
                return new ConversationResult(Collections.emptyList(), 0L);
            }

            // QUERY 2: Get total count (FAST - indexed columns only)
            long totalCount = fetchTotalCount(userId, search, filter, fromDate, toDate);

            // PROCESSING: Map to complex structure AFTER connections released
            List<Map<String, Object>> processedData = processRawData(rawRows);

            long totalTime = System.currentTimeMillis() - startTime;
            log.debug("ConversationRepository total time: {}ms (data + count + processing)", totalTime);

            return new ConversationResult(processedData, totalCount);

        } finally {
            // Always reset timeout
            jdbcTemplate.setQueryTimeout(originalTimeout);
        }
    }

    /**
     * STEP 1: Fetch raw data quickly (NO COUNT(*) OVER())
     * Connection released immediately after ResultSet processing
     */
    private List<RawConversationRow> fetchRawData(
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
                
                last_msg.last_chat_time,
                COALESCE(unread_tbl.unread_count, 0) AS unread_count
                
            FROM chat_contacts cc
            
            INNER JOIN (
                SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
                FROM chats
                WHERE user_id = ?
                GROUP BY contact_id
            ) last_msg ON last_msg.contact_id = cc.id
            
            INNER JOIN chats c ON c.contact_id = cc.id
                AND CAST(c.time AS UNSIGNED) = last_msg.last_chat_time
                AND c.user_id = ?
            
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

        // Apply filters
        applyFilters(sql, params, search, filter, fromDate, toDate, userId);

        sql.append(" ORDER BY last_msg.last_chat_time DESC LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);

        // FAST: Simple DTO mapping, minimal object creation
        return jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> new RawConversationRow(
                        // Contact fields
                        rs.getInt("id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getString("mobile"),
                        rs.getString("country_id"),
                        rs.getString("email"),
                        rs.getObject("status"),
                        rs.getObject("time"),
                        rs.getBoolean("allowed_sms"),
                        rs.getBoolean("allowed_broadcast"),
                        rs.getTimestamp("created_at"),
                        rs.getTimestamp("updated_at"),
                        rs.getTimestamp("deleted_at"),

                        // Chat fields
                        rs.getObject("chat_id"),
                        rs.getString("chat_source"),
                        rs.getObject("chat_user_id"),
                        rs.getObject("chat_contact_id"),
                        rs.getString("chat_send_from"),
                        rs.getString("chat_send_to"),
                        rs.getString("chat_send_from_id"),
                        rs.getString("chat_send_to_id"),
                        rs.getString("chat_text"),
                        rs.getString("chat_type"),
                        rs.getString("chat_method"),
                        rs.getString("chat_image_id"),
                        rs.getString("chat_time"),
                        rs.getObject("chat_template_id"),
                        rs.getString("chat_status"),
                        rs.getString("chat_is_media"),
                        rs.getString("chat_contact"),
                        rs.getString("chat_payload"),
                        rs.getString("chat_response"),
                        rs.getString("chat_message_id"),
                        rs.getTimestamp("chat_created_at"),
                        rs.getTimestamp("chat_updated_at"),
                        rs.getTimestamp("chat_deleted_at"),
                        rs.getString("chat_caption"),
                        rs.getString("chat_reply_from"),
                        rs.getString("chat_reply_message_id"),
                        rs.getObject("chat_bot_session_id"),

                        // Metadata
                        rs.getObject("last_chat_time"),
                        rs.getInt("unread_count")
                )
        );
    }

    /**
     * STEP 2: Fetch count separately (FAST - indexed columns)
     */
    private long fetchTotalCount(
            Integer userId,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        StringBuilder sql = new StringBuilder("""
            SELECT COUNT(DISTINCT cc.id)
            FROM chat_contacts cc
            
            INNER JOIN (
                SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
                FROM chats
                WHERE user_id = ?
                GROUP BY contact_id
            ) last_msg ON last_msg.contact_id = cc.id
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId); // subquery

        // Add LEFT JOIN for unread only if filtering by unread
        if ("unread".equalsIgnoreCase(filter)) {
            sql.append("""
                LEFT JOIN (
                    SELECT contact_id, COUNT(*) AS unread_count
                    FROM chats
                    WHERE user_id = ?
                      AND LOWER(TRIM(type)) = 'recieve'
                      AND TRIM(status) = '0'
                    GROUP BY contact_id
                ) unread_tbl ON unread_tbl.contact_id = cc.id
            """);
            params.add(userId);
        }

        sql.append(" WHERE cc.user_id = ? AND last_msg.last_chat_time IS NOT NULL ");
        params.add(userId);

        // Apply same filters as data query
        applyFilters(sql, params, search, filter, fromDate, toDate, userId);

        Long count = jdbcTemplate.queryForObject(sql.toString(), Long.class, params.toArray());
        return count != null ? count : 0L;
    }

    /**
     * STEP 3: Process raw data into complex maps (AFTER connection released)
     */
    private List<Map<String, Object>> processRawData(List<RawConversationRow> rawRows) {
        List<Map<String, Object>> result = new ArrayList<>(rawRows.size());

        for (RawConversationRow raw : rawRows) {
            Map<String, Object> row = new LinkedHashMap<>();

            // Contact fields
            row.put("id", raw.id());
            row.put("user_id", raw.userId());
            row.put("name", raw.name());
            row.put("mobile", raw.mobile());
            row.put("country_id", raw.countryId());
            row.put("email", raw.email());
            row.put("status", raw.status());
            row.put("time", raw.time());
            row.put("allowed_sms", raw.allowedSms() ? 1 : 0);
            row.put("allowed_broadcast", raw.allowedBroadcast() ? 1 : 0);
            row.put("created_at", formatTimestamp(raw.createdAt()));
            row.put("updated_at", formatTimestamp(raw.updatedAt()));
            row.put("deleted_at", formatTimestamp(raw.deletedAt()));

            // Last chat (nested object)
            Map<String, Object> lastChat = new LinkedHashMap<>();
            lastChat.put("id", raw.chatId());
            lastChat.put("source", raw.chatSource());
            lastChat.put("user_id", raw.chatUserId());
            lastChat.put("contact_id", raw.chatContactId());
            lastChat.put("send_from", raw.chatSendFrom());
            lastChat.put("send_to", raw.chatSendTo());
            lastChat.put("send_from_id", raw.chatSendFromId());
            lastChat.put("send_to_id", raw.chatSendToId());
            lastChat.put("text", raw.chatText());
            lastChat.put("type", raw.chatType());
            lastChat.put("method", raw.chatMethod());
            lastChat.put("image_id", raw.chatImageId());
            lastChat.put("time", raw.chatTime());
            lastChat.put("template_id", raw.chatTemplateId());
            lastChat.put("status", raw.chatStatus());
            lastChat.put("is_media", raw.chatIsMedia());
            lastChat.put("contact", raw.chatContact());
            lastChat.put("payload", raw.chatPayload());
            lastChat.put("response", raw.chatResponse());
            lastChat.put("message_id", raw.chatMessageId());
            lastChat.put("created_at", formatTimestamp(raw.chatCreatedAt()));
            lastChat.put("updated_at", formatTimestamp(raw.chatUpdatedAt()));
            lastChat.put("deleted_at", formatTimestamp(raw.chatDeletedAt()));
            lastChat.put("caption", raw.chatCaption());
            lastChat.put("reply_from", raw.chatReplyFrom());
            lastChat.put("reply_message_id", raw.chatReplyMessageId());
            lastChat.put("chat_bot_session_id", raw.chatBotSessionId());

            row.put("last_chat", lastChat);

            // Metadata
            Object lastChatTimeObj = raw.lastChatTime();
            if (lastChatTimeObj instanceof BigInteger) {
                row.put("last_chat_time", ((BigInteger) lastChatTimeObj).longValue());
            } else if (lastChatTimeObj instanceof Number) {
                row.put("last_chat_time", ((Number) lastChatTimeObj).longValue());
            } else {
                row.put("last_chat_time", lastChatTimeObj);
            }

            row.put("unread_count", raw.unreadCount());
            row.put("total_msg_count", raw.unreadCount());

            result.add(row);
        }

        return result;
    }

    /**
     * Apply filters to SQL query
     */
    private void applyFilters(
            StringBuilder sql,
            List<Object> params,
            String search,
            String filter,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            Integer userId
    ) {
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

        // Date range filters
        if (fromDate != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM chats ch WHERE ch.contact_id = cc.id AND ch.created_at >= ?) ");
            params.add(Timestamp.valueOf(fromDate));
        }

        if (toDate != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM chats ch WHERE ch.contact_id = cc.id AND ch.created_at <= ?) ");
            params.add(Timestamp.valueOf(toDate));
        }
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
     * Simple record for raw database row (minimal memory footprint)
     */
    private record RawConversationRow(
            // Contact fields
            int id,
            int userId,
            String name,
            String mobile,
            String countryId,
            String email,
            Object status,
            Object time,
            boolean allowedSms,
            boolean allowedBroadcast,
            Timestamp createdAt,
            Timestamp updatedAt,
            Timestamp deletedAt,

            // Chat fields
            Object chatId,
            String chatSource,
            Object chatUserId,
            Object chatContactId,
            String chatSendFrom,
            String chatSendTo,
            String chatSendFromId,
            String chatSendToId,
            String chatText,
            String chatType,
            String chatMethod,
            String chatImageId,
            String chatTime,
            Object chatTemplateId,
            String chatStatus,
            String chatIsMedia,
            String chatContact,
            String chatPayload,
            String chatResponse,
            String chatMessageId,
            Timestamp chatCreatedAt,
            Timestamp chatUpdatedAt,
            Timestamp chatDeletedAt,
            String chatCaption,
            String chatReplyFrom,
            String chatReplyMessageId,
            Object chatBotSessionId,

            // Metadata
            Object lastChatTime,
            int unreadCount
    ) {}

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