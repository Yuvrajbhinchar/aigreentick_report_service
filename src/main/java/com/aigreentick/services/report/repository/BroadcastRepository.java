package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
    List<Broadcast> findByUserIdOrderByIdDesc(Long userId);
}
