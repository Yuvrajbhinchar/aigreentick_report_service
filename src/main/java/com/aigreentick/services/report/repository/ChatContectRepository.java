package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.ChatContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatContectRepository extends JpaRepository<ChatContact, Integer> {
}
