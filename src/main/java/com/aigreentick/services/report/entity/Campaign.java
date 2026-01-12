package com.aigreentick.services.report.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaigns")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= RELATION IDS ================= */

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "whatsapp")
    private Integer whatsapp;

    @Column(name = "country_id", nullable = false)
    private Long countryId;

    /* ================= CAMPAIGN INFO ================= */

    @Column(name = "campname", nullable = false, length = 255)
    private String campname;


    @Column(name = "is_media", nullable = false)
    private String isMedia;

    @Column(name = "col_name", length = 255)
    private String colName;

    @Column(name = "total", nullable = false)
    private Integer total = 0;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "schedule_at")
    private LocalDateTime scheduleAt;

    /* ================= TIMESTAMPS ================= */

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /* ================= ENUMS (PIN-TO-PIN) ================= */

    public enum IsMedia {
        _0, _1
    }

    public enum Status {
        _0, _1, _2
    }
}
