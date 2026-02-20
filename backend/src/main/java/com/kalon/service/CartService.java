package com.kalon.service;

import com.kalon.dto.AddToCartRequest;
import com.kalon.dto.CartDTO;
import com.kalon.dto.CartItemDTO;
import com.kalon.entity.*;
import com.kalon.exception.CartAccessException;
import com.kalon.exception.InsufficientStockException;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;

    public CartDTO getCartByUserId(Long userId) {
        Cart cart = getCartWithItems(userId);
        return toDTO(cart);
    }

    @Transactional
    public CartDTO addToCart(Long userId, AddToCartRequest request) {
        Cart cart = getOrCreateCart(userId);

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product is not available");
        }

        ProductVariant variant = productVariantRepository.findById(request.getVariantId())
                .orElseThrow(() -> new ResourceNotFoundException("Variant not found"));

        if (!variant.getProduct().getId().equals(product.getId())) {
            throw new ResourceNotFoundException("Variant does not belong to this product");
        }

        if (!variant.isAvailable() || variant.getStockQuantity() <= 0) {
            throw new InsufficientStockException(variant.getStockQuantity(), 0, 0, request.getQuantity());
        }

        // Check if item already exists in cart BEFORE stock validation
        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductIdAndVariantId(cart.getId(), request.getProductId(), request.getVariantId())
                .orElse(null);

        int existingQuantity = existingItem != null ? existingItem.getQuantity() : 0;
        int totalRequested = existingQuantity + request.getQuantity();

        // Validate combined quantity against stock
        if (variant.getStockQuantity() < totalRequested) {
            int canAdd = Math.max(0, variant.getStockQuantity() - existingQuantity);
            throw new InsufficientStockException(
                    variant.getStockQuantity(),
                    existingQuantity,
                    canAdd,
                    request.getQuantity()
            );
        }

        if (existingItem != null) {
            existingItem.setQuantity(totalRequested);
            cartItemRepository.save(existingItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .build();
            cartItemRepository.save(newItem);
        }

        return toDTO(cartRepository.findByIdWithItems(cart.getId()).orElse(cart));
    }

    @Transactional
    public CartDTO updateCartItemQuantity(Long userId, Long itemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new CartAccessException("Item does not belong to user's cart");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            if (item.getVariant().getStockQuantity() < quantity) {
                throw new InsufficientStockException(
                        item.getVariant().getStockQuantity(),
                        item.getQuantity(),
                        item.getVariant().getStockQuantity(),
                        quantity
                );
            }
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return toDTO(cartRepository.findByIdWithItems(cart.getId()).orElse(cart));
    }

    @Transactional
    public CartDTO removeFromCart(Long userId, Long itemId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new CartAccessException("Item does not belong to user's cart");
        }

        cartItemRepository.delete(item);
        return toDTO(cartRepository.findByIdWithItems(cart.getId()).orElse(cart));
    }

    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteByCartId(cart.getId());
    }

    @Transactional
    public CartDTO validateAndGetCart(Long userId) {
        Cart cart = getCartWithItems(userId);
        boolean modified = false;

        Iterator<CartItem> iterator = cart.getItems().iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            ProductVariant variant = productVariantRepository.findById(item.getVariant().getId())
                    .orElse(null);

            if (variant == null || !variant.isAvailable() || variant.getStockQuantity() <= 0
                    || !item.getProduct().isActive()) {
                cartItemRepository.delete(item);
                iterator.remove();
                modified = true;
            } else if (item.getQuantity() > variant.getStockQuantity()) {
                item.setQuantity(variant.getStockQuantity());
                cartItemRepository.save(item);
                modified = true;
            }
        }

        if (modified) {
            cart = cartRepository.findByIdWithItems(cart.getId()).orElse(cart);
        }

        return toDTO(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    private Cart getCartWithItems(Long userId) {
        return cartRepository.findByUserIdWithItems(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }

    private CartDTO toDTO(Cart cart) {
        List<CartItemDTO> items = cart.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        BigDecimal subtotal = items.stream()
                .map(CartItemDTO::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shippingCost = subtotal.compareTo(new BigDecimal("999")) >= 0 ?
                BigDecimal.ZERO : new BigDecimal("99");

        boolean hasStockIssues = items.stream().anyMatch(i ->
                !Boolean.TRUE.equals(i.getIsAvailable()) || i.getQuantity() > i.getStockQuantity());

        return CartDTO.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(items)
                .subtotal(subtotal)
                .shippingCost(shippingCost)
                .total(subtotal.add(shippingCost))
                .itemCount(items.stream().mapToInt(CartItemDTO::getQuantity).sum())
                .hasStockIssues(hasStockIssues)
                .build();
    }

    private CartItemDTO toItemDTO(CartItem item) {
        String primaryImage = item.getProduct().getImages().stream()
                .filter(ProductImage::isPrimary)
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(item.getProduct().getImages().isEmpty() ? null :
                        item.getProduct().getImages().get(0).getImageUrl());

        return CartItemDTO.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productName(item.getProduct().getName())
                .productImage(primaryImage)
                .variantId(item.getVariant().getId())
                .size(item.getVariant().getSize())
                .color(item.getVariant().getColor())
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .totalPrice(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .stockQuantity(item.getVariant().getStockQuantity())
                .isAvailable(item.getVariant().isAvailable() && item.getVariant().getStockQuantity() > 0)
                .build();
    }
}
