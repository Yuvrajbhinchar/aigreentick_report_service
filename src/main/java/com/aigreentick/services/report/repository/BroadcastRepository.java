package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Broadcast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
    List<Broadcast> findByUserIdOrderByIdDesc(Long userId);

    Optional<Broadcast> findByIdAndDeletedAtIsNull(Long id);
}
