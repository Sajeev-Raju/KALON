package com.kalon.service;

import com.kalon.dto.LowStockAlertDTO;
import com.kalon.entity.Order;
import com.kalon.entity.ReturnRequest;
import com.kalon.entity.User;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.annotation.PostConstruct;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;
    private final String fromName;
    private final String frontendUrl;
    private final String mailUsername;
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender,
                        TemplateEngine templateEngine,
                        @Value("${mail.from-address}") String fromAddress,
                        @Value("${mail.from-name}") String fromName,
                        @Value("${app.frontend-url}") String frontendUrl,
                        @Value("${spring.mail.username:}") String mailUsername,
                        @Value("${mail.enabled:true}") boolean emailEnabled) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.frontendUrl = frontendUrl;
        this.mailUsername = mailUsername;
        this.emailEnabled = emailEnabled;
    }

    @PostConstruct
    public void validateConfig() {
        if (!emailEnabled) {
            log.warn("═══════════════════════════════════════════════════════════");
            log.warn("  EMAIL DISABLED: mail.enabled=false");
            log.warn("  Set EMAIL_ENABLED=true to enable email notifications.");
            log.warn("═══════════════════════════════════════════════════════════");
        } else if (!StringUtils.hasText(mailUsername)) {
            emailEnabled = false;
            log.warn("═══════════════════════════════════════════════════════════");
            log.warn("  EMAIL DISABLED: MAIL_USERNAME is not configured.");
            log.warn("  Set MAIL_USERNAME and MAIL_PASSWORD to enable emails.");
            log.warn("═══════════════════════════════════════════════════════════");
        } else {
            log.info("Email service initialized: from={}, frontendUrl={}", fromAddress, frontendUrl);
        }
    }

    // ─── 1. Welcome Email ───

    @Async
    public void sendWelcomeEmail(User user) {
        try {
            Context context = new Context();
            context.setVariable("firstName", safeFirstName(user));
            context.setVariable("shopUrl", frontendUrl);

            String html = templateEngine.process("welcome-email", context);
            sendHtmlEmail(user.getEmail(), "Welcome to KALON!", html);
            log.info("Welcome email sent: userId={}, email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email: userId={}, email={}, error={}",
                    user.getId(), user.getEmail(), e.getMessage(), e);
        }
    }

    // ─── 2. Order Confirmation Email ───

    @Async
    public void sendOrderConfirmationEmail(User user, Order order) {
        try {
            Context context = buildOrderContext(user, order);

            String html = templateEngine.process("order-confirmation-email", context);
            sendHtmlEmail(user.getEmail(),
                    "Order Confirmed - " + order.getOrderNumber(), html);
            log.info("Order confirmation email sent: orderNumber={}, email={}",
                    order.getOrderNumber(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email: orderNumber={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }

    // ─── 3. Shipping Notification Email ───

    @Async
    public void sendShippingNotificationEmail(User user, Order order) {
        try {
            Context context = buildOrderContext(user, order);
            context.setVariable("trackingNumber", order.getTrackingNumber());

            String html = templateEngine.process("shipping-notification-email", context);
            sendHtmlEmail(user.getEmail(),
                    "Your Order Has Shipped - " + order.getOrderNumber(), html);
            log.info("Shipping notification email sent: orderNumber={}, email={}",
                    order.getOrderNumber(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send shipping notification email: orderNumber={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }

    // ─── 4. Order Cancellation Email ───

    @Async
    public void sendOrderCancellationEmail(User user, Order order) {
        try {
            Context context = buildOrderContext(user, order);

            String html = templateEngine.process("order-cancellation-email", context);
            sendHtmlEmail(user.getEmail(),
                    "Order Cancelled - " + order.getOrderNumber(), html);
            log.info("Order cancellation email sent: orderNumber={}, email={}",
                    order.getOrderNumber(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order cancellation email: orderNumber={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }

    // ─── 5. Order Delivered Email ───

    @Async
    public void sendOrderDeliveredEmail(User user, Order order) {
        try {
            Context context = buildOrderContext(user, order);

            String html = templateEngine.process("order-delivered-email", context);
            sendHtmlEmail(user.getEmail(),
                    "Order Delivered - " + order.getOrderNumber(), html);
            log.info("Order delivered email sent: orderNumber={}, email={}",
                    order.getOrderNumber(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send order delivered email: orderNumber={}, error={}",
                    order.getOrderNumber(), e.getMessage(), e);
        }
    }

    // ─── 6. Return Request Status Email ───

    @Async
    public void sendReturnStatusEmail(User user, Order order,
                                       ReturnRequest returnRequest, String statusAction) {
        try {
            Context context = new Context();
            context.setVariable("firstName", safeFirstName(user));
            context.setVariable("orderNumber", order.getOrderNumber());
            context.setVariable("returnId", returnRequest.getId());
            context.setVariable("status", returnRequest.getStatus().name());
            context.setVariable("statusAction", statusAction);
            context.setVariable("reason", returnRequest.getReason());
            context.setVariable("refundAmount", returnRequest.getRefundAmount());
            context.setVariable("adminNotes", returnRequest.getAdminNotes());
            context.setVariable("requestType", returnRequest.getRequestType().name());
            context.setVariable("productName", returnRequest.getOrderItem().getProductName());
            context.setVariable("returnsUrl", frontendUrl + "/my-returns");
            context.setVariable("shopUrl", frontendUrl);

            String html = templateEngine.process("return-status-email", context);
            String subject = "Return " + statusAction + " - " + order.getOrderNumber();
            sendHtmlEmail(user.getEmail(), subject, html);
            log.info("Return status email sent: returnRequestId={}, status={}, email={}",
                    returnRequest.getId(), statusAction, user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send return status email: returnRequestId={}, error={}",
                    returnRequest.getId(), e.getMessage(), e);
        }
    }

    // ─── 7. Password Reset Email ───

    @Async
    public void sendPasswordResetEmail(User user, String token) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + token;

            Context context = new Context();
            context.setVariable("firstName", safeFirstName(user));
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expiryMinutes", 60);
            context.setVariable("shopUrl", frontendUrl);

            String html = templateEngine.process("password-reset-email", context);
            sendHtmlEmail(user.getEmail(), "Reset Your KALON Password", html);
            log.info("Password reset email sent: userId={}, email={}", user.getId(), user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send password reset email: userId={}, email={}, error={}",
                    user.getId(), user.getEmail(), e.getMessage(), e);
        }
    }

    // ─── 8. Low Stock Alert Email (synchronous — called from scheduler) ───

    public void sendLowStockAlertEmail(String adminEmail, List<LowStockAlertDTO> lowStockItems, int threshold) {
        try {
            Context context = new Context();
            context.setVariable("lowStockItems", lowStockItems);
            context.setVariable("alertCount", lowStockItems.size());
            context.setVariable("threshold", threshold);

            String html = templateEngine.process("low-stock-alert-email", context);
            sendHtmlEmail(adminEmail, "Low Stock Alert — " + lowStockItems.size() + " variant(s) below threshold", html);
            log.info("Low stock alert email sent: to={}, itemCount={}", adminEmail, lowStockItems.size());
        } catch (Exception e) {
            log.error("Failed to send low stock alert email: to={}, error={}", adminEmail, e.getMessage(), e);
        }
    }

    // ─── 9. Abandoned Cart Email ───

    @Async
    public void sendAbandonedCartEmail(User user, int itemCount) {
        if (!emailEnabled) return;
        try {
            Context context = new Context();
            context.setVariable("userName", user.getFirstName());
            context.setVariable("itemCount", itemCount);
            context.setVariable("cartUrl", frontendUrl + "/cart");

            String html = templateEngine.process("abandoned-cart-email", context);
            sendHtmlEmail(user.getEmail(), "You left items in your cart!", html);
            log.info("Abandoned cart email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send abandoned cart email to {}: {}", user.getEmail(), e.getMessage());
        }
    }

    // ─── Private helpers ───

    private Context buildOrderContext(User user, Order order) {
        Context context = new Context();
        context.setVariable("firstName", safeFirstName(user));
        context.setVariable("orderNumber", order.getOrderNumber());
        context.setVariable("orderDate", order.getCreatedAt()
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")));
        context.setVariable("items", order.getItems());
        context.setVariable("subtotal", order.getSubtotal());
        context.setVariable("shippingCost", order.getShippingCost());
        context.setVariable("totalAmount", order.getTotalAmount());
        context.setVariable("paymentMethod", order.getPaymentMethod() != null
                ? order.getPaymentMethod().name() : "N/A");
        context.setVariable("paymentStatus", order.getPaymentStatus() != null
                ? order.getPaymentStatus().name() : "PENDING");
        context.setVariable("shippingAddress", order.getShippingAddress());
        context.setVariable("ordersUrl", frontendUrl + "/orders");
        context.setVariable("shopUrl", frontendUrl);
        return context;
    }

    private String safeFirstName(User user) {
        return StringUtils.hasText(user.getFirstName()) ? user.getFirstName() : "there";
    }

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 2000;

    private void sendHtmlEmail(String to, String subject, String htmlContent)
            throws MessagingException, UnsupportedEncodingException {
        if (!emailEnabled) {
            log.debug("Email sending disabled — skipping: subject='{}', to='{}'", subject, to);
            return;
        }
        if (!StringUtils.hasText(to) || !to.contains("@")) {
            log.warn("Skipping email send — invalid recipient address: {}", to);
            return;
        }

        MessagingException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
                helper.setFrom(fromAddress, fromName);
                helper.setTo(to);
                helper.setSubject(subject);
                helper.setText(htmlContent, true);
                mailSender.send(message);
                return; // Success
            } catch (MessagingException e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    log.warn("Email send attempt {}/{} failed for '{}', retrying in {}ms: {}",
                            attempt, MAX_RETRIES, to, RETRY_DELAY_MS, e.getMessage());
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential-ish backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                }
            }
        }
        log.error("Failed to send email after {} attempts: subject='{}', to='{}'",
                MAX_RETRIES, subject, to);
        throw lastException;
    }
}
