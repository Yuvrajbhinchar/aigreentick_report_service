package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.SendByTagLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SendByTagLogRepository extends JpaRepository<SendByTagLog, Long> {
}
