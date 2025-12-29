package com.aigreentick.services.report.dto;

import java.sql.Date;

public record ChatVolumeReportDTO(
        Date date,
        Long totalMessages
) {}
