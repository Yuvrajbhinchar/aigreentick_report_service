package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Efficiently get all status counts in a single query for ONE broadcast
     * Returns: [sentCount, readCount, deliveredCount, failedCount, pendingCount, processCount]
     */
    @Query("""
            SELECT 
                COALESCE(SUM(CASE WHEN r.status = 'sent' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN r.status = 'read' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN r.status = 'delivered' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN r.status = 'failed' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN r.status = 'pending' THEN 1 ELSE 0 END), 0),
                COALESCE(SUM(CASE WHEN r.status IN ('process', 'queue') THEN 1 ELSE 0 END), 0)
            FROM Report r
            WHERE r.broadcast.id = :broadcastId
            """)
    Object[] getStatusCountsByBroadcastId(@Param("broadcastId") Long broadcastId);

    /**
     * OPTIMIZED: Get status counts for MULTIPLE broadcasts in a single query
     * This eliminates N+1 query problem
     * Returns: List of [broadcastId, status, count]
     */
    @Query("""
            SELECT r.broadcast.id, r.status, COUNT(r)
            FROM Report r
            WHERE r.broadcast.id IN :broadcastIds
            GROUP BY r.broadcast.id, r.status
            """)
    List<Object[]> getBatchStatusCountsByBroadcastIds(@Param("broadcastIds") List<Long> broadcastIds);

    /**
     * Alternative optimized query using CASE statements for batch processing
     * This returns a more structured result but might be slightly slower
     */
    @Query("""
            SELECT 
                r.broadcast.id,
                SUM(CASE WHEN r.status = 'sent' THEN 1 ELSE 0 END) as sentCount,
                SUM(CASE WHEN r.status = 'read' THEN 1 ELSE 0 END) as readCount,
                SUM(CASE WHEN r.status = 'delivered' THEN 1 ELSE 0 END) as deliveredCount,
                SUM(CASE WHEN r.status = 'failed' THEN 1 ELSE 0 END) as failedCount,
                SUM(CASE WHEN r.status = 'pending' THEN 1 ELSE 0 END) as pendingCount,
                SUM(CASE WHEN r.status IN ('process', 'queue') THEN 1 ELSE 0 END) as processCount
            FROM Report r
            WHERE r.broadcast.id IN :broadcastIds
            GROUP BY r.broadcast.id
            """)
    List<Object[]> getBatchStatusCountsStructured(@Param("broadcastIds") List<Long> broadcastIds);

    long countByBroadcastIdAndStatus(Long broadcastId, String status);
}