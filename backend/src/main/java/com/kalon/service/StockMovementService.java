package com.kalon.service;

import com.kalon.dto.StockMovementDTO;
import com.kalon.entity.Product;
import com.kalon.entity.ProductVariant;
import com.kalon.entity.StockMovement;
import com.kalon.entity.User;
import com.kalon.repository.StockMovementRepository;
import com.kalon.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;
    private final UserRepository userRepository;

    public void recordMovement(ProductVariant variant, Product product,
                               StockMovement.MovementType movementType,
                               int quantityChange, int quantityBefore, int quantityAfter,
                               StockMovement.ReferenceType referenceType, Long referenceId,
                               String notes) {
        User currentUser = getCurrentUser();

        StockMovement movement = StockMovement.builder()
                .productVariant(variant)
                .product(product)
                .movementType(movementType)
                .quantityChange(quantityChange)
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .notes(notes)
                .createdBy(currentUser)
                .build();

        stockMovementRepository.save(movement);
        log.debug("Stock movement recorded: variantId={}, type={}, change={}, before={}, after={}",
                variant.getId(), movementType, quantityChange, quantityBefore, quantityAfter);
    }

    public Page<StockMovementDTO> getMovementsByVariant(Long variantId, Pageable pageable) {
        return stockMovementRepository.findByProductVariantIdOrderByCreatedAtDesc(variantId, pageable)
                .map(this::toDTO);
    }

    public Page<StockMovementDTO> getMovementsByProduct(Long productId, Pageable pageable) {
        return stockMovementRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toDTO);
    }

    public Page<StockMovementDTO> getMovementsWithFilters(Long variantId, Long productId,
                                                           StockMovement.MovementType movementType,
                                                           LocalDateTime dateFrom, LocalDateTime dateTo,
                                                           Pageable pageable) {
        return stockMovementRepository.findWithFilters(variantId, productId, movementType, dateFrom, dateTo, pageable)
                .map(this::toDTO);
    }

    private StockMovementDTO toDTO(StockMovement sm) {
        return StockMovementDTO.builder()
                .id(sm.getId())
                .variantId(sm.getProductVariant().getId())
                .productId(sm.getProduct().getId())
                .productName(sm.getProduct().getName())
                .variantSize(sm.getProductVariant().getSize())
                .variantColor(sm.getProductVariant().getColor())
                .movementType(sm.getMovementType().name())
                .quantityChange(sm.getQuantityChange())
                .quantityBefore(sm.getQuantityBefore())
                .quantityAfter(sm.getQuantityAfter())
                .referenceType(sm.getReferenceType() != null ? sm.getReferenceType().name() : null)
                .referenceId(sm.getReferenceId())
                .notes(sm.getNotes())
                .createdByEmail(sm.getCreatedBy() != null ? sm.getCreatedBy().getEmail() : "SYSTEM")
                .createdAt(sm.getCreatedAt())
                .build();
    }

    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
                String email = auth.getName();
                return userRepository.findByEmail(email).orElse(null);
            }
        } catch (Exception e) {
            log.debug("Could not resolve current user for stock movement: {}", e.getMessage());
        }
        return null;
    }
}
