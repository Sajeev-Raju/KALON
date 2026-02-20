package com.kalon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_activity_logs", indexes = {
    @Index(name = "idx_activity_admin_id", columnList = "admin_id"),
    @Index(name = "idx_activity_action", columnList = "action"),
    @Index(name = "idx_activity_resource_type", columnList = "resource_type"),
    @Index(name = "idx_activity_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id", nullable = false)
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Action action;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "resource_name")
    private String resourceName;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum Action {
        CREATE, UPDATE, DELETE, STATUS_CHANGE, APPROVE, REJECT, PROCESS, COMPLETE, UPLOAD, LOGIN
    }

    public enum ResourceType {
        PRODUCT, CATEGORY, ORDER, USER, RETURN_REQUEST, REVIEW, SITE_CONFIG, FILE, AUTH
    }
}
