package com.kalon.service;

import com.kalon.exception.PaymentException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class RazorpayService {

    @Value("${razorpay.key.id}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret}")
    private String razorpayKeySecret;

    @Value("${razorpay.currency:INR}")
    private String currency;

    private RazorpayClient razorpayClient;

    @PostConstruct
    private void initClient() {
        try {
            this.razorpayClient = new RazorpayClient(razorpayKeyId, razorpayKeySecret);
            log.info("RazorpayClient initialized successfully");
        } catch (RazorpayException e) {
            log.error("Failed to initialize RazorpayClient", e);
            throw new PaymentException("Failed to initialize payment gateway", e);
        }
    }

    public OrderResponse createOrder(BigDecimal amount, String orderId, String customerName, String customerEmail, String customerPhone) {
        try {
            JSONObject orderRequest = new JSONObject();
            // Amount in paise (smallest currency unit for INR) — use longValue to avoid int overflow
            orderRequest.put("amount", amount.multiply(new BigDecimal("100")).longValue());
            orderRequest.put("currency", currency);
            orderRequest.put("receipt", orderId);

            JSONObject notes = new JSONObject();
            notes.put("orderId", orderId);
            notes.put("customerName", customerName);
            orderRequest.put("notes", notes);

            JSONObject customer = new JSONObject();
            customer.put("name", customerName);
            if (customerEmail != null && !customerEmail.isEmpty()) {
                customer.put("email", customerEmail);
            }
            if (customerPhone != null && !customerPhone.isEmpty()) {
                customer.put("contact", customerPhone);
            }
            orderRequest.put("customer", customer);

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

            Object amountObj = razorpayOrder.get("amount");
            Long amountInPaise = amountObj instanceof Integer ? ((Integer) amountObj).longValue() : (Long) amountObj;
            Long amountInRupees = amountInPaise / 100;

            log.info("Razorpay order created: razorpayOrderId={}, receipt={}, amount={}",
                    razorpayOrder.get("id"), orderId, amount);

            return OrderResponse.builder()
                    .razorpayOrderId((String) razorpayOrder.get("id"))
                    .amount(amountInRupees)
                    .currency((String) razorpayOrder.get("currency"))
                    .keyId(razorpayKeyId)
                    .build();
        } catch (RazorpayException e) {
            log.error("Error creating Razorpay order: receipt={}", orderId, e);
            throw new PaymentException("Failed to create Razorpay order: " + e.getMessage(), e);
        }
    }

    public PaymentDetails fetchPayment(String razorpayPaymentId) {
        try {
            com.razorpay.Payment payment = razorpayClient.payments.fetch(razorpayPaymentId);

            Object amountObj = payment.get("amount");
            Long amountPaise = amountObj instanceof Integer ? ((Integer) amountObj).longValue() : (Long) amountObj;

            return PaymentDetails.builder()
                    .paymentId((String) payment.get("id"))
                    .orderId((String) payment.get("order_id"))
                    .amount(amountPaise)
                    .currency((String) payment.get("currency"))
                    .status((String) payment.get("status"))
                    .method(payment.get("method") != null ? payment.get("method").toString() : null)
                    .build();
        } catch (RazorpayException e) {
            log.error("Error fetching Razorpay payment: paymentId={}", razorpayPaymentId, e);
            throw new PaymentException("Failed to fetch payment details: " + e.getMessage(), e);
        }
    }

    public boolean verifyWebhookSignature(String payload, String signature, String webhookSecret) {
        try {
            String generatedSignature = generateSignature(payload, webhookSecret);
            boolean valid = java.security.MessageDigest.isEqual(
                    generatedSignature.getBytes(), signature.getBytes());

            if (!valid) {
                log.warn("Webhook signature verification failed");
            }
            return valid;
        } catch (Exception e) {
            log.error("Error verifying webhook signature", e);
            return false;
        }
    }

    public boolean verifyPayment(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            String secret = razorpayKeySecret;

            // Verify signature using standard HMAC SHA256 verification
            String generatedSignature = generateSignature(razorpayOrderId + "|" + razorpayPaymentId, secret);

            // Use constant-time comparison to prevent timing attacks
            boolean valid = java.security.MessageDigest.isEqual(
                    generatedSignature.getBytes(), razorpaySignature.getBytes());

            if (valid) {
                log.info("Payment signature verified: razorpayOrderId={}, paymentId={}", razorpayOrderId, razorpayPaymentId);
            } else {
                log.warn("Payment signature verification failed: razorpayOrderId={}, paymentId={}", razorpayOrderId, razorpayPaymentId);
            }

            return valid;
        } catch (Exception e) {
            log.error("Error verifying Razorpay payment: razorpayOrderId={}", razorpayOrderId, e);
            return false;
        }
    }

    private String generateSignature(String data, String secret) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC signature", e);
            throw new PaymentException("Failed to generate payment signature", e);
        }
    }

    public RefundResponse createRefund(String paymentId, BigDecimal amount, String notes) {
        // Validate refund amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentException("Refund amount must be positive");
        }

        // Verify payment status before refunding
        try {
            PaymentDetails paymentDetails = fetchPayment(paymentId);
            if (!"captured".equals(paymentDetails.getStatus())) {
                throw new PaymentException("Payment is not in captured state. Current status: " + paymentDetails.getStatus());
            }
            // Verify refund amount doesn't exceed payment amount
            long refundAmountPaise = amount.multiply(new BigDecimal("100")).longValue();
            if (refundAmountPaise > paymentDetails.getAmount()) {
                throw new PaymentException("Refund amount (₹" + amount + ") exceeds payment amount");
            }
        } catch (PaymentException e) {
            // Re-throw PaymentExceptions for validation failures
            if (e.getMessage().startsWith("Refund amount") || e.getMessage().startsWith("Payment is not")) {
                throw e;
            }
            // If we can't verify, log warning and proceed (Razorpay API will reject if invalid)
            log.warn("Could not verify payment before refund: paymentId={}, error={}", paymentId, e.getMessage());
        }

        try {
            JSONObject refundRequest = new JSONObject();
            // Amount in paise — use longValue to avoid int overflow
            refundRequest.put("amount", amount.multiply(new BigDecimal("100")).longValue());
            if (notes != null && !notes.isEmpty()) {
                refundRequest.put("notes", new JSONObject().put("reason", notes));
            }

            com.razorpay.Refund refund = razorpayClient.payments.refund(paymentId, refundRequest);

            Object refundAmountObj = refund.get("amount");
            Long refundAmountPaise = refundAmountObj instanceof Integer
                    ? ((Integer) refundAmountObj).longValue()
                    : (Long) refundAmountObj;

            RefundResponse response = RefundResponse.builder()
                    .refundId((String) refund.get("id"))
                    .amount(refundAmountPaise)
                    .currency((String) refund.get("currency"))
                    .status((String) refund.get("status"))
                    .build();

            log.info("Razorpay refund created: refundId={}, paymentId={}, amount={}, status={}",
                    response.getRefundId(), paymentId, amount, response.getStatus());

            return response;
        } catch (RazorpayException e) {
            log.error("Error creating refund: paymentId={}", paymentId, e);
            throw new PaymentException("Failed to create refund: " + e.getMessage(), e);
        }
    }

    @lombok.Builder
    @lombok.Data
    public static class RefundResponse {
        private String refundId;
        private Long amount;
        private String currency;
        private String status;
    }

    @lombok.Builder
    @lombok.Data
    public static class OrderResponse {
        private String razorpayOrderId;
        private Long amount;
        private String currency;
        private String keyId;
    }

    @lombok.Builder
    @lombok.Data
    public static class PaymentDetails {
        private String paymentId;
        private String orderId;
        private Long amount;
        private String currency;
        private String status;
        private String method;
    }
}
