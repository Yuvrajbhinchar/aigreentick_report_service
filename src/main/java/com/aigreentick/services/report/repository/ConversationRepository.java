package com.aigreentick.services.report.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@Slf4j
public class ConversationRepository {

    @PersistenceContext
    private EntityManager em;

    /* =========================
       FETCH CONTACTS (NO PAGE)
       ========================= */
    public List<Map<String, Object>> fetchContacts(
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

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object[] r : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", r[0]);
            row.put("user_id", r[1]);
            row.put("name", r[2]);
            row.put("mobile", r[3]);
            row.put("country_id", r[4]);
            row.put("email", r[5]);
            row.put("status", r[6]);
            row.put("time", r[7]);
            row.put("created_at", r[8] == null ? null : r[8].toString());
            row.put("updated_at", r[9] == null ? null : r[9].toString());
            row.put("deleted_at", r[10] == null ? null : r[10].toString());
            row.put("last_chat_time", r[11]);
            row.put("unread_count", r[12]);
            result.add(row);
        }

        return result;
    }

    /* =========================
       COUNT CONTACTS
       ========================= */
    public long countContacts(Integer userId) {
        Query q = em.createNativeQuery("""
            SELECT COUNT(*)
            FROM chat_contacts cc
            JOIN (
              SELECT contact_id, MAX(CAST(time AS UNSIGNED)) AS last_chat_time
              FROM chats GROUP BY contact_id
            ) lm ON lm.contact_id = cc.id
            WHERE cc.user_id = :userId
        """);
        q.setParameter("userId", userId);
        return ((Number) q.getSingleResult()).longValue();
    }

    /* =========================
       LAST CHAT PER CONTACT
       ========================= */
    public Map<Integer, Map<String, Object>> fetchLastChats(List<Integer> contactIds) {

        if (contactIds.isEmpty()) return Map.of();

        Query query = em.createNativeQuery("""
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
              FROM chats GROUP BY contact_id
            ) c2
              ON c1.contact_id = c2.contact_id
             AND CAST(c1.time AS UNSIGNED) = c2.max_time
            WHERE c1.contact_id IN :ids
        """);

        query.setParameter("ids", contactIds);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();

        Map<Integer, Map<String, Object>> map = new HashMap<>();

        for (Object[] r : rows) {
            Map<String, Object> chat = new LinkedHashMap<>();
            chat.put("id", r[0]);
            chat.put("source", r[1]);
            chat.put("user_id", r[2]);
            chat.put("contact_id", r[3]);
            chat.put("send_from", r[4]);
            chat.put("send_to", r[5]);
            chat.put("send_from_id", r[6]);
            chat.put("send_to_id", r[7]);
            chat.put("text", r[8]);
            chat.put("type", r[9]);
            chat.put("method", r[10]);
            chat.put("image_id", r[11]);
            chat.put("time", r[12]);
            chat.put("template_id", r[13]);
            chat.put("status", r[14]);
            chat.put("is_media", r[15]);
            chat.put("contact", r[16]);
            chat.put("payload", r[17]);
            chat.put("response", r[18]);
            chat.put("message_id", r[19]);
            chat.put("created_at", r[20] == null ? null : r[20].toString());
            chat.put("updated_at", r[21] == null ? null : r[21].toString());
            chat.put("deleted_at", r[22] == null ? null : r[22].toString());

            map.put(((Number) r[3]).intValue(), chat);
        }

        return map;
    }
}