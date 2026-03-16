package com.kalon.service;

import com.kalon.dto.NotificationPreferenceDTO;
import com.kalon.entity.NotificationPreference;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.NotificationPreferenceRepository;
import com.kalon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceDTO getPreferences(Long userId) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return toDTO(pref);
    }

    @Transactional
    public NotificationPreferenceDTO updatePreferences(Long userId, NotificationPreferenceDTO dto) {
        NotificationPreference pref = preferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        pref.setOrderUpdates(dto.isOrderUpdates());
        pref.setPromotionalEmails(dto.isPromotionalEmails());
        pref.setStockAlerts(dto.isStockAlerts());
        pref.setReviewReminders(dto.isReviewReminders());
        pref.setAbandonedCartReminders(dto.isAbandonedCartReminders());

        pref = preferenceRepository.save(pref);
        return toDTO(pref);
    }

    private NotificationPreference createDefaultPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        NotificationPreference pref = NotificationPreference.builder()
                .user(user)
                .orderUpdates(true)
                .promotionalEmails(false)
                .stockAlerts(true)
                .reviewReminders(true)
                .abandonedCartReminders(true)
                .build();

        return preferenceRepository.save(pref);
    }

    public boolean canSendAbandonedCartEmail(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .map(NotificationPreference::isAbandonedCartReminders)
                .orElse(true);
    }

    private NotificationPreferenceDTO toDTO(NotificationPreference pref) {
        return NotificationPreferenceDTO.builder()
                .orderUpdates(pref.isOrderUpdates())
                .promotionalEmails(pref.isPromotionalEmails())
                .stockAlerts(pref.isStockAlerts())
                .reviewReminders(pref.isReviewReminders())
                .abandonedCartReminders(pref.isAbandonedCartReminders())
                .build();
    }
}
