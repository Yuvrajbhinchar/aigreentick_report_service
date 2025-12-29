package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.WalletTransactionDTO;
import com.aigreentick.services.report.entity.Wallet;
import com.aigreentick.services.report.service.WalletReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/reports/wallet")
@RequiredArgsConstructor
public class WalletReportController {

    private final WalletReportService walletReportService;

    @GetMapping("/transactions")
    public List<WalletTransactionDTO> getWalletTransactions(
            @RequestParam Long userId,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) Wallet.WalletType type,
            @RequestParam(required = false) Wallet.WalletStatus status,
            @RequestParam(required = false) Integer broadcastId,
            @RequestParam(required = false) Integer scheduledId
    ) {
        return walletReportService.getWalletTransactions(
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
