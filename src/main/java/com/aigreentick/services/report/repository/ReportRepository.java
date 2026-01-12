package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReportRepository extends JpaRepository<Report, Long> {

    @Query("""
        SELECT COUNT(r) FROM Report r
        WHERE r.broadcast.id = :broadcastId AND r.status = :status
    """)
    long countByBroadcastAndStatus(Long broadcastId, String status);
}
