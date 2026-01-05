package com.aigreentick.services.report.repository;

import com.aigreentick.services.report.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

}
