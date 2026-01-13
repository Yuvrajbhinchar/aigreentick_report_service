package com.aigreentick.services.report.service;

import com.aigreentick.services.report.dto.walletDTO.TransactionDTO;
import com.aigreentick.services.report.dto.walletDTO.TransactionPageData;
import com.aigreentick.services.report.dto.walletDTO.WalletHistoryResponse;
import com.aigreentick.services.report.entity.Wallet;
import com.aigreentick.services.report.mapper.WalletMapper;
import com.aigreentick.services.report.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    public WalletHistoryResponse getWalletHistory(Long userId, int page, int size, String basePath) {
        // Create pageable (Spring Data uses 0-based indexing)
        Pageable pageable = PageRequest.of(page - 1, size);

        // Fetch wallet transactions with broadcast in single query using LEFT JOIN FETCH
        Page<Wallet> walletPage = walletRepository.findByUserIdWithBroadcast(userId, pageable);

        // Map to DTOs - broadcast is already loaded
        List<TransactionDTO> transactionDTOs = walletPage.getContent().stream()
                .map(walletMapper::toTransactionDTO)
                .collect(Collectors.toList());

        // Create page for DTOs
        Page<TransactionDTO> transactionPage = new PageImpl<>(
                transactionDTOs,
                pageable,
                walletPage.getTotalElements()
        );

        // Calculate totals
        Double totalDebit = walletRepository.getTotalDebitByUserId(userId);
        Double totalCredit = walletRepository.getTotalCreditByUserId(userId);
        Double balance = walletRepository.getBalanceByUserId(userId);

        // Build page data
        TransactionPageData pageData = walletMapper.toTransactionPageData(transactionPage, basePath);

        // Build response
        WalletHistoryResponse response = new WalletHistoryResponse();
        response.setMessage("Wallet history retrieved successfully");
        response.setTotalDebit(totalDebit != null ? totalDebit : 0.0);
        response.setTotalCredit(totalCredit != null ? totalCredit : 0.0);
        response.setBalance(balance != null ? balance : 0.0);
        response.setData(pageData);

        return response;
    }
}