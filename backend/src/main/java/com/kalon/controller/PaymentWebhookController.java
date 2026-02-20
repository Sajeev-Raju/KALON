package com.kalon.controller;

import com.kalon.service.OrderService;
import com.kalon.service.RazorpayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentWebhookController {

    private final RazorpayService razorpayService;
    private final OrderService orderService;

    @Value("${razorpay.webhook.secret:}")
    private String webhookSecret;

    @PostMapping("/razorpay/webhook")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Received Razorpay webhook");

        // Verify webhook signature if secret is configured
        if (webhookSecret != null && !webhookSecret.isEmpty()) {
            if (signature == null || signature.isEmpty()) {
                log.warn("Webhook rejected: missing signature");
                return ResponseEntity.badRequest().body("Missing signature");
            }

            if (!razorpayService.verifyWebhookSignature(payload, signature, webhookSecret)) {
                log.warn("Webhook rejected: invalid signature");
                return ResponseEntity.badRequest().body("Invalid signature");
            }
        }

        try {
            JSONObject webhookData = new JSONObject(payload);
            String event = webhookData.getString("event");

            log.info("Webhook event: {}", event);

            switch (event) {
                case "payment.captured": {
                    JSONObject paymentEntity = webhookData
                            .getJSONObject("payload")
                            .getJSONObject("payment")
                            .getJSONObject("entity");

                    String paymentId = paymentEntity.getString("id");
                    String orderId = paymentEntity.getString("order_id");
                    Long amount = paymentEntity.getLong("amount");

                    orderService.handlePaymentCaptured(paymentId, orderId, amount);
                    break;
                }
                case "payment.failed": {
                    JSONObject paymentEntity = webhookData
                            .getJSONObject("payload")
                            .getJSONObject("payment")
                            .getJSONObject("entity");

                    String paymentId = paymentEntity.getString("id");
                    String orderId = paymentEntity.optString("order_id", null);

                    if (orderId != null) {
                        orderService.handlePaymentFailed(orderId, paymentId);
                    }
                    break;
                }
                default:
                    log.debug("Unhandled webhook event: {}", event);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            // Return 200 to prevent Razorpay from retrying for parsing errors
            return ResponseEntity.ok("Error processed");
        }
    }
}
