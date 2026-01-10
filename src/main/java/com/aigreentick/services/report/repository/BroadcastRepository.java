package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Broadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {

    @Query("""
        SELECT b
        FROM Broadcast b
        WHERE b.userId = :userId
          AND b.deletedAt IS NULL

          AND (
              :search IS NULL OR
              LOWER(b.campname) LIKE LOWER(CONCAT('%', :search, '%')) OR
              EXISTS (
                  SELECT 1
                  FROM Report r
                  WHERE r.broadcast.id = b.id
                    AND LOWER(r.mobile) LIKE LOWER(CONCAT('%', :search, '%'))
              )
          )

          AND (
              :type IS NULL OR
              EXISTS (
                  SELECT 1
                  FROM Report r
                  WHERE r.broadcast.id = b.id
                    AND CAST(r.platform AS string) = :type
              )
          )

          AND (
              :state IS NULL OR
              (
                  :state = 'pending' AND EXISTS (
                      SELECT 1 FROM Report r
                      WHERE r.broadcast.id = b.id AND r.status = 'pending'
                  )
              ) OR
              (
                  :state = 'failed' AND EXISTS (
                      SELECT 1 FROM Report r
                      WHERE r.broadcast.id = b.id AND r.status = 'failed'
                  )
              ) OR
              (
                  :state = 'completed' AND NOT EXISTS (
                      SELECT 1 FROM Report r
                      WHERE r.broadcast.id = b.id AND r.status = 'pending'
                  )
              )
          )

          AND (:fromDate IS NULL OR b.createdAt >= :fromDate)
          AND (:toDate   IS NULL OR b.createdAt <  :toDate)

        ORDER BY b.id DESC
    """)
    Page<Broadcast> findBroadcastsWithFilters(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("type") String type,
            @Param("state") String state,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable
    );

    @Query("""
        SELECT b
        FROM Broadcast b
        WHERE b.userId = :userId
          AND b.deletedAt IS NULL
        ORDER BY b.id DESC
    """)
    Page<Broadcast> findByUserIdOrderByIdDesc(
            @Param("userId") Long userId,
            Pageable pageable
    );
}
