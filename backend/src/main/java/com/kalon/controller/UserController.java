package com.kalon.controller;

import com.kalon.dto.ApiResponse;
import com.kalon.dto.ChangePasswordRequest;
import com.kalon.dto.NotificationPreferenceDTO;
import com.kalon.dto.UserDTO;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.UserRepository;
import com.kalon.service.NotificationPreferenceService;
import com.kalon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(userId)));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserDTO dto) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(userId, dto)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = getUserId(userDetails);
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/notification-preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> getNotificationPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        NotificationPreferenceDTO prefs = notificationPreferenceService.getPreferences(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Notification preferences", prefs));
    }

    @PutMapping("/notification-preferences")
    public ResponseEntity<ApiResponse<NotificationPreferenceDTO>> updateNotificationPreferences(
            @RequestBody NotificationPreferenceDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        NotificationPreferenceDTO prefs = notificationPreferenceService.updatePreferences(user.getId(), dto);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", prefs));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
