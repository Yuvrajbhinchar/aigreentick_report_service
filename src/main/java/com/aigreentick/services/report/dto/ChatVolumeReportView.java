package com.aigreentick.services.report.dto;

import java.sql.Date;

public interface ChatVolumeReportView {
    Date getDate();
    Long getTotalMessages();
}
