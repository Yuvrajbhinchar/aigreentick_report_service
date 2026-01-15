package com.aigreentick.services.report.controller;

import com.aigreentick.services.report.dto.walletDTO.WalletHistoryResponse;
import com.aigreentick.services.report.service.WalletService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/transactionHistory")
    public ResponseEntity<WalletHistoryResponse> getWalletHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(name = "from_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "to_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "sort_by", required = false) String sortBy,
            HttpServletRequest request) {

        Long userId = 41L; // replace with auth user

        // Build base path for pagination URLs
        String basePath = request.getRequestURL().toString();

        WalletHistoryResponse response = walletService.getWalletHistory(
                userId, page, size, search, fromDate, toDate, sortBy, basePath
        );

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}