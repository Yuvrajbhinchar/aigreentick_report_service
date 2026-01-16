package com.aigreentick.services.report.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.aigreentick.services.report.entity.ContactMessage;

import java.time.LocalDateTime;
import java.util.Map;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {

    @Query(value = """
        SELECT 
            cm.id as cm_id,
            cm.contact_id,
            cm.chat_id,
            cm.report_id,
            cm.created_at as cm_created_at,
            
            cc.id as contact_id,
            cc.name as contact_name,
            cc.mobile as contact_mobile,
            cc.email as contact_email,
            cc.country_id as contact_country_id,
            
            c.text as chat_text,
            c.type as chat_type,
            c.time as chat_time,
            c.status as chat_status,
            
            r.id as report_id,
            r.status as report_status,
            
            (SELECT COUNT(*) FROM chats WHERE contact_id = cm.contact_id AND type = 'recieve' AND status != 'read') as unread_count,
            
            (SELECT UNIX_TIMESTAMP(MAX(created_at)) FROM chats WHERE contact_id = cm.contact_id) as last_chat_time
            
        FROM contacts_messages cm
        INNER JOIN chat_contacts cc ON cm.contact_id = cc.id
        LEFT JOIN chats c ON cm.chat_id = c.id
        LEFT JOIN reports r ON cm.report_id = r.id
        WHERE 1=1
        AND (:fromDate IS NULL OR cm.created_at >= :fromDate)
        AND (:toDate IS NULL OR cm.created_at <= :toDate)
        AND (:search IS NULL OR :search = '' OR 
             cc.name LIKE CONCAT('%', :search, '%') OR 
             cc.mobile LIKE CONCAT('%', :search, '%'))
        ORDER BY 
            COALESCE(
                (SELECT MAX(created_at) FROM chats WHERE contact_id = cm.contact_id),
                (SELECT MAX(created_at) FROM reports WHERE id = cm.report_id),
                cm.created_at
            ) DESC
        """,
            countQuery = """
        SELECT COUNT(DISTINCT cm.id) 
        FROM contacts_messages cm
        INNER JOIN chat_contacts cc ON cm.contact_id = cc.id
        WHERE 1=1
        AND (:fromDate IS NULL OR cm.created_at >= :fromDate)
        AND (:toDate IS NULL OR cm.created_at <= :toDate)
        AND (:search IS NULL OR :search = '' OR 
             cc.name LIKE CONCAT('%', :search, '%') OR 
             cc.mobile LIKE CONCAT('%', :search, '%'))
        """,
            nativeQuery = true)
    Page<Map<String, Object>> findConversationsWithAllData(
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            @Param("search") String search,
            Pageable pageable
    );
}