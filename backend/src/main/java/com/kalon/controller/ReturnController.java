package com.kalon.controller;

import com.kalon.dto.*;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.UserRepository;
import com.kalon.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {

    private final ReturnService returnService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> createReturnRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReturnRequestDTO request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Return request created", returnService.createReturnRequest(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnRequestDTO>>> getReturnRequests(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(returnService.getReturnRequestsByUserId(userId)));
    }

    @GetMapping("/{returnRequestId}")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> getReturnRequestById(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long returnRequestId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(returnService.getReturnRequestById(returnRequestId, userId)));
    }

    @PostMapping("/{returnRequestId}/cancel")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> cancelReturnRequest(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long returnRequestId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Return request cancelled",
                returnService.cancelReturnRequest(returnRequestId, userId)));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
