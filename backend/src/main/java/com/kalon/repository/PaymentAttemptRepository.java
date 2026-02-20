package com.kalon.repository;

import com.kalon.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {
    List<PaymentAttempt> findByOrderIdOrderByAttemptNumberDesc(Long orderId);

    long countByOrderId(Long orderId);

    Optional<PaymentAttempt> findByRazorpayOrderId(String razorpayOrderId);

    Optional<PaymentAttempt> findByRazorpayPaymentId(String razorpayPaymentId);

    @Query("SELECT pa FROM PaymentAttempt pa WHERE pa.order.id = :orderId AND pa.status = :status ORDER BY pa.attemptNumber DESC")
    List<PaymentAttempt> findByOrderIdAndStatus(@Param("orderId") Long orderId, @Param("status") PaymentAttempt.AttemptStatus status);

    @Query("SELECT MAX(pa.attemptNumber) FROM PaymentAttempt pa WHERE pa.order.id = :orderId")
    Optional<Integer> findMaxAttemptNumberByOrderId(@Param("orderId") Long orderId);
}
