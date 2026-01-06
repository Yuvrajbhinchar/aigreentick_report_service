package com.aigreentick.services.report.repository;


import com.aigreentick.services.report.dto.ChatHistoryRowDTO;
import com.aigreentick.services.report.dto.ChatMessageRowDTO;
import com.aigreentick.services.report.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    @Query(
            value = """
    SELECT
        cm.id                     AS id,
        cm.contact_id             AS contactId,

        cc.name                   AS name,
        cc.mobile                 AS mobile,
        cc.email                  AS email,
        cc.country_id             AS countryId,

        c.text                    AS chatText,
        c.type                    AS chatType,
        c.time                    AS chatTime,
        c.status                  AS chatStatus,

        r.id                      AS reportId,
        r.status                  AS reportStatus,

        unread_tbl.unread_count   AS unreadCount,
        last_msg.last_chat_time   AS lastChatTime,
        cm.created_at             AS createdAt

    FROM contacts_messages cm

    LEFT JOIN chat_contacts cc
        ON cc.id = cm.contact_id

    LEFT JOIN chats c
        ON c.id = cm.chat_id

    LEFT JOIN reports r
        ON r.id = cm.report_id

    LEFT JOIN (
        SELECT contact_id, COUNT(*) AS unread_count
        FROM chats
        WHERE LOWER(TRIM(type)) = 'recieve'
          AND TRIM(status) = '0'
        GROUP BY contact_id
    ) unread_tbl
        ON unread_tbl.contact_id = cc.id

    LEFT JOIN (
        SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
        FROM chats
        GROUP BY contact_id
    ) last_msg
        ON last_msg.contact_id = cc.id

    WHERE cm.user_id = :userId

    AND (
        :search IS NULL
        OR cc.mobile LIKE %:search%
        OR cc.name LIKE %:search%
    )

    AND (
        :filter != 'unread'
        OR unread_tbl.unread_count IS NOT NULL
    )

    AND (
        :filter != 'active'
        OR last_msg.last_chat_time >= :activeAfter
    )

    ORDER BY cm.id DESC
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM contacts_messages cm
    LEFT JOIN chat_contacts cc ON cc.id = cm.contact_id
    WHERE cm.user_id = :userId
    """,
            nativeQuery = true
    )
    Page<ChatHistoryRowDTO> findChatHistory(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("filter") String filter,
            @Param("activeAfter") Long activeAfter,
            Pageable pageable
    );


    @Query(
            value = """
        SELECT
            cm.id AS id,
            t.category AS category,

            c.reply_message_id AS replyMessageId,
            c.message_id AS messageId,
            c.reply_from AS replyFrom,

            CASE
                WHEN tc.type = 'CAROUSEL' THEN 'CAROUSEL'
                ELSE 'STANDARD'
            END AS templateType,

            CASE
                WHEN cm.chat_id IS NOT NULL THEN 'chat'
                ELSE 'broadcast'
            END AS chatType,

            IF(cm.chat_id IS NOT NULL, c.type, 'sent') AS type,
            COALESCE(c.status, r.status, 'sent') AS status,

            CASE
                WHEN tc.type = 'BODY' THEN CONVERT(tc.text USING utf8mb4)
                ELSE COALESCE(CONVERT(c.text USING utf8mb4),
                              CONVERT(tc.text USING utf8mb4))
            END AS message,

            COALESCE(c.payload, r.payload) AS payload,
            COALESCE(c.response, r.response) AS response,

            c.send_from AS sendFrom,
            COALESCE(c.send_to, r.mobile) AS sendTo,

            cm.created_at AS createdAt,

            JSON_ARRAYAGG(
                JSON_OBJECT(
                    'type', cb.type,
                    'number', cb.number,
                    'text', cb.text,
                    'url', cb.url
                )
            ) AS buttons,

            JSON_ARRAYAGG(
                JSON_OBJECT(
                    'id', cbn.id,
                    'type', cbn.type,
                    'text', cbn.text,
                    'url', cbn.url
                )
            ) AS chatButtons,

            CASE
                WHEN tc.type = 'CAROUSEL' THEN
                    JSON_ARRAYAGG(
                        JSON_OBJECT(
                            'card_id', cards.id,
                            'template_id', cards.template_id,
                            'body', cards.body,
                            'media_type', cards.media_type,
                            'image_url', cards.image_url,
                            'parameters', cards.parameters
                        )
                    )
                ELSE NULL
            END AS carouselCards

        FROM contacts_messages cm
        LEFT JOIN chats c ON c.id = cm.chat_id
        LEFT JOIN chat_buttons cbn ON cbn.chat_id = c.id
        LEFT JOIN reports r ON r.id = cm.report_id
        LEFT JOIN broadcasts b ON b.id = r.broadcast_id
        LEFT JOIN templates t ON t.id = b.template_id
        LEFT JOIN template_components tc ON tc.template_id = t.id
        LEFT JOIN template_component_buttons cb
            ON cb.template_id = t.id AND cb.component_id = tc.id
        LEFT JOIN template_carousel_cards cards
            ON cards.template_id = t.id AND tc.type = 'CAROUSEL'

        WHERE cm.user_id = :userId
          AND cm.contact_id = :contactId

        AND (:search IS NULL OR
             c.text LIKE %:search% OR
             c.send_from LIKE %:search% OR
             c.send_to LIKE %:search%)

        AND (:fromDate IS NULL OR cm.created_at >= :fromDate)
        AND (:toDate IS NULL OR cm.created_at <= :toDate)

        GROUP BY cm.id, c.id, r.id, t.id, tc.id
        ORDER BY cm.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*)
        FROM contacts_messages
        WHERE user_id = :userId
          AND contact_id = :contactId
        """,
            nativeQuery = true
    )
    Page<ChatMessageRowDTO> findConversation(
            @Param("userId") Long userId,
            @Param("contactId") Long contactId,
            @Param("search") String search,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );


    //Addtional optimized query
    /*
     @Query(
            value = """
        SELECT
            cm.id                      AS id,
            cm.contact_id              AS contactId,
            cc.name                    AS name,
            cc.mobile                  AS mobile,
            cc.email                   AS email,
            cc.country_id              AS countryId,

            c.text                     AS chatText,
            c.type                     AS chatType,
            c.time                     AS chatTime,
            c.status                   AS chatStatus,

            r.id                       AS reportId,
            r.status                   AS reportStatus,

            IFNULL(unread_tbl.unread_count,0) AS unreadCount,
            last_msg.last_chat_time    AS lastChatTime,
            cm.created_at              AS createdAt
        FROM contacts_messages cm

        JOIN (
            SELECT contact_id, MAX(id) AS last_message_id
            FROM contacts_messages
            WHERE user_id = :userId
            GROUP BY contact_id
        ) lm ON lm.last_message_id = cm.id

        JOIN chat_contacts cc ON cc.id = cm.contact_id
        LEFT JOIN chats c ON c.id = cm.chat_id
        LEFT JOIN reports r ON r.id = cm.report_id

        LEFT JOIN (
            SELECT contact_id, COUNT(*) unread_count
            FROM chats
            WHERE LOWER(TRIM(type)) = 'recieve'
              AND TRIM(status) = '0'
            GROUP BY contact_id
        ) unread_tbl ON unread_tbl.contact_id = cc.id

        LEFT JOIN (
            SELECT contact_id, MAX(CAST(time AS UNSIGNED)) last_chat_time
            FROM chats
            GROUP BY contact_id
        ) last_msg ON last_msg.contact_id = cc.id

        WHERE cm.user_id = :userId
          AND (:search IS NULL
               OR cc.name LIKE %:search%
               OR cc.mobile LIKE %:search%)
          AND (:unreadOnly = FALSE OR unread_tbl.unread_count IS NOT NULL)
          AND (:activeOnly = FALSE OR last_msg.last_chat_time >= :activeAfter)

        ORDER BY cm.id DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM (
            SELECT 1
            FROM contacts_messages cm
            JOIN (
                SELECT contact_id, MAX(id) last_message_id
                FROM contacts_messages
                WHERE user_id = :userId
                GROUP BY contact_id
            ) lm ON lm.last_message_id = cm.id
            JOIN chat_contacts cc ON cc.id = cm.contact_id
            WHERE cm.user_id = :userId
        ) x
        """,
            nativeQuery = true
    )
     */
}

