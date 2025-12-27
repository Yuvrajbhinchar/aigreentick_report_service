package com.aigreentick.services.report.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "send_by_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendByGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= FOREIGN IDS ================= */

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "group_id", nullable = false)
    private Long groupId;

    @Column(name = "country_id")
    private Long countryId;

    /* ================= DATA ================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "is_media", nullable = false)
    private IsMedia isMedia;

    @Column(name = "total", nullable = false)
    @Builder.Default
    private Integer total = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Status status = Status._0;

    @Column(name = "schedule_at")
    private LocalDateTime scheduleAt;

    /* ================= TIMESTAMPS ================= */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ================= ENUMS ================= */

    public enum IsMedia {
        _0, _1
    }

    public enum Status {
        _0, _1
    }
}
