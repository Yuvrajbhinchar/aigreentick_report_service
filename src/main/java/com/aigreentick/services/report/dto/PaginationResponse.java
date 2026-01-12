package com.aigreentick.services.report.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PaginationResponse<T> {

    @JsonProperty("current_page")
    private int currentPage;

    private List<T> data;
}

