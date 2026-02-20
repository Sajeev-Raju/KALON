package com.kalon.service;

import com.kalon.dto.ProductDTO;
import com.kalon.dto.ProductImageDTO;
import com.kalon.dto.ProductVariantDTO;
import com.kalon.entity.Product;
import com.kalon.entity.ProductImage;
import com.kalon.entity.ProductVariant;
import com.kalon.entity.User;
import com.kalon.entity.Wishlist;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.exception.WishlistException;
import com.kalon.repository.ProductRepository;
import com.kalon.repository.UserRepository;
import com.kalon.repository.WishlistRepository;
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
public class WishlistService {

    private static final int MAX_WISHLIST_SIZE = 100;

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public List<ProductDTO> getWishlistByUserId(Long userId) {
        return wishlistRepository.findByUserIdWithProducts(userId).stream()
                .filter(wishlist -> wishlist.getProduct().isActive())
                .map(wishlist -> toProductDTO(wishlist.getProduct()))
                .collect(Collectors.toList());
    }

    public Page<ProductDTO> getWishlistByUserId(Long userId, Pageable pageable) {
        return wishlistRepository.findActiveByUserId(userId, pageable)
                .map(wishlist -> toProductDTO(wishlist.getProduct()));
    }

    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }

    @Transactional
    public void addToWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new WishlistException("Product already in wishlist");
        }

        long currentSize = wishlistRepository.countByUserId(userId);
        if (currentSize >= MAX_WISHLIST_SIZE) {
            throw new WishlistException("Wishlist is full. Maximum " + MAX_WISHLIST_SIZE + " items allowed.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new WishlistException("Product is no longer available");
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        wishlistRepository.save(wishlist);
        log.info("Added to wishlist: userId={}, productId={}", userId, productId);
    }

    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        wishlistRepository.deleteByUserIdAndProductId(userId, productId);
        log.info("Removed from wishlist: userId={}, productId={}", userId, productId);
    }

    @Transactional
    public void toggleWishlist(Long userId, Long productId) {
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            removeFromWishlist(userId, productId);
        } else {
            addToWishlist(userId, productId);
        }
    }

    /**
     * Removes all wishlist entries for a product.
     * Called when a product is deactivated.
     */
    @Transactional
    public void removeProductFromAllWishlists(Long productId) {
        wishlistRepository.deleteByProductId(productId);
        log.info("Removed product {} from all wishlists", productId);
    }

    // ────────────────────────────── DTO mapping ──────────────────────────────

    private ProductDTO toProductDTO(Product product) {
        List<String> sizes = product.getVariants().stream()
                .map(ProductVariant::getSize)
                .distinct()
                .collect(Collectors.toList());

        List<String> colors = product.getVariants().stream()
                .map(ProductVariant::getColor)
                .distinct()
                .collect(Collectors.toList());

        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .discountPercentage(product.getDiscountPercentage())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .brandName(product.getBrandName())
                .material(product.getMaterial())
                .careInstructions(product.getCareInstructions())
                .countryOfOrigin(product.getCountryOfOrigin())
                .isFeatured(product.isFeatured())
                .isNewArrival(product.isNewArrival())
                .isBestSeller(product.isBestSeller())
                .averageRating(product.getAverageRating())
                .reviewCount(product.getReviewCount())
                .isReturnable(product.isReturnable())
                .images(product.getImages().stream().map(this::toImageDTO).collect(Collectors.toList()))
                .variants(product.getVariants().stream().map(this::toVariantDTO).collect(Collectors.toList()))
                .availableSizes(sizes)
                .availableColors(colors)
                .build();
    }

    private ProductImageDTO toImageDTO(ProductImage image) {
        return ProductImageDTO.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .isPrimary(image.isPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    private ProductVariantDTO toVariantDTO(ProductVariant variant) {
        return ProductVariantDTO.builder()
                .id(variant.getId())
                .size(variant.getSize())
                .color(variant.getColor())
                .colorCode(variant.getColorCode())
                .sku(variant.getSku())
                .stockQuantity(variant.getStockQuantity())
                .isAvailable(variant.isAvailable())
                .build();
    }
}
