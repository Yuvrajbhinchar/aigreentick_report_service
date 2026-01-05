package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.dto.ChatDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ConversationRepository {

    @PersistenceContext
    private EntityManager em;

    /* =========================
       STEP 1: FETCH CONTACTS
       ========================= */
    public Page<Object[]> fetchContacts(
            Integer userId,
            String search,
            String filter,
            int offset,
            int limit
    ) {

        StringBuilder sql = new StringBuilder("""
            SELECT
              cc.id,
              cc.name,
              cc.mobile,
              last_msg.last_chat_time,
              unread_tbl.unread_count
            FROM chat_contacts cc

            LEFT JOIN (
              SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
              FROM chats
              GROUP BY contact_id
            ) last_msg ON last_msg.contact_id = cc.id

            LEFT JOIN (
              SELECT contact_id, COUNT(*) AS unread_count
              FROM chats
              WHERE LOWER(TRIM(type)) = 'recieve'
                AND TRIM(status) = '0'
              GROUP BY contact_id
            ) unread_tbl ON unread_tbl.contact_id = cc.id

            WHERE cc.user_id = :userId
              AND last_msg.last_chat_time IS NOT NULL
        """);

        if (search != null && !search.isBlank()) {
            sql.append(" AND (cc.name LIKE :search OR cc.mobile LIKE :search) ");
        }

        if ("unread".equalsIgnoreCase(filter)) {
            sql.append(" AND unread_tbl.unread_count IS NOT NULL ");
        }

        if ("active".equalsIgnoreCase(filter)) {
            sql.append(" AND last_msg.last_chat_time >= :activeSince ");
        }

        sql.append(" ORDER BY last_msg.last_chat_time DESC ");

        Query query = em.createNativeQuery(sql.toString());
        query.setParameter("userId", userId);

        if (search != null && !search.isBlank()) {
            query.setParameter("search", "%" + search + "%");
        }

        if ("active".equalsIgnoreCase(filter)) {
            long last24h = (System.currentTimeMillis() / 1000) - (24 * 60 * 60);
            query.setParameter("activeSince", last24h);
        }

        query.setFirstResult(offset);
        query.setMaxResults(limit);

        List<Object[]> rows = query.getResultList();

        Query countQuery = em.createNativeQuery("""
            SELECT COUNT(*) FROM (
              SELECT cc.id
              FROM chat_contacts cc
              LEFT JOIN (
                SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
                FROM chats GROUP BY contact_id
              ) lm ON lm.contact_id = cc.id
              WHERE cc.user_id = :userId
                AND lm.last_chat_time IS NOT NULL
            ) x
        """);

        countQuery.setParameter("userId", userId);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(rows, PageRequest.of(offset / limit, limit), total);
    }

    /* =========================
       STEP 2: FETCH LAST CHATS
       ========================= */
    public Map<Integer, ChatDto> fetchLastChats(List<Integer> contactIds) {

        if (contactIds.isEmpty()) return Map.of();

        Query query = em.createNativeQuery("""
        SELECT c1.id,
               c1.contact_id,
               c1.text,
               c1.type,
               c1.status,
               c1.time
        FROM chats c1
        JOIN (
          SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS max_time
          FROM chats
          GROUP BY contact_id
        ) c2
          ON c1.contact_id = c2.contact_id
         AND CAST(c1.time AS UNSIGNED) = c2.max_time
        WHERE c1.contact_id IN :ids
    """);

        query.setParameter("ids", contactIds);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<Integer, ChatDto> map = new HashMap<>();

        for (Object[] r : rows) {
            ChatDto chat = new ChatDto(
                    ((Number) r[0]).longValue(),   // id
                    ((Number) r[1]).intValue(),    // contact_id
                    (String) r[2],                 // text
                    (String) r[3],                 // type
                    (String) r[4],                 // status
                    (String) r[5]                  // time
            );
            map.put(chat.contactId(), chat);
        }

        return map;

    }
}
