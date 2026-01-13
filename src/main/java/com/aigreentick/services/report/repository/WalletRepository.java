package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    @Query("SELECT w FROM Wallet w " +
            "LEFT JOIN FETCH w.broadcast " +
            "WHERE w.userId = :userId AND w.deletedAt IS NULL " +
            "ORDER BY w.createdAt DESC")
    Page<Wallet> findByUserIdWithBroadcast(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(w.amount), 0.0) FROM Wallet w WHERE w.userId = :userId AND w.type = 'debit' AND w.deletedAt IS NULL")
    Double getTotalDebitByUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(w.amount), 0.0) FROM Wallet w WHERE w.userId = :userId AND w.type = 'credit' AND w.deletedAt IS NULL")
    Double getTotalCreditByUserId(@Param("userId") Long userId);

    @Query("SELECT (COALESCE(SUM(CASE WHEN w.type = 'credit' THEN w.amount ELSE 0 END), 0.0) - " +
            "COALESCE(SUM(CASE WHEN w.type = 'debit' THEN w.amount ELSE 0 END), 0.0)) " +
            "FROM Wallet w WHERE w.userId = :userId AND w.deletedAt IS NULL")
    Double getBalanceByUserId(@Param("userId") Long userId);
}