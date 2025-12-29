package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.WalletTransactionDTO;
import com.aigreentick.services.report.entity.Wallet;
import com.aigreentick.services.report.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletReportService {

    private final WalletRepository walletRepository;

    public List<WalletTransactionDTO> getWalletTransactions(
            Long userId,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Wallet.WalletType type,
            Wallet.WalletStatus status,
            Integer broadcastId,
            Integer scheduledId
    ) {

        return walletRepository.findWalletTransactions(
                userId,
                startDate,
                endDate,
                type,
                status,
                broadcastId,
                scheduledId
        );
    }
}
