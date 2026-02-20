package com.kalon.service;

import com.kalon.dto.UserDTO;
import com.kalon.entity.User;
import com.kalon.exception.InvalidCredentialsException;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.OrderRepository;
import com.kalon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toDTO);
    }

    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toDTO(user);
    }

    @Transactional
    public UserDTO updateUser(Long id, UserDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) {
            validatePhoneNumber(dto.getPhoneNumber());
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getRole() != null) {
            user.setRole(User.Role.valueOf(dto.getRole()));
        }
        if (dto.getPassword() != null && !dto.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }

        user = userRepository.save(user);
        log.info("Admin updated user: userId={}", id);
        return toDTO(user);
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
        log.info("Toggled user status: userId={}, isActive={}", id, user.isActive());
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setActive(false);
        userRepository.save(user);
        log.info("Deactivated user: userId={}", id);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getPassword() == null) {
            throw new InvalidCredentialsException(
                    "This account uses Google sign-in and has no password. Please use Google to login.");
        }

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            log.warn("Failed password change attempt: userId={}", userId);
            throw new InvalidCredentialsException("Current password is incorrect");
        }

        validatePasswordStrength(newPassword);

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed: userId={}", userId);
    }

    @Transactional
    public UserDTO updateProfile(Long userId, UserDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Email is the primary identifier and cannot be changed via profile update
        if (dto.getEmail() != null && !dto.getEmail().equals(user.getEmail())) {
            log.warn("Rejected email change attempt: userId={}, attempted={}", userId, dto.getEmail());
            throw new IllegalArgumentException("Email cannot be changed");
        }

        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName());
        if (dto.getPhoneNumber() != null) {
            validatePhoneNumber(dto.getPhoneNumber());
            user.setPhoneNumber(dto.getPhoneNumber());
        }

        user = userRepository.save(user);
        log.info("Profile updated: userId={}", userId);
        return toDTO(user);
    }

    private void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber.isEmpty()) return; // allow clearing
        // Indian phone: 10 digits, optionally prefixed with +91
        if (!phoneNumber.matches("^(\\+91)?[6-9]\\d{9}$")) {
            throw new IllegalArgumentException("Invalid phone number format. Expected 10-digit Indian mobile number.");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters long");
        }
        if (!password.matches(".*[A-Z].*")) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!password.matches(".*[a-z].*")) {
            throw new IllegalArgumentException("Password must contain at least one lowercase letter");
        }
        if (!password.matches(".*\\d.*")) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
    }

    private UserDTO toDTO(User user) {
        // Use aggregation queries instead of loading all orders (N+1 fix)
        long orderCount = orderRepository.countByUserId(user.getId());
        BigDecimal totalSpent = orderRepository.sumTotalAmountByUserIdAndStatus(
                user.getId(), com.kalon.entity.Order.OrderStatus.DELIVERED);

        return UserDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole().name())
                .isActive(user.isActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .orderCount((int) orderCount)
                .totalSpent(totalSpent)
                .build();
    }
}
