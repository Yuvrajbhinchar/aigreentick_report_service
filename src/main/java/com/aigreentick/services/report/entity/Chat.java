package com.aigreentick.services.report.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "chats")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {

    /* ================= PRIMARY KEY ================= */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /* ================= BASIC ================= */

    @Column(name = "source", nullable = false, length = 255)
    private String source;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "contact_id")
    private Integer contactId;

    @Column(name = "send_from", nullable = false, length = 525)
    private String sendFrom;

    @Column(name = "send_to", nullable = false, length = 525)
    private String sendTo;

    @Column(name = "send_from_id", length = 522)
    private String sendFromId;

    @Column(name = "send_to_id", length = 522)
    private String sendToId;

    /* ================= MESSAGE ================= */

    @Column(name = "text", columnDefinition = "longtext")
    private String text;

    @Column(name = "type", nullable = false, length = 525)
    private String type;

    @Column(name = "method", length = 522)
    private String method;

    @Column(name = "image_id", length = 522)
    private String imageId;

    @Column(name = "time", length = 522)
    private String time;

    @Column(name = "template_id")
    private Integer templateId;

    @Column(name = "status", nullable = false, length = 522)
    private String status;

    /* ================= FLAGS ================= */

    @Enumerated(EnumType.STRING)
    @Column(name = "is_media", nullable = false, columnDefinition = "enum('0','1')")
    private MediaFlag isMedia;

    /* ================= JSON ================= */

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "contact", columnDefinition = "json")
    private String contact;

    @Column(name = "payload", columnDefinition = "text")
    private String payload;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response", columnDefinition = "json")
    private String response;

    /* ================= META ================= */

    @Column(name = "message_id", nullable = false, length = 525)
    private String messageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "caption", columnDefinition = "text")
    private String caption;

    @Column(name = "reply_from", length = 525)
    private String replyFrom;

    @Column(name = "reply_message_id", length = 525)
    private String replyMessageId;

    @Column(name = "chat_bot_session_id")
    private Integer chatBotSessionId;

    public enum MediaFlag {
        _0, _1
    }
}
