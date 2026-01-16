package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.ContactMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface MessageHistoryRepository extends JpaRepository<ContactMessage, Long> {

//    /**
//     * Get paginated message history with latest message per contact
//     * Matches PHP implementation exactly
//     */
//    @Query(
//            value = """
//        SELECT
//            cm.id                AS id,
//            cm.contact_id        AS contactId,
//            cm.created_at        AS createdAt,
//
//            cc.name              AS name,
//            cc.mobile            AS mobile,
//            cc.email             AS email,
//            cc.country_id        AS countryId,
//
//            c.text               AS chatText,
//            c.type               AS chatType,
//            c.time               AS chatTime,
//            c.status             AS chatStatus,
//
//            r.id                 AS reportId,
//            r.status             AS reportStatus,
//
//            COALESCE(unread_tbl.unread_count, 0) AS unreadCount,
//            last_msg.last_chat_time AS lastChatTime
//
//        FROM contacts_messages cm
//
//        INNER JOIN (
//            SELECT contact_id, MAX(id) AS max_id
//            FROM contacts_messages
//            WHERE user_id = :userId
//            GROUP BY contact_id
//        ) latest ON latest.max_id = cm.id
//
//        INNER JOIN chat_contacts cc
//            ON cc.id = cm.contact_id
//
//        LEFT JOIN chats c
//            ON c.id = cm.chat_id
//
//        LEFT JOIN reports r
//            ON r.id = cm.report_id
//
//        LEFT JOIN (
//            SELECT contact_id, COUNT(*) AS unread_count
//            FROM chats
//            WHERE LOWER(TRIM(type)) = 'recieve'
//              AND TRIM(status) = '0'
//            GROUP BY contact_id
//        ) unread_tbl ON unread_tbl.contact_id = cc.id
//
//        LEFT JOIN (
//            SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
//            FROM chats
//            GROUP BY contact_id
//        ) last_msg ON last_msg.contact_id = cc.id
//
//        WHERE cm.user_id = :userId
//
//        -- Search filter
//        AND (:search IS NULL OR
//             cc.mobile LIKE CONCAT('%', :search, '%') OR
//             cc.name LIKE CONCAT('%', :search, '%'))
//
//        -- Unread filter
//        AND (:filterUnread = FALSE OR unread_tbl.unread_count IS NOT NULL)
//
//        -- Active filter (last 24 hours)
//        AND (:filterActive = FALSE OR last_msg.last_chat_time >= :activeThreshold)
//
//        -- Date range filter
//        AND (:fromDate IS NULL OR cm.created_at >= :fromDate)
//        AND (:toDate IS NULL OR cm.created_at <= :toDate)
//
//        ORDER BY cm.id DESC
//        """,
//            countQuery = """
//        SELECT COUNT(DISTINCT cm.contact_id)
//        FROM contacts_messages cm
//        INNER JOIN (
//            SELECT contact_id, MAX(id) AS max_id
//            FROM contacts_messages
//            WHERE user_id = :userId
//            GROUP BY contact_id
//        ) latest ON latest.max_id = cm.id
//        INNER JOIN chat_contacts cc ON cc.id = cm.contact_id
//        LEFT JOIN (
//            SELECT contact_id, COUNT(*) AS unread_count
//            FROM chats
//            WHERE LOWER(TRIM(type)) = 'recieve' AND TRIM(status) = '0'
//            GROUP BY contact_id
//        ) unread_tbl ON unread_tbl.contact_id = cc.id
//        LEFT JOIN (
//            SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
//            FROM chats
//            GROUP BY contact_id
//        ) last_msg ON last_msg.contact_id = cc.id
//        WHERE cm.user_id = :userId
//        AND (:search IS NULL OR cc.mobile LIKE CONCAT('%', :search, '%') OR cc.name LIKE CONCAT('%', :search, '%'))
//        AND (:filterUnread = FALSE OR unread_tbl.unread_count IS NOT NULL)
//        AND (:filterActive = FALSE OR last_msg.last_chat_time >= :activeThreshold)
//        AND (:fromDate IS NULL OR cm.created_at >= :fromDate)
//        AND (:toDate IS NULL OR cm.created_at <= :toDate)
//        """,
//            nativeQuery = true
//    )
//    Page<MessageHistoryProjection> findMessageHistory(
//            @Param("userId") Long userId,
//            @Param("search") String search,
//            @Param("filterUnread") boolean filterUnread,
//            @Param("filterActive") boolean filterActive,
//            @Param("activeThreshold") Long activeThreshold,
//            @Param("fromDate") LocalDateTime fromDate,
//            @Param("toDate") LocalDateTime toDate,
//            Pageable pageable
//    );
}