package com.kalon.service;

import com.kalon.entity.Cart;
import com.kalon.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbandonedCartService {

    private final CartRepository cartRepository;
    private final EmailService emailService;

    /**
     * Check for abandoned carts every 2 hours.
     * Sends reminder email for carts that have items and haven't been updated in 2-24 hours.
     */
    @Scheduled(fixedRate = 7200000) // Every 2 hours
    @Transactional(readOnly = true)
    public void sendAbandonedCartReminders() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusHours(24);

        List<Cart> abandonedCarts = cartRepository.findAbandonedCarts(oneDayAgo, twoHoursAgo);

        for (Cart cart : abandonedCarts) {
            try {
                if (cart.getUser() != null && cart.getUser().isActive() && !cart.getItems().isEmpty()) {
                    emailService.sendAbandonedCartEmail(cart.getUser(), cart.getItems().size());
                }
            } catch (Exception e) {
                log.error("Failed to process abandoned cart for userId={}: {}",
                    cart.getUser().getId(), e.getMessage());
            }
        }

        if (!abandonedCarts.isEmpty()) {
            log.info("Abandoned cart reminders sent: {}", abandonedCarts.size());
        }
    }
}
