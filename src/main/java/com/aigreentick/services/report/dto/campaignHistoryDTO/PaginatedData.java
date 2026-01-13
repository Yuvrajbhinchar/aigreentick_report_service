package com.aigreentick.services.report.dto.campaignHistoryDTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedData<T> {

    @JsonProperty("current_page")
    private Integer currentPage;

    private List<T> data;

    @JsonProperty("first_page_url")
    private String firstPageUrl;

    private Integer from;

    @JsonProperty("last_page")
    private Integer lastPage;

    @JsonProperty("last_page_url")
    private String lastPageUrl;

    private List<PaginationLink> links;

    @JsonProperty("next_page_url")
    private String nextPageUrl;

    private String path;

    @JsonProperty("per_page")
    private Integer perPage;

    @JsonProperty("prev_page_url")
    private String prevPageUrl;

    private Integer to;

    private Long total;
}
