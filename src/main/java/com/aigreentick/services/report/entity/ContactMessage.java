package com.aigreentick.services.report.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contacts_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactMessage {

    /* ================= PRIMARY KEY ================= */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ================= REFERENCES (ID ONLY) ================= */

    @Column(name = "contact_id", nullable = false)
    private Long contactId;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "report_id")
    private Long reportId;

    /* ================= TIMESTAMPS ================= */

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

