package com.kalon.service;

import com.kalon.dto.AdminActivityLogDTO;
import com.kalon.entity.AdminActivityLog;
import com.kalon.entity.User;
import com.kalon.repository.AdminActivityLogRepository;
import com.kalon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminActivityLogService {

    private final AdminActivityLogRepository activityLogRepository;
    private final UserRepository userRepository;

    /**
     * Log an admin action. Runs in a separate transaction so failures don't affect the main operation.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AdminActivityLog.Action action,
                    AdminActivityLog.ResourceType resourceType,
                    Long resourceId,
                    String resourceName,
                    String details) {
        try {
            User admin = getCurrentAdmin();
            if (admin == null) return;

            AdminActivityLog logEntry = AdminActivityLog.builder()
                    .admin(admin)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .resourceName(resourceName)
                    .details(details)
                    .build();

            activityLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to log admin activity: {} {} {}", action, resourceType, resourceId, e);
        }
    }

    /**
     * Log an admin action with a known admin user (for cases where SecurityContext may not be available).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(Long adminId,
                    AdminActivityLog.Action action,
                    AdminActivityLog.ResourceType resourceType,
                    Long resourceId,
                    String resourceName,
                    String details) {
        try {
            User admin = userRepository.findById(adminId).orElse(null);
            if (admin == null) return;

            AdminActivityLog logEntry = AdminActivityLog.builder()
                    .admin(admin)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .resourceName(resourceName)
                    .details(details)
                    .build();

            activityLogRepository.save(logEntry);
        } catch (Exception e) {
            log.error("Failed to log admin activity: {} {} {}", action, resourceType, resourceId, e);
        }
    }

    public Page<AdminActivityLogDTO> getActivityLogs(Pageable pageable) {
        return activityLogRepository.findAll(pageable).map(this::toDTO);
    }

    public Page<AdminActivityLogDTO> searchActivityLogs(String search, Pageable pageable) {
        return activityLogRepository.searchLogs(search.trim(), pageable).map(this::toDTO);
    }

    public Page<AdminActivityLogDTO> getActivityLogsWithFilters(Long adminId,
                                                                 AdminActivityLog.Action action,
                                                                 AdminActivityLog.ResourceType resourceType,
                                                                 LocalDateTime dateFrom,
                                                                 LocalDateTime dateTo,
                                                                 Pageable pageable) {
        return activityLogRepository.findWithFilters(adminId, action, resourceType, dateFrom, dateTo, pageable)
                .map(this::toDTO);
    }

    public java.util.List<java.util.Map<String, Object>> getDistinctAdmins() {
        return activityLogRepository.findDistinctAdmins().stream()
                .map(user -> {
                    java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
                    map.put("id", user.getId());
                    map.put("name", user.getFirstName() + " " + user.getLastName());
                    map.put("email", user.getEmail());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
    }

    private User getCurrentAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;

        String email = auth.getName();
        return userRepository.findByEmail(email).orElse(null);
    }

    private AdminActivityLogDTO toDTO(AdminActivityLog logEntry) {
        return AdminActivityLogDTO.builder()
                .id(logEntry.getId())
                .adminId(logEntry.getAdmin().getId())
                .adminName(logEntry.getAdmin().getFirstName() + " " + logEntry.getAdmin().getLastName())
                .adminEmail(logEntry.getAdmin().getEmail())
                .action(logEntry.getAction().name())
                .resourceType(logEntry.getResourceType().name())
                .resourceId(logEntry.getResourceId())
                .resourceName(logEntry.getResourceName())
                .details(logEntry.getDetails())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
