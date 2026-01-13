package com.aigreentick.services.report.dto.walletDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TransactionPageData {
    private Integer currentPage;
    private List<TransactionDTO> data;
    private String firstPageUrl;
    private Integer from;
    private Integer lastPage;
    private String lastPageUrl;
    private List<PageLink> links;
    private String nextPageUrl;
    private String path;
    private Integer perPage;
    private String prevPageUrl;
    private Integer to;
    private Long total;
}
