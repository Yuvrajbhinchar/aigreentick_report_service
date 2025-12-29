package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.dto.WalletTransactionDTO;
import com.aigreentick.services.report.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("""
        select new com.aigreentick.services.report.dto.WalletTransactionDTO(
            w.id,
            w.type,
            w.amount,
            w.description,
            w.transection,
            w.broadcastId,
            w.scheduledId,
            w.status,
            w.createdAt
        )
        from Wallet w
        where w.userId = :userId
          and w.deletedAt is null
          and (:startDate is null or w.createdAt >= :startDate)
          and (:endDate is null or w.createdAt <= :endDate)
          and (:type is null or w.type = :type)
          and (:status is null or w.status = :status)
          and (:broadcastId is null or w.broadcastId = :broadcastId)
          and (:scheduledId is null or w.scheduledId = :scheduledId)
        order by w.createdAt desc
    """)
    List<WalletTransactionDTO> findWalletTransactions(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("type") Wallet.WalletType type,
            @Param("status") Wallet.WalletStatus status,
            @Param("broadcastId") Integer broadcastId,
            @Param("scheduledId") Integer scheduledId
    );
}
