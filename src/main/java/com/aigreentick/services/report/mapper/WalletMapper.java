package com.aigreentick.services.report.mapper;

import com.aigreentick.services.report.dto.walletDTO.BroadcastDTO;
import com.aigreentick.services.report.dto.walletDTO.PageLink;
import com.aigreentick.services.report.dto.walletDTO.TransactionDTO;
import com.aigreentick.services.report.dto.walletDTO.TransactionPageData;
import com.aigreentick.services.report.entity.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class WalletMapper {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public TransactionDTO toTransactionDTO(Wallet wallet) {
        TransactionDTO dto = new TransactionDTO();
        dto.setId(wallet.getId());
        dto.setType(wallet.getType() != null ? wallet.getType() : null);
        dto.setAmount(wallet.getAmount());
        dto.setDescription(wallet.getDescription());

        // Map broadcast if exists
        if (wallet.getBroadcast() != null) {
            BroadcastDTO broadcastDTO = new BroadcastDTO();
            broadcastDTO.setId(wallet.getBroadcast().getId());
            broadcastDTO.setCampname(wallet.getBroadcast().getCampname());
            dto.setBroadcast(broadcastDTO);
        }

        dto.setCreatedAt(wallet.getCreatedAt() != null ? wallet.getCreatedAt().format(DATE_FORMATTER) : null);

        return dto;
    }

    public TransactionPageData toTransactionPageData(Page<TransactionDTO> page, String basePath) {
        TransactionPageData pageData = new TransactionPageData();

        pageData.setCurrentPage(page.getNumber() + 1);
        pageData.setData(page.getContent());
        pageData.setFrom(page.getNumberOfElements() > 0 ? (page.getNumber() * page.getSize()) + 1 : null);
        pageData.setTo(page.getNumberOfElements() > 0 ? (page.getNumber() * page.getSize()) + page.getNumberOfElements() : null);
        pageData.setLastPage(page.getTotalPages());
        pageData.setPerPage(page.getSize());
        pageData.setTotal(page.getTotalElements());
        pageData.setPath(basePath);

        // Build URLs
        pageData.setFirstPageUrl(buildUrl(basePath, 1, page.getSize()));
        pageData.setLastPageUrl(buildUrl(basePath, page.getTotalPages(), page.getSize()));
        pageData.setNextPageUrl(page.hasNext() ? buildUrl(basePath, page.getNumber() + 2, page.getSize()) : null);
        pageData.setPrevPageUrl(page.hasPrevious() ? buildUrl(basePath, page.getNumber(), page.getSize()) : null);

        // Build links
        pageData.setLinks(buildPageLinks(page, basePath));

        return pageData;
    }

    private String buildUrl(String basePath, int page, int size) {
        return basePath + "?page=" + page + "&size=" + size;
    }

    private List<PageLink> buildPageLinks(Page<?> page, String basePath) {
        List<PageLink> links = new ArrayList<>();

        // Previous link
        links.add(new PageLink(
                page.hasPrevious() ? buildUrl(basePath, page.getNumber(), page.getSize()) : null,
                "&laquo; Previous",
                false
        ));

        // Page number links
        int currentPage = page.getNumber() + 1;
        int totalPages = page.getTotalPages();

        int startPage = Math.max(1, currentPage - 2);
        int endPage = Math.min(totalPages, currentPage + 2);

        for (int i = startPage; i <= endPage; i++) {
            links.add(new PageLink(
                    buildUrl(basePath, i, page.getSize()),
                    String.valueOf(i),
                    i == currentPage
            ));
        }

        // Next link
        links.add(new PageLink(
                page.hasNext() ? buildUrl(basePath, page.getNumber() + 2, page.getSize()) : null,
                "Next &raquo;",
                false
        ));

        return links;
    }
}