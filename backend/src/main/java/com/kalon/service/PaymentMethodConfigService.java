package com.kalon.service;

import com.kalon.dto.PaymentMethodConfigDTO;
import com.kalon.entity.Order;
import com.kalon.entity.PaymentMethodConfig;
import com.kalon.exception.OrderStateException;
import com.kalon.exception.ResourceNotFoundException;
import com.kalon.repository.PaymentMethodConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodConfigService {

    private final PaymentMethodConfigRepository configRepository;

    // In-memory cache for fast lookups during order creation
    private final Map<Order.PaymentMethod, PaymentMethodConfig> configCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
        seedDefaults();
    }

    private void seedDefaults() {
        if (configRepository.count() == 0) {
            log.info("Seeding default payment method configurations");
            createDefault(Order.PaymentMethod.RAZORPAY, "Pay Online",
                    "UPI, Cards, Net Banking, Wallets", true, 1, null, null);
            createDefault(Order.PaymentMethod.COD, "Cash on Delivery",
                    "Pay when you receive your order", true, 2, null, new BigDecimal("10000"));
            refreshCache();
        }
    }

    private void createDefault(Order.PaymentMethod method, String displayName, String description,
                                boolean enabled, int order, BigDecimal min, BigDecimal max) {
        PaymentMethodConfig config = PaymentMethodConfig.builder()
                .paymentMethod(method)
                .displayName(displayName)
                .description(description)
                .isEnabled(enabled)
                .displayOrder(order)
                .minAmount(min)
                .maxAmount(max)
                .build();
        configRepository.save(config);
    }

    public void refreshCache() {
        configCache.clear();
        configRepository.findAll().forEach(c -> configCache.put(c.getPaymentMethod(), c));
        log.debug("Payment method config cache refreshed: {} entries", configCache.size());
    }

    // ────────────────────────────── Validation ──────────────────────────────

    public boolean isMethodEnabled(Order.PaymentMethod method) {
        PaymentMethodConfig config = configCache.get(method);
        return config != null && config.isEnabled();
    }

    public void validateMethodForAmount(Order.PaymentMethod method, BigDecimal amount) {
        PaymentMethodConfig config = configCache.get(method);
        if (config == null || !config.isEnabled()) {
            throw new OrderStateException("Payment method '" + method + "' is not currently available");
        }

        if (config.getMinAmount() != null && amount.compareTo(config.getMinAmount()) < 0) {
            throw new OrderStateException(config.getDisplayName() + " requires a minimum order of ₹" + config.getMinAmount());
        }

        if (config.getMaxAmount() != null && amount.compareTo(config.getMaxAmount()) > 0) {
            throw new OrderStateException(config.getDisplayName() + " is not available for orders above ₹" + config.getMaxAmount());
        }
    }

    // ────────────────────────────── Read operations ──────────────────────────────

    public List<PaymentMethodConfigDTO> getEnabledMethods() {
        return configRepository.findByIsEnabledTrueOrderByDisplayOrderAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<PaymentMethodConfigDTO> getAllMethods() {
        return configRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public PaymentMethodConfigDTO getById(Long id) {
        return toDTO(configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method config not found")));
    }

    // ────────────────────────────── Admin operations ──────────────────────────────

    @Transactional
    public PaymentMethodConfigDTO createOrUpdate(PaymentMethodConfigDTO dto) {
        Order.PaymentMethod method;
        try {
            method = Order.PaymentMethod.valueOf(dto.getPaymentMethod());
        } catch (IllegalArgumentException e) {
            throw new OrderStateException("Invalid payment method: " + dto.getPaymentMethod());
        }

        PaymentMethodConfig config = configRepository.findByPaymentMethod(method)
                .orElse(PaymentMethodConfig.builder().paymentMethod(method).build());

        config.setDisplayName(dto.getDisplayName());
        config.setDescription(dto.getDescription());
        config.setEnabled(dto.getIsEnabled());
        config.setDisplayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0);
        config.setMinAmount(dto.getMinAmount());
        config.setMaxAmount(dto.getMaxAmount());

        config = configRepository.save(config);
        refreshCache();

        log.info("Payment method config updated: method={}, enabled={}", method, config.isEnabled());
        return toDTO(config);
    }

    @Transactional
    public PaymentMethodConfigDTO toggleMethod(Long id, boolean enabled) {
        PaymentMethodConfig config = configRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment method config not found"));

        config.setEnabled(enabled);
        config = configRepository.save(config);
        refreshCache();

        log.info("Payment method toggled: method={}, enabled={}", config.getPaymentMethod(), enabled);
        return toDTO(config);
    }

    // ────────────────────────────── DTO mapping ──────────────────────────────

    private PaymentMethodConfigDTO toDTO(PaymentMethodConfig config) {
        return PaymentMethodConfigDTO.builder()
                .id(config.getId())
                .paymentMethod(config.getPaymentMethod().name())
                .displayName(config.getDisplayName())
                .description(config.getDescription())
                .isEnabled(config.isEnabled())
                .displayOrder(config.getDisplayOrder())
                .minAmount(config.getMinAmount())
                .maxAmount(config.getMaxAmount())
                .build();
    }
}
