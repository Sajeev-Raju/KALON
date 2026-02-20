package com.kalon.controller;

import com.kalon.dto.ApiResponse;
import com.kalon.dto.ProductDTO;
import com.kalon.entity.User;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.UserRepository;
import com.kalon.service.WishlistService;
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
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProductDTO>>> getWishlist(@AuthenticationPrincipal UserDetails userDetails) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(wishlistService.getWishlistByUserId(userId)));
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getWishlistPaginated(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.getWishlistByUserId(userId,
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))));
    }

    @GetMapping("/check/{productId}")
    public ResponseEntity<ApiResponse<Boolean>> isInWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Long userId = getUserId(userDetails);
        return ResponseEntity.ok(ApiResponse.success(wishlistService.isInWishlist(userId, productId)));
    }

    @PostMapping("/add/{productId}")
    public ResponseEntity<ApiResponse<Void>> addToWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Long userId = getUserId(userDetails);
        wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Added to wishlist", null));
    }

    @DeleteMapping("/remove/{productId}")
    public ResponseEntity<ApiResponse<Void>> removeFromWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Long userId = getUserId(userDetails);
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Removed from wishlist", null));
    }

    @PostMapping("/toggle/{productId}")
    public ResponseEntity<ApiResponse<Void>> toggleWishlist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long productId) {
        Long userId = getUserId(userDetails);
        wishlistService.toggleWishlist(userId, productId);
        return ResponseEntity.ok(ApiResponse.success("Wishlist updated", null));
    }

    private Long getUserId(UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return user.getId();
    }
}
