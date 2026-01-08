package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.dto.ChatMessageProjection;
import com.aigreentick.services.report.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatPaginationRepository
        extends JpaRepository<ContactMessage, Long> {

    @Query(
            value = """
        SELECT
            cm.id,
            t.category,
            c.reply_message_id      AS replyMessageId,
            c.message_id            AS messageId,
            c.reply_from            AS replyFrom,

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
              WHEN tc.type = 'BODY'
                THEN CONVERT(tc.text USING utf8mb4)
              ELSE COALESCE(
                CONVERT(c.text USING utf8mb4),
                CONVERT(tc.text USING utf8mb4)
              )
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
        LEFT JOIN reports r ON r.id = cm.report_id
        LEFT JOIN broadcasts b ON b.id = r.broadcast_id
        LEFT JOIN templates t ON t.id = b.template_id
        LEFT JOIN template_components tc ON tc.template_id = t.id
        LEFT JOIN template_component_buttons cb
               ON cb.template_id = t.id AND cb.component_id = tc.id
        LEFT JOIN chat_buttons cbn ON cbn.chat_id = c.id
        LEFT JOIN template_carousel_cards cards
               ON cards.template_id = t.id AND tc.type = 'CAROUSEL'

        WHERE cm.contact_id = :contactId
          AND cm.user_id = :userId

        GROUP BY cm.id, c.id, r.id, t.id, tc.id
        ORDER BY cm.created_at DESC
        """,
            countQuery = """
        SELECT COUNT(*) FROM contacts_messages
        WHERE contact_id = :contactId
          AND user_id = :userId
        """,
            nativeQuery = true
    )
    Page<ChatMessageProjection> findChats(
            @Param("userId") Long userId,
            @Param("contactId") Long contactId,
            Pageable pageable
    );
}
