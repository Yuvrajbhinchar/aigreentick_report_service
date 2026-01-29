package com.aigreentick.services.report.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Slf4j
public class ConversationRepository {

    @PersistenceContext
    private EntityManager em;

    private final JdbcTemplate jdbcTemplate;

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /* =========================
       ULTRA-OPTIMIZED: Single query with COUNT(*) OVER() to get total count
       Similar to MessageHistoryRepository approach
      ========================= */
    public ConversationResult fetchContactsOptimized(
            Integer userId,
            String search,
            String filter,
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
                cc.created_at,
                cc.updated_at,
                cc.deleted_at,
                lm.last_chat_time,
                ut.unread_count,
                lm.chat_id,
                lm.chat_text,
                lm.chat_type,
                lm.chat_method,
                lm.chat_status,
                lm.chat_created_at,
                COUNT(*) OVER() AS total_count
            FROM chat_contacts cc
            
            /* Last message with full chat details using CAST for performance */
            LEFT JOIN (
                SELECT
                    c.contact_id,
                    CAST(c.time AS UNSIGNED) AS last_chat_time,
                    c.id AS chat_id,
                    c.text AS chat_text,
                    c.type AS chat_type,
                    c.method AS chat_method,
                    c.status AS chat_status,
                    c.created_at AS chat_created_at
                FROM chats c
                INNER JOIN (
                    SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS max_time
                    FROM chats
                    GROUP BY contact_id
                ) max_chats ON c.contact_id = max_chats.contact_id 
                    AND CAST(c.time AS UNSIGNED) = max_chats.max_time
            ) lm ON lm.contact_id = cc.id
            
            /* Unread count */
            LEFT JOIN (
                SELECT
                    contact_id,
                    COUNT(*) AS unread_count
                FROM chats
                WHERE LOWER(TRIM(type)) = 'recieve'
                  AND TRIM(status) = '0'
                GROUP BY contact_id
            ) ut ON ut.contact_id = cc.id
            
            WHERE cc.user_id = ?
              AND lm.last_chat_time IS NOT NULL
        """);

        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (cc.name LIKE ? OR cc.mobile LIKE ?) ");
            String searchPattern = "%" + search + "%";
            params.add(searchPattern);
            params.add(searchPattern);
        }

        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND ut.unread_count IS NOT NULL ");
        }

        if ("active".equalsIgnoreCase(filter)) {
            long activeSince = (System.currentTimeMillis() / 1000) - (24 * 60 * 60);
            sql.append(" AND lm.last_chat_time >= ? ");
            params.add(activeSince);
        }

        sql.append(" ORDER BY lm.last_chat_time DESC ");
        sql.append(" LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);

        log.debug("Executing OPTIMIZED conversation query with total count");

        List<Map<String, Object>> rows = jdbcTemplate.query(
                sql.toString(),
                params.toArray(),
                (rs, rowNum) -> {
                    Map<String, Object> row = new LinkedHashMap<>();

                    // Contact data
                    row.put("id", rs.getInt("id"));
                    row.put("user_id", rs.getInt("user_id"));
                    row.put("name", rs.getString("name"));
                    row.put("mobile", rs.getString("mobile"));
                    row.put("country_id", rs.getString("country_id"));
                    row.put("email", rs.getString("email"));
                    row.put("status", rs.getObject("status"));
                    row.put("time", rs.getObject("time"));
                    row.put("created_at", rs.getTimestamp("created_at") != null
                            ? rs.getTimestamp("created_at").toLocalDateTime().toString()
                            : null);
                    row.put("updated_at", rs.getTimestamp("updated_at") != null
                            ? rs.getTimestamp("updated_at").toLocalDateTime().toString()
                            : null);
                    row.put("deleted_at", rs.getTimestamp("deleted_at") != null
                            ? rs.getTimestamp("deleted_at").toLocalDateTime().toString()
                            : null);

                    // Last chat metadata
                    row.put("last_chat_time", rs.getObject("last_chat_time"));
                    row.put("unread_count", rs.getObject("unread_count"));

                    // Last chat details (embedded as nested map)
                    Map<String, Object> lastChat = new LinkedHashMap<>();
                    lastChat.put("id", rs.getObject("chat_id"));
                    lastChat.put("text", rs.getString("chat_text"));
                    lastChat.put("type", rs.getString("chat_type"));
                    lastChat.put("method", rs.getString("chat_method"));
                    lastChat.put("status", rs.getString("chat_status"));
                    lastChat.put("created_at", rs.getTimestamp("chat_created_at") != null
                            ? rs.getTimestamp("chat_created_at").toLocalDateTime().toString()
                            : null);

                    row.put("last_chat", lastChat);

                    // Total count from window function
                    row.put("total_count", rs.getLong("total_count"));

                    return row;
                }
        );

        // Extract total count from first row (all rows have same total_count)
        long totalCount = rows.isEmpty() ? 0L : ((Number) rows.get(0).get("total_count")).longValue();

        return new ConversationResult(rows, totalCount);
    }

    /* =========================
       FALLBACK: Keep old methods for backward compatibility
       But they should not be used anymore
      ========================= */
    @Deprecated
    public List<Map<String, Object>> fetchContacts(
            Integer userId,
            String search,
            String filter,
            int offset,
            int limit
    ) {
        // Use new optimized method and extract just the data
        ConversationResult result = fetchContactsOptimized(userId, search, filter, offset, limit);
        return result.getData();
    }

    @Deprecated
    public long countContacts(Integer userId, String search, String filter) {
        // This is no longer needed as we get count in the main query
        // But keep for compatibility - just call optimized method with limit 1
        ConversationResult result = fetchContactsOptimized(userId, search, filter, 0, 1);
        return result.getTotalCount();
    }

    @Deprecated
    public Map<Integer, Map<String, Object>> fetchLastChats(List<Integer> contactIds) {
        // This is no longer needed as last chat is included in main query
        // But keep for backward compatibility
        if (contactIds.isEmpty()) return Map.of();

        String sql = """
            SELECT c1.id,
                   c1.source,
                   c1.user_id,
                   c1.contact_id,
                   c1.send_from,
                   c1.send_to,
                   c1.send_from_id,
                   c1.send_to_id,
                   c1.text,
                   c1.type,
                   c1.method,
                   c1.image_id,
                   c1.time,
                   c1.template_id,
                   c1.status,
                   c1.is_media,
                   c1.contact,
                   c1.payload,
                   c1.response,
                   c1.message_id,
                   c1.created_at,
                   c1.updated_at,
                   c1.deleted_at
            FROM chats c1
            JOIN (
                SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS max_time
                FROM chats
                WHERE contact_id IN :ids
                GROUP BY contact_id
            ) c2
              ON c1.contact_id = c2.contact_id
             AND CAST(c1.time AS UNSIGNED) = c2.max_time
        """;

        return jdbcTemplate.query(
                sql.replace(":ids", contactIds.stream()
                        .map(String::valueOf)
                        .collect(java.util.stream.Collectors.joining(","))),
                rs -> {
                    Map<Integer, Map<String, Object>> map = new HashMap<>();
                    while (rs.next()) {
                        Map<String, Object> chat = new LinkedHashMap<>();
                        chat.put("id", rs.getObject(1));
                        chat.put("source", rs.getString(2));
                        chat.put("user_id", rs.getObject(3));
                        chat.put("contact_id", rs.getObject(4));
                        chat.put("send_from", rs.getString(5));
                        chat.put("send_to", rs.getString(6));
                        chat.put("send_from_id", rs.getString(7));
                        chat.put("send_to_id", rs.getString(8));
                        chat.put("text", rs.getString(9));
                        chat.put("type", rs.getString(10));
                        chat.put("method", rs.getString(11));
                        chat.put("image_id", rs.getString(12));
                        chat.put("time", rs.getString(13));
                        chat.put("template_id", rs.getObject(14));
                        chat.put("status", rs.getString(15));
                        chat.put("is_media", rs.getString(16));
                        chat.put("contact", rs.getString(17));
                        chat.put("payload", rs.getString(18));
                        chat.put("response", rs.getString(19));
                        chat.put("message_id", rs.getString(20));
                        chat.put("created_at", rs.getTimestamp(21) != null
                                ? rs.getTimestamp(21).toLocalDateTime().toString()
                                : null);
                        chat.put("updated_at", rs.getTimestamp(22) != null
                                ? rs.getTimestamp(22).toLocalDateTime().toString()
                                : null);
                        chat.put("deleted_at", rs.getTimestamp(23) != null
                                ? rs.getTimestamp(23).toLocalDateTime().toString()
                                : null);

                        map.put(((Number) rs.getObject(4)).intValue(), chat);
                    }
                    return map;
                }
        );
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