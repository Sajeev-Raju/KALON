package com.kalon.service;

import com.kalon.dto.LowStockAlertDTO;
import com.kalon.entity.LowStockAlertSent;
import com.kalon.repository.LowStockAlertSentRepository;
import com.kalon.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockAlertScheduler {

    private final ProductVariantRepository productVariantRepository;
    private final LowStockAlertSentRepository lowStockAlertSentRepository;
    private final SiteConfigService siteConfigService;
    private final EmailService emailService;

    @Scheduled(fixedRate = 3600000) // Every hour
    @Transactional
    public void checkLowStockAndAlert() {
        try {
            // 1. Read admin email — skip if not configured
            String adminEmail = siteConfigService.getLowStockAlertEmail();
            if (adminEmail == null) {
                log.debug("Low stock alert skipped: no admin email configured");
                return;
            }

            // 2. Read threshold
            int threshold = siteConfigService.getLowStockThreshold();

            // 3. Query variants below threshold or reorder point
            List<Object[]> lowStockRows = productVariantRepository.findVariantsBelowThresholdOrReorderPoint(threshold);
            if (lowStockRows.isEmpty()) {
                // Clear all alerts if nothing is low stock anymore
                long clearedCount = lowStockAlertSentRepository.count();
                if (clearedCount > 0) {
                    lowStockAlertSentRepository.deleteAll();
                    log.info("All stock replenished — cleared {} alert records", clearedCount);
                }
                return;
            }

            // 4. Build DTO list and collect current low-stock variant IDs
            List<LowStockAlertDTO> allLowStock = new ArrayList<>();
            Set<Long> currentLowStockVariantIds = new java.util.HashSet<>();
            for (Object[] row : lowStockRows) {
                Long variantId = ((Number) row[0]).longValue();
                currentLowStockVariantIds.add(variantId);
                allLowStock.add(LowStockAlertDTO.builder()
                        .variantId(variantId)
                        .productId(((Number) row[1]).longValue())
                        .productName((String) row[2])
                        .size((String) row[3])
                        .color((String) row[4])
                        .stockQuantity(((Number) row[5]).intValue())
                        .reorderPoint(row[6] != null ? ((Number) row[6]).intValue() : null)
                        .build());
            }

            // 5. Load already-alerted variant IDs
            Set<Long> alreadyAlerted = lowStockAlertSentRepository.findAllAlertedVariantIds();

            // 6. Clear alerted records for replenished variants (no longer in low-stock set)
            List<Long> replenishedIds = alreadyAlerted.stream()
                    .filter(id -> !currentLowStockVariantIds.contains(id))
                    .collect(Collectors.toList());
            if (!replenishedIds.isEmpty()) {
                lowStockAlertSentRepository.deleteByVariantIdIn(replenishedIds);
                log.info("Cleared {} replenished variant alert records", replenishedIds.size());
            }

            // 7. Filter to only NEW alerts (not already alerted)
            List<LowStockAlertDTO> newAlerts = allLowStock.stream()
                    .filter(dto -> !alreadyAlerted.contains(dto.getVariantId()))
                    .collect(Collectors.toList());

            if (newAlerts.isEmpty()) {
                log.debug("Low stock check complete: no new alerts (all {} already notified)", allLowStock.size());
                return;
            }

            // 8. Send single email with all new low-stock variants
            emailService.sendLowStockAlertEmail(adminEmail, newAlerts, threshold);

            // 9. Mark new variants as alerted
            for (LowStockAlertDTO alert : newAlerts) {
                lowStockAlertSentRepository.save(LowStockAlertSent.builder()
                        .variantId(alert.getVariantId())
                        .build());
            }

            log.info("Low stock alert sent: {} new alerts (total low stock: {})", newAlerts.size(), allLowStock.size());

        } catch (Exception e) {
            log.error("Low stock alert scheduler failed: {}", e.getMessage(), e);
        }
    }
}
