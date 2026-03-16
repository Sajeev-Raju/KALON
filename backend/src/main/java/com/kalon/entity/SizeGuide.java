package com.kalon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "size_guides")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SizeGuide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "gender_type")
    @Enumerated(EnumType.STRING)
    private Category.GenderType genderType;

    @Column(columnDefinition = "TEXT")
    private String measurements;

    @Column(name = "size_chart_image")
    private String sizeChartImage;

    @Column(name = "fit_notes", columnDefinition = "TEXT")
    private String fitNotes;

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
