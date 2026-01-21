package com.aigreentick.services.report.dto.MessageHistoryDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatInfo {
    private String text;
    private String type;
    private String time;
    private String status;
}
