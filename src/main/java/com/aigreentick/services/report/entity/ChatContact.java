package com.aigreentick.services.report.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_contacts",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "unique_user_mobile",
                        columnNames = {"user_id", "mobile"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /* ================= BASIC ================= */

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "mobile", nullable = false, length = 200)
    private String mobile;

    @Column(name = "country_id", nullable = false, length = 10)
    @Builder.Default
    private String countryId = "91";

    @Column(name = "email", length = 100)
    private String email;

    /* ================= FLAGS ================= */

    // tinyint → Byte
    @Column(name = "status", nullable = false)
    @Builder.Default
    private Byte status = 1;

    @Column(name = "time")
    private Integer time;

    @Column(name = "allowed_sms")
    @JdbcTypeCode(SqlTypes.BIT)
    @Builder.Default
    private Boolean allowedSms = false;

    @Column(name = "allowed_broadcast", nullable = false)
    @JdbcTypeCode(SqlTypes.BIT)
    @Builder.Default
    private Boolean allowedBroadcast = true;

    /* ================= TIMESTAMPS ================= */

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
