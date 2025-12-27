package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.SendByGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SendByGroupRepository extends JpaRepository<SendByGroup, Long> {
}
