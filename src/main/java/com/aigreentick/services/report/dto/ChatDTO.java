package com.aigreentick.services.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatDTO {
    private String text;
    private String type;
    private String time;
    private String status;
}


