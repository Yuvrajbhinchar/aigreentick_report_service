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
public class ChatPaginationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ChatMessageResult getChatsOptimized(
            Long userId,
            Long contactId,
            String search,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int limit,
            int offset
    ) {

        /* =========================
           STEP 1: FETCH MESSAGE IDS
           ========================= */
        StringBuilder idQuery = new StringBuilder("""
            SELECT cm.id
            FROM contacts_messages cm
            LEFT JOIN chats c ON c.id = cm.chat_id
            LEFT JOIN reports r ON r.id = cm.report_id
            WHERE cm.contact_id = ?
              AND cm.user_id = ?
        """);

        List<Object> idParams = new ArrayList<>();
        idParams.add(contactId);
        idParams.add(userId);

        if (search != null && !search.trim().isEmpty()) {
            idQuery.append("""
                AND (
                    c.text LIKE ? 
                    OR c.send_from LIKE ? 
                    OR c.send_to LIKE ? 
                    OR r.mobile LIKE ?
                )
            """);
            String pattern = "%" + search.trim() + "%";
            idParams.add(pattern);
            idParams.add(pattern);
            idParams.add(pattern);
            idParams.add(pattern);
        }

        if (fromDate != null && toDate != null) {
            idQuery.append(" AND cm.created_at BETWEEN ? AND ? ");
            idParams.add(Timestamp.valueOf(fromDate));
            idParams.add(Timestamp.valueOf(toDate));
        } else if (fromDate != null) {
            idQuery.append(" AND cm.created_at >= ? ");
            idParams.add(Timestamp.valueOf(fromDate));
        } else if (toDate != null) {
            idQuery.append(" AND cm.created_at <= ? ");
            idParams.add(Timestamp.valueOf(toDate));
        }

        idQuery.append(" ORDER BY cm.created_at DESC LIMIT ? OFFSET ? ");
        idParams.add(limit);
        idParams.add(offset);

        List<Long> messageIds = jdbcTemplate.queryForList(
                idQuery.toString(),
                idParams.toArray(),
                Long.class
        );

        if (messageIds.isEmpty()) {
            long total = getTotalCount(userId, contactId, search, fromDate, toDate);
            return new ChatMessageResult(Collections.emptyList(), total);
        }

        /* =========================
           STEP 2: MAIN DATA QUERY
           ========================= */
        StringBuilder sql = new StringBuilder("""
SELECT
    cm.id,
    t.category,
    c.reply_message_id,
    c.message_id,
    c.reply_from,
    c.caption,

    CASE
        WHEN EXISTS (
            SELECT 1 FROM template_components tc2
            WHERE tc2.template_id = t.id
              AND tc2.type = 'CAROUSEL'
        )
        THEN 'CAROUSEL'
        ELSE 'STANDARD'
    END AS template_type,

    CASE
        WHEN cm.chat_id IS NOT NULL THEN 'chat'
        ELSE 'broadcast'
    END AS chat_type,

    IF(cm.chat_id IS NOT NULL, c.type, 'sent') AS type,
    COALESCE(c.status, r.status, 'sent') AS status,

    CASE
        WHEN EXISTS (
            SELECT 1 FROM template_components tc3
            WHERE tc3.template_id = t.id
              AND tc3.type = 'BODY'
        )
        THEN (
            SELECT CONVERT(tc4.text USING utf8mb4)
            FROM template_components tc4
            WHERE tc4.template_id = t.id
              AND tc4.type = 'BODY'
            LIMIT 1
        )
        ELSE COALESCE(
            CONVERT(c.text USING utf8mb4),
            (
                SELECT CONVERT(tc5.text USING utf8mb4)
                FROM template_components tc5
                WHERE tc5.template_id = t.id
                LIMIT 1
            )
        )
    END AS message,

    COALESCE(c.payload, r.payload) AS payload,
    COALESCE(c.response, r.response) AS response,

    c.send_from,
    COALESCE(c.send_to, r.mobile) AS send_to,
    cm.created_at,

    (
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'type', cb.type,
                'number', cb.number,
                'text', cb.text,
                'url', cb.url
            )
        )
        FROM template_component_buttons cb
        WHERE cb.template_id = t.id
    ) AS buttons,

    (
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'id', cbn.id,
                'type', cbn.type,
                'text', cbn.text,
                'url', cbn.url
            )
        )
        FROM chat_buttons cbn
        WHERE cbn.chat_id = c.id
    ) AS chat_buttons,

    (
        SELECT JSON_ARRAYAGG(
            JSON_OBJECT(
                'card_id', cards.id,
                'template_id', cards.template_id,
                'body', cards.body,
                'media_type', cards.media_type,
                'image_url', cards.image_url,
                'parameters', cards.parameters,
                'buttons', (
                    SELECT JSON_ARRAYAGG(
                        JSON_OBJECT(
                            'type', card_btns.type,
                            'number', card_btns.phone_number,
                            'text', card_btns.text,
                            'url', card_btns.url,
                            'parameters', card_btns.parameters
                        )
                    )
                    FROM template_carousel_card_buttons card_btns
                    WHERE card_btns.card_id = cards.id
                )
            )
        )
        FROM template_carousel_cards cards
        WHERE cards.template_id = t.id
    ) AS carousel_cards

FROM contacts_messages cm
LEFT JOIN chats c ON c.id = cm.chat_id
LEFT JOIN reports r ON r.id = cm.report_id
LEFT JOIN broadcasts b ON b.id = r.broadcast_id
LEFT JOIN templates t ON t.id = b.template_id
WHERE cm.id IN (:messageIds)
ORDER BY cm.created_at DESC
""");

        String inClause = messageIds.stream()
                .map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));

        String finalSql = sql.toString().replace(":messageIds", inClause);

        List<Map<String, Object>> rows = jdbcTemplate.query(finalSql, (rs, rowNum) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("category", rs.getString("category"));
            row.put("reply_message_id", rs.getString("reply_message_id"));
            row.put("message_id", rs.getString("message_id"));
            row.put("reply_from", rs.getString("reply_from"));
            row.put("caption", rs.getString("caption"));
            row.put("template_type", rs.getString("template_type"));
            row.put("chat_type", rs.getString("chat_type"));
            row.put("type", rs.getString("type"));
            row.put("status", rs.getString("status"));
            row.put("message", rs.getString("message"));
            row.put("payload", rs.getString("payload"));
            row.put("response", rs.getString("response"));
            row.put("send_from", rs.getString("send_from"));
            row.put("send_to", rs.getString("send_to"));

            Timestamp ts = rs.getTimestamp("created_at");
            row.put("created_at", ts != null ? ts.toLocalDateTime() : null);

            row.put("buttons", rs.getString("buttons"));
            row.put("chat_buttons", rs.getString("chat_buttons"));
            row.put("carousel_cards", rs.getString("carousel_cards"));
            return row;
        });

        long total = getTotalCount(userId, contactId, search, fromDate, toDate);
        return new ChatMessageResult(rows, total);
    }

    /* =========================
       COUNT QUERY
       ========================= */
    private long getTotalCount(
            Long userId,
            Long contactId,
            String search,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {

        StringBuilder countSql = new StringBuilder("""
            SELECT COUNT(cm.id)
            FROM contacts_messages cm
            LEFT JOIN chats c ON c.id = cm.chat_id
            LEFT JOIN reports r ON r.id = cm.report_id
            WHERE cm.contact_id = ?
              AND cm.user_id = ?
        """);

        List<Object> params = new ArrayList<>();
        params.add(contactId);
        params.add(userId);

        if (search != null && !search.trim().isEmpty()) {
            countSql.append("""
                AND (
                    c.text LIKE ? 
                    OR c.send_from LIKE ? 
                    OR c.send_to LIKE ? 
                    OR r.mobile LIKE ?
                )
            """);
            String pattern = "%" + search.trim() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (fromDate != null && toDate != null) {
            countSql.append(" AND cm.created_at BETWEEN ? AND ? ");
            params.add(Timestamp.valueOf(fromDate));
            params.add(Timestamp.valueOf(toDate));
        } else if (fromDate != null) {
            countSql.append(" AND cm.created_at >= ? ");
            params.add(Timestamp.valueOf(fromDate));
        } else if (toDate != null) {
            countSql.append(" AND cm.created_at <= ? ");
            params.add(Timestamp.valueOf(toDate));
        }

        Long count = jdbcTemplate.queryForObject(
                countSql.toString(),
                params.toArray(),
                Long.class
        );
        return count != null ? count : 0L;
    }

    /* =========================
       RESULT WRAPPER
       ========================= */
    public static class ChatMessageResult {
        private final List<Map<String, Object>> data;
        private final long totalCount;

        public ChatMessageResult(List<Map<String, Object>> data, long totalCount) {
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
