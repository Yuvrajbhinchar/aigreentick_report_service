package com.aigreentick.services.report.dto;

import com.aigreentick.services.report.entity.Wallet;
import java.time.LocalDateTime;

public record WalletTransactionDTO(
        Long id,
        Wallet.WalletType type,
        Double amount,
        String description,
        String transection,
        Integer broadcastId,
        Integer scheduledId,
        Wallet.WalletStatus status,
        LocalDateTime createdAt
) {}
