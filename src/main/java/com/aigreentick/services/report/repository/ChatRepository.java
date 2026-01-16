package com.aigreentick.services.report.repository;


import com.aigreentick.services.report.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Integer> {

    @Query(value = """
        SELECT * FROM chats 
        WHERE contact_id = :contactId 
        ORDER BY created_at DESC 
        LIMIT 1
        """, nativeQuery = true)
    Optional<Chat> findLastChatByContactId(@Param("contactId") Integer contactId);

    @Query(value = """
        SELECT * FROM chats 
        WHERE contact_id IN :contactIds 
        AND id IN (
            SELECT MAX(id) FROM chats 
            WHERE contact_id IN :contactIds 
            GROUP BY contact_id
        )
        """, nativeQuery = true)
    List<Chat> findLastChatsByContactIds(@Param("contactIds") List<Integer> contactIds);

    @Query(value = """
        SELECT COUNT(*) FROM chats 
        WHERE contact_id = :contactId 
        AND type = 'recieve' 
        AND status != 'read'
        """, nativeQuery = true)
    Integer countUnreadByContactId(@Param("contactId") Integer contactId);
}