package com.kalon.controller;

import com.kalon.dto.ApiResponse;
import com.kalon.dto.CreateReviewRequest;
import com.kalon.dto.ReviewDTO;
import com.kalon.dto.UpdateReviewRequest;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.UserRepository;
import com.kalon.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;
    private final UserRepository userRepository;

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Page<ReviewDTO>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        Sort sortOrder = switch (sort) {
            case "oldest" -> Sort.by("createdAt").ascending();
            case "highest" -> Sort.by("rating").descending().and(Sort.by("createdAt").descending());
            case "lowest" -> Sort.by("rating").ascending().and(Sort.by("createdAt").descending());
            default -> Sort.by("createdAt").descending();
        };
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.getProductReviews(productId, PageRequest.of(page, size, sortOrder))));
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<ReviewDTO>>> getUserReviews(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(reviewService.getUserReviews(userId)));
    }

    @GetMapping("/user/product/{productId}")
    public ResponseEntity<ApiResponse<ReviewDTO>> getUserReviewForProduct(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Long userId = getUserId(userDetails);
        ReviewDTO review = reviewService.getUserReviewForProduct(userId, productId);
        return ResponseEntity.ok(ApiResponse.success(review));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReviewDTO>> createReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateReviewRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Review submitted successfully",
                reviewService.createReview(userId, request)));
    }

    @PutMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<ReviewDTO>> updateReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId,
            @Valid @RequestBody UpdateReviewRequest request) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success("Review updated successfully",
                reviewService.updateReview(reviewId, userId, request)));
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long reviewId) {
        Long userId = getUserId(userDetails);
        reviewService.deleteReview(reviewId, userId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
