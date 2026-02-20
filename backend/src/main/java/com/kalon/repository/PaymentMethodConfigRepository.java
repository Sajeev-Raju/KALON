package com.kalon.repository;

import com.kalon.entity.Order;
import com.kalon.entity.PaymentMethodConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentMethodConfigRepository extends JpaRepository<PaymentMethodConfig, Long> {
    Optional<PaymentMethodConfig> findByPaymentMethod(Order.PaymentMethod paymentMethod);

    List<PaymentMethodConfig> findByIsEnabledTrueOrderByDisplayOrderAsc();

    List<PaymentMethodConfig> findAllByOrderByDisplayOrderAsc();
}
