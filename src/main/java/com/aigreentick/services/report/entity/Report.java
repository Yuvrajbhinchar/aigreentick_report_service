package com.aigreentick.services.report.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "reports")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ===================== RELATIONS ===================== */

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id")
    private Broadcast broadcast;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id")
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_send_id")
    private SendByGroup groupSend;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_log_id")
    private SendByTagLog tagLog;

    /* ===================== COLUMNS ===================== */

    @Column(name = "mobile", nullable = false, length = 255)
    private String mobile;

    @Column(name = "type", nullable = false)
    @Builder.Default
    private String type = "template";

    @Column(name = "message_id")
    private String messageId;

    @Column(name = "wa_id")
    private String waId;

    @Column(name = "message_status")
    private String messageStatus;

    @Column(name = "status", nullable = false, length = 522)
    private String status;

    @Lob
    @Column(name = "payload", columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private Integer paymentStatus = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response", columnDefinition = "json")
    private Map<String, Object> response;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact", columnDefinition = "json")
    private Map<String, Object> contact;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    @Builder.Default
    private Platform platform = Platform.web;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Platform {
        api, web
    }
}
