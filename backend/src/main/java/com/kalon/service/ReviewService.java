package com.kalon.service;

import com.kalon.dto.CreateReviewRequest;
import com.kalon.dto.ReviewDTO;
import com.kalon.dto.UpdateReviewRequest;
import com.kalon.entity.Product;
import com.kalon.entity.Review;
import com.kalon.entity.User;
import com.kalon.exception.OrderAccessException;
import com.kalon.exception.OrderStateException;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.OrderRepository;
import com.kalon.repository.ProductRepository;
import com.kalon.repository.ReviewRepository;
import com.kalon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public Page<ReviewDTO> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndIsApprovedTrue(productId, pageable)
                .map(this::toDTO);
    }

    public List<ReviewDTO> getUserReviews(Long userId) {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReviewDTO createReview(Long userId, CreateReviewRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new OrderStateException("This product is no longer available for reviews.");
        }

        // Duplicate prevention
        if (reviewRepository.existsByUserIdAndProductId(userId, request.getProductId())) {
            throw new OrderStateException("You have already reviewed this product. You can edit your existing review instead.");
        }

        // Check if user has purchased this product (DELIVERED or RETURNED)
        boolean hasOrdered = orderRepository.existsByUserIdAndProductId(userId, request.getProductId());

        // Require purchase for reviewing
        if (!hasOrdered) {
            throw new OrderStateException("You can only review products you have purchased and received.");
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .isVerifiedPurchase(true) // Always true since we now require purchase
                .isApproved(true)
                .build();

        review = reviewRepository.save(review);
        updateProductRating(product);

        log.info("Review created: userId={}, productId={}, rating={}", userId, product.getId(), request.getRating());
        return toDTO(review);
    }

    @Transactional
    public ReviewDTO updateReview(Long reviewId, Long userId, UpdateReviewRequest request) {
        Review review = reviewRepository.findByIdWithDetails(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new OrderAccessException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        review = reviewRepository.save(review);

        updateProductRating(review.getProduct());

        log.info("Review updated: reviewId={}, userId={}, newRating={}", reviewId, userId, request.getRating());
        return toDTO(review);
    }

    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        Review review = reviewRepository.findByIdWithDetails(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        if (!review.getUser().getId().equals(userId)) {
            throw new OrderAccessException("You can only delete your own reviews");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        updateProductRating(product);

        log.info("Review deleted: reviewId={}, userId={}", reviewId, userId);
    }

    /**
     * Check if the current user has already reviewed a product.
     */
    public ReviewDTO getUserReviewForProduct(Long userId, Long productId) {
        return reviewRepository.findByUserIdAndProductId(userId, productId)
                .map(this::toDTO)
                .orElse(null);
    }

    // ────────────────────────────── Admin methods ──────────────────────────────

    public Page<ReviewDTO> getAllReviews(Pageable pageable) {
        return reviewRepository.findAllWithDetails(pageable).map(this::toDTO);
    }

    public Page<ReviewDTO> getReviewsFiltered(Long productId, Long userId, Boolean isApproved,
                                               Integer minRating, Integer maxRating, Pageable pageable) {
        return reviewRepository.findWithFilters(productId, userId, isApproved, minRating, maxRating, pageable)
                .map(this::toDTO);
    }

    @Transactional
    public ReviewDTO approveReview(Long reviewId) {
        Review review = reviewRepository.findByIdWithDetails(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        boolean wasApproved = review.isApproved();
        review.setApproved(true);
        review = reviewRepository.save(review);

        if (!wasApproved) {
            updateProductRating(review.getProduct());
        }

        log.info("Review approved: reviewId={}, statusChanged={}", reviewId, !wasApproved);
        return toDTO(review);
    }

    @Transactional
    public ReviewDTO rejectReview(Long reviewId) {
        Review review = reviewRepository.findByIdWithDetails(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        boolean wasApproved = review.isApproved();
        review.setApproved(false);
        review = reviewRepository.save(review);

        if (wasApproved) {
            updateProductRating(review.getProduct());
        }

        log.info("Review rejected: reviewId={}, statusChanged={}", reviewId, wasApproved);
        return toDTO(review);
    }

    @Transactional
    public void adminDeleteReview(Long reviewId) {
        Review review = reviewRepository.findByIdWithDetails(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        Product product = review.getProduct();
        reviewRepository.delete(review);
        updateProductRating(product);

        log.info("Review admin-deleted: reviewId={}", reviewId);
    }

    // ────────────────────────────── Helpers ──────────────────────────────

    private void updateProductRating(Product product) {
        Double avgRating = reviewRepository.getAverageRatingByProductId(product.getId());
        Integer reviewCount = reviewRepository.getReviewCountByProductId(product.getId());

        product.setAverageRating(avgRating != null ? avgRating : 0.0);
        product.setReviewCount(reviewCount != null ? reviewCount : 0);
        productRepository.save(product);
    }

    private ReviewDTO toDTO(Review review) {
        return ReviewDTO.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .productSlug(review.getProduct().getSlug())
                .userId(review.getUser().getId())
                .userName(review.getUser().getFirstName() + " " + review.getUser().getLastName())
                .rating(review.getRating())
                .comment(review.getComment())
                .isVerifiedPurchase(review.isVerifiedPurchase())
                .isApproved(review.isApproved())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
