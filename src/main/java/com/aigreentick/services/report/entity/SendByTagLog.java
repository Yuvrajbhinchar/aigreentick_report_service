package com.aigreentick.services.report.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "send_by_tag_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendByTagLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= FOREIGN IDS ================= */

    @Column(name = "tag_log_id", nullable = false)
    private Long tagLogId;

    /* ================= MESSAGE DATA ================= */

    @Column(name = "mobile", nullable = false, length = 255)
    private String mobile;

    @Column(name = "type", nullable = false, length = 255)
    @Builder.Default
    private String type = "template";

    @Column(name = "message_id", length = 255)
    private String messageId;

    @Column(name = "wa_id", length = 255)
    private String waId;

    @Column(name = "message_status", nullable = false, length = 255)
    private String messageStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    /* ================= TIMESTAMPS ================= */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ================= ENUM ================= */

    public enum Status {
        success,
        pending,
        failed
    }
}
