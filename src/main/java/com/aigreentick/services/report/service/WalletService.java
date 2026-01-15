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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletMapper walletMapper;

    public WalletHistoryResponse getWalletHistory(
            Long userId, int page, int size,
            String search, LocalDate fromDate, LocalDate toDate,
            String sortBy, String basePath) {

        // Create pageable (Spring Data uses 0-based indexing)
        Pageable pageable = PageRequest.of(page - 1, size);

        // Build specification for dynamic filtering
        Specification<Wallet> spec = buildSpecification(userId, search, fromDate, toDate, sortBy);

        // Fetch wallet transactions with filters
        Page<Wallet> walletPage = walletRepository.findAll(spec, pageable);

        // Map to DTOs - broadcast will be lazily loaded if needed
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

    private Specification<Wallet> buildSpecification(
            Long userId, String search, LocalDate fromDate,
            LocalDate toDate, String sortBy) {

        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            // Base filter: userId and not deleted
            predicates.add(cb.equal(root.get("userId"), userId));
            predicates.add(cb.isNull(root.get("deletedAt")));

            // Search filter (campaign name)
            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.trim() + "%";
                var broadcastJoin = root.join("broadcast", jakarta.persistence.criteria.JoinType.LEFT);
                predicates.add(cb.like(broadcastJoin.get("campname"), searchPattern));
            }

            // Date range filters
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        cb.function("DATE", LocalDate.class, root.get("createdAt")),
                        fromDate
                ));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        cb.function("DATE", LocalDate.class, root.get("createdAt")),
                        toDate
                ));
            }

            // Sort by type filter
            if (sortBy != null && !sortBy.trim().isEmpty()) {
                String type = sortBy.trim().toLowerCase();
                switch (type) {
                    case "debit":
                        predicates.add(cb.equal(root.get("type"), "debit"));
                        break;
                    case "credit":
                        predicates.add(cb.equal(root.get("type"), "credit"));
                        predicates.add(cb.isNull(root.get("broadcast")));
                        break;
                    case "refund":
                        predicates.add(cb.equal(root.get("type"), "credit"));
                        predicates.add(cb.isNotNull(root.get("broadcast")));
                        break;
                }
            }

            // Order by created_at DESC
            if (query != null) {
                query.orderBy(cb.desc(root.get("createdAt")));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}