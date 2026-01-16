package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.ChatContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatContactRepository extends JpaRepository<ChatContact, Integer> {
    List<ChatContact> findByIdIn(List<Integer> ids);
}
