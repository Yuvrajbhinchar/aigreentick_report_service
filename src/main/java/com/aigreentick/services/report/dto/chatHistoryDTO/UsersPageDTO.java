package com.aigreentick.services.report.dto.chatHistoryDTO;

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
public class UsersPageDTO {
    @JsonProperty("current_page")
    private Integer currentPage;

    private List<ConversationDTO> data;

    @JsonProperty("first_page_url")
    private String firstPageUrl;

    private Integer from;

    @JsonProperty("last_page")
    private Integer lastPage;

    @JsonProperty("last_page_url")
    private String lastPageUrl;

    private List<LinkDTO> links;

    @JsonProperty("next_page_url")
    private String nextPageUrl;

    private String path;

    @JsonProperty("per_page")
    private Integer perPage;

    @JsonProperty("prev_page_url")
    private String prevPageUrl;

    private Integer to;

    private Integer total;
}
