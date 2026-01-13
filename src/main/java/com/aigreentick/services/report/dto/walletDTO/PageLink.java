package com.aigreentick.services.report.dto.walletDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageLink {
    private String url;
    private String label;
    private Boolean active;
}
