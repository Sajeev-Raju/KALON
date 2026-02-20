package com.kalon.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "low_stock_alerts_sent")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LowStockAlertSent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "variant_id", unique = true, nullable = false)
    private Long variantId;

    @Column(name = "alerted_at", nullable = false)
    private LocalDateTime alertedAt;

    @PrePersist
    protected void onCreate() {
        alertedAt = LocalDateTime.now();
    }
}
