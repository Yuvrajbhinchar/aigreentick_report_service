package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.dto.MessageHistoryProjection;
import com.aigreentick.services.report.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageHistoryRepository extends JpaRepository<ContactMessage, Long> {


    @Query(
            value = """
        SELECT
            cm.id,
            cm.contact_id        AS contactId,
            cm.created_at        AS createdAt,

            cc.name,
            cc.mobile,
            cc.email,
            cc.country_id        AS countryId,

            c.text               AS chatText,
            c.type               AS chatType,
            c.time               AS chatTime,
            c.status             AS chatStatus,

            r.id                 AS reportId,
            r.status             AS reportStatus,

            unread_tbl.unread_count AS unreadCount,
            last_msg.last_chat_time AS lastChatTime

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

        WHERE cm.user_id = :userId
        """,
            countQuery = """
        SELECT COUNT(*) FROM (
            SELECT 1
            FROM contacts_messages
            WHERE user_id = :userId
            GROUP BY contact_id
        ) x
      """,
            nativeQuery = true
    )
    Page<MessageHistoryProjection> findMessages(
            @Param("userId") Long userId,
            Pageable pageable
    );
}

