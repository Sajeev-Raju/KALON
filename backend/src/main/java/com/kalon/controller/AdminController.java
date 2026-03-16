package com.kalon.controller;

import com.kalon.dto.*;
import com.kalon.service.AdminService;
import com.kalon.service.CategoryService;
import com.kalon.service.CouponService;
import com.kalon.service.OrderService;
import com.kalon.service.ProductService;
import com.kalon.service.SiteConfigService;
import com.kalon.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final OrderService orderService;
    private final UserService userService;
    private final AdminService adminService;
    private final SiteConfigService siteConfigService;
    private final com.kalon.service.FileStorageService fileStorageService;
    private final com.kalon.service.ReturnService returnService;
    private final com.kalon.service.ReviewService reviewService;
    private final com.kalon.service.AdminActivityLogService activityLogService;
    private final com.kalon.service.OrderExportService orderExportService;
    private final com.kalon.service.PaymentMethodConfigService paymentMethodConfigService;
    private final com.kalon.service.StockMovementService stockMovementService;
    private final com.kalon.service.InvoiceService invoiceService;
    private final com.kalon.repository.OrderRepository orderRepository;
    private final CouponService couponService;

    // Dashboard
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsDTO>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats()));
    }

    // Product Management
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<Page<ProductDTO>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) java.math.BigDecimal minPrice,
            @RequestParam(required = false) java.math.BigDecimal maxPrice,
            @RequestParam(required = false) Long categoryId) {
        Pageable pageable = PageRequest.of(page, size);

        boolean hasKeyword = search != null && !search.trim().isEmpty();
        boolean hasFilters = minPrice != null || maxPrice != null || categoryId != null;

        if (hasKeyword && !hasFilters) {
            // Use advanced search (full-text + fuzzy) for keyword-only searches — includes inactive products
            return ResponseEntity.ok(ApiResponse.success(productService.advancedSearchAdmin(search.trim(), pageable)));
        }
        if (hasFilters || hasKeyword) {
            // Combined keyword + filters uses LIKE-based query
            return ResponseEntity.ok(ApiResponse.success(
                    productService.getProductsWithAdminFilters(search, minPrice, maxPrice, categoryId, pageable)));
        }
        return ResponseEntity.ok(ApiResponse.success(productService.getAllProducts(pageable)));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDTO>> createProduct(@Valid @RequestBody ProductDTO dto) {
        ProductDTO created = productService.createProduct(dto);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.CREATE,
                com.kalon.entity.AdminActivityLog.ResourceType.PRODUCT,
                created.getId(), created.getName(), null);
        return ResponseEntity.ok(ApiResponse.success("Product created", created));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDTO>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductDTO dto) {
        ProductDTO updated = productService.updateProduct(id, dto);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPDATE,
                com.kalon.entity.AdminActivityLog.ResourceType.PRODUCT,
                id, updated.getName(), null);
        return ResponseEntity.ok(ApiResponse.success("Product updated", updated));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.DELETE,
                com.kalon.entity.AdminActivityLog.ResourceType.PRODUCT,
                id, null, "Product deactivated");
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    // Category Management
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<java.util.List<CategoryDTO>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(@Valid @RequestBody CategoryDTO dto) {
        CategoryDTO created = categoryService.createCategory(dto);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.CREATE,
                com.kalon.entity.AdminActivityLog.ResourceType.CATEGORY,
                created.getId(), created.getName(), null);
        return ResponseEntity.ok(ApiResponse.success("Category created", created));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryDTO dto) {
        CategoryDTO updated = categoryService.updateCategory(id, dto);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPDATE,
                com.kalon.entity.AdminActivityLog.ResourceType.CATEGORY,
                id, updated.getName(), null);
        return ResponseEntity.ok(ApiResponse.success("Category updated", updated));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.DELETE,
                com.kalon.entity.AdminActivityLog.ResourceType.CATEGORY,
                id, null, "Category deactivated");
        return ResponseEntity.ok(ApiResponse.success("Category deleted", null));
    }

    // Order Management
    @GetMapping("/orders")
    public ResponseEntity<ApiResponse<Page<OrderDTO>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount) {

        // Parse sort parameter (format: "field,direction" e.g. "createdAt,desc")
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "createdAt"); // default
        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(",");
            String field = parts[0];
            Sort.Direction direction = parts.length > 1 && "asc".equalsIgnoreCase(parts[1])
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            sortOrder = Sort.by(direction, field);
        }

        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // Parse status
        com.kalon.entity.Order.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = com.kalon.entity.Order.OrderStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid order status: " + status));
            }
        }

        // Parse date filters
        java.time.LocalDateTime dateFromParsed = null;
        java.time.LocalDateTime dateToParsed = null;
        try {
            if (dateFrom != null && !dateFrom.isEmpty()) {
                dateFromParsed = java.time.LocalDate.parse(dateFrom).atStartOfDay();
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                dateToParsed = java.time.LocalDate.parse(dateTo).atTime(23, 59, 59);
            }
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid date format. Use yyyy-MM-dd"));
        }

        // Check if any advanced filters are active
        boolean hasFilters = orderStatus != null || dateFromParsed != null || dateToParsed != null
                || minAmount != null || maxAmount != null;

        if (hasFilters) {
            return ResponseEntity.ok(ApiResponse.success(
                    orderService.getOrdersWithFilters(orderStatus, dateFromParsed, dateToParsed, minAmount, maxAmount, pageable)));
        }

        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(pageable)));
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam String format,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) java.math.BigDecimal minAmount,
            @RequestParam(required = false) java.math.BigDecimal maxAmount) {

        if (!java.util.Set.of("csv", "xlsx", "pdf").contains(format.toLowerCase())) {
            return ResponseEntity.badRequest().build();
        }

        com.kalon.entity.Order.OrderStatus orderStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                orderStatus = com.kalon.entity.Order.OrderStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().build();
            }
        }

        java.time.LocalDateTime dateFromParsed = null;
        java.time.LocalDateTime dateToParsed = null;
        try {
            if (dateFrom != null && !dateFrom.isEmpty()) {
                dateFromParsed = java.time.LocalDate.parse(dateFrom).atStartOfDay();
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                dateToParsed = java.time.LocalDate.parse(dateTo).atTime(23, 59, 59);
            }
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            java.util.List<com.kalon.entity.Order> orders =
                    orderExportService.getFilteredOrders(orderStatus, dateFromParsed, dateToParsed, minAmount, maxAmount);

            byte[] fileContent;
            String contentType;
            String formatLower = format.toLowerCase();

            switch (formatLower) {
                case "csv":
                    fileContent = orderExportService.generateCsv(orders);
                    contentType = "text/csv";
                    break;
                case "xlsx":
                    fileContent = orderExportService.generateExcel(orders);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    break;
                case "pdf":
                    fileContent = orderExportService.generatePdf(orders);
                    contentType = "application/pdf";
                    break;
                default:
                    return ResponseEntity.badRequest().build();
            }

            String filename = orderExportService.generateFilename(formatLower, orderStatus);

            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .header("Content-Type", contentType)
                    .header("Access-Control-Expose-Headers", "Content-Disposition")
                    .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/orders/{orderId}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrderByIdForAdmin(orderId)));
    }

    @PutMapping("/orders/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam String status) {
        try {
            com.kalon.entity.Order.OrderStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid order status: " + status));
        }
        OrderDTO updated = orderService.updateOrderStatus(orderId, status);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.STATUS_CHANGE,
                com.kalon.entity.AdminActivityLog.ResourceType.ORDER,
                orderId, updated.getOrderNumber(), "Status changed to " + status);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", updated));
    }

    // User Management
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Page<UserDTO>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(pageable)));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDTO>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserDTO dto) {
        UserDTO updated = userService.updateUser(id, dto);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPDATE,
                com.kalon.entity.AdminActivityLog.ResourceType.USER,
                id, updated.getEmail(), null);
        return ResponseEntity.ok(ApiResponse.success("User updated", updated));
    }

    @PutMapping("/users/{id}/toggle-status")
    public ResponseEntity<ApiResponse<Void>> toggleUserStatus(@PathVariable Long id) {
        userService.toggleUserStatus(id);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.STATUS_CHANGE,
                com.kalon.entity.AdminActivityLog.ResourceType.USER,
                id, null, "User status toggled");
        return ResponseEntity.ok(ApiResponse.success("User status updated", null));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.DELETE,
                com.kalon.entity.AdminActivityLog.ResourceType.USER,
                id, null, "User deleted");
        return ResponseEntity.ok(ApiResponse.success("User deleted", null));
    }

    // Site Configuration Management
    @GetMapping("/site-config")
    public ResponseEntity<ApiResponse<java.util.List<SiteConfigDTO>>> getAllSiteConfigs() {
        return ResponseEntity.ok(ApiResponse.success(siteConfigService.getAllConfigs()));
    }

    @GetMapping("/site-config/{key}")
    public ResponseEntity<ApiResponse<SiteConfigDTO>> getSiteConfigByKey(@PathVariable String key) {
        SiteConfigDTO config = siteConfigService.getConfigByKey(key);
        if (config == null) {
            return ResponseEntity.ok(ApiResponse.error("Config not found"));
        }
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PostMapping("/site-config")
    public ResponseEntity<ApiResponse<SiteConfigDTO>> createOrUpdateSiteConfig(@Valid @RequestBody SiteConfigDTO dto) {
        SiteConfigDTO result = siteConfigService.createOrUpdateConfig(dto);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPDATE,
                com.kalon.entity.AdminActivityLog.ResourceType.SITE_CONFIG,
                result.getId(), result.getConfigKey(), null);
        return ResponseEntity.ok(ApiResponse.success("Config updated", result));
    }

    @DeleteMapping("/site-config/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSiteConfig(@PathVariable Long id) {
        siteConfigService.deleteConfig(id);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.DELETE,
                com.kalon.entity.AdminActivityLog.ResourceType.SITE_CONFIG,
                id, null, "Site config deleted");
        return ResponseEntity.ok(ApiResponse.success("Config deleted", null));
    }

    // Payment Method Configuration
    @GetMapping("/payment-methods")
    public ResponseEntity<ApiResponse<java.util.List<PaymentMethodConfigDTO>>> getAllPaymentMethods() {
        return ResponseEntity.ok(ApiResponse.success(paymentMethodConfigService.getAllMethods()));
    }

    @PostMapping("/payment-methods")
    public ResponseEntity<ApiResponse<PaymentMethodConfigDTO>> createOrUpdatePaymentMethod(
            @Valid @RequestBody PaymentMethodConfigDTO dto) {
        PaymentMethodConfigDTO result = paymentMethodConfigService.createOrUpdate(dto);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPDATE,
                com.kalon.entity.AdminActivityLog.ResourceType.SITE_CONFIG,
                result.getId(), result.getPaymentMethod(), "Payment method config updated");
        return ResponseEntity.ok(ApiResponse.success("Payment method config updated", result));
    }

    @PutMapping("/payment-methods/{id}/toggle")
    public ResponseEntity<ApiResponse<PaymentMethodConfigDTO>> togglePaymentMethod(
            @PathVariable Long id, @RequestParam boolean enabled) {
        PaymentMethodConfigDTO result = paymentMethodConfigService.toggleMethod(id, enabled);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPDATE,
                com.kalon.entity.AdminActivityLog.ResourceType.SITE_CONFIG,
                id, result.getPaymentMethod(), "Payment method " + (enabled ? "enabled" : "disabled"));
        return ResponseEntity.ok(ApiResponse.success("Payment method " + (enabled ? "enabled" : "disabled"), result));
    }

    @GetMapping("/orders/{orderId}/invoice")
    public ResponseEntity<byte[]> downloadInvoiceAdmin(@PathVariable Long orderId) {
        com.kalon.entity.Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new com.kalon.exception.ResourceNotFoundException("Order not found"));

        byte[] pdfBytes = invoiceService.generateInvoicePdf(order);

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "attachment; filename=invoice-" + order.getOrderNumber() + ".pdf")
                .body(pdfBytes);
    }

    // Payment Attempts (Admin view)
    @GetMapping("/orders/{orderId}/payment-attempts")
    public ResponseEntity<ApiResponse<java.util.List<PaymentAttemptDTO>>> getPaymentAttemptsForAdmin(
            @PathVariable Long orderId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getPaymentAttemptsForAdmin(orderId)));
    }

    // Return Request Management
    @GetMapping("/returns")
    public ResponseEntity<ApiResponse<Page<ReturnRequestDTO>>> getAllReturnRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // If search is provided, use search endpoint (ignores status/type filters)
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(returnService.searchReturnRequests(search.trim(), pageable)));
        }

        try {
            if (status != null && !status.isEmpty() && type != null && !type.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(
                        returnService.getReturnRequestsByStatusAndType(
                                com.kalon.entity.ReturnRequest.ReturnStatus.valueOf(status),
                                com.kalon.entity.ReturnRequest.RequestType.valueOf(type),
                                pageable)));
            } else if (status != null && !status.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(
                        returnService.getReturnRequestsByStatus(
                                com.kalon.entity.ReturnRequest.ReturnStatus.valueOf(status), pageable)));
            } else if (type != null && !type.isEmpty()) {
                return ResponseEntity.ok(ApiResponse.success(
                        returnService.getReturnRequestsByType(
                                com.kalon.entity.ReturnRequest.RequestType.valueOf(type), pageable)));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status or type value"));
        }

        return ResponseEntity.ok(ApiResponse.success(returnService.getAllReturnRequests(pageable)));
    }

    @GetMapping("/returns/{returnRequestId}")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> getReturnRequestById(@PathVariable Long returnRequestId) {
        return ResponseEntity.ok(ApiResponse.success(returnService.getReturnRequestByIdForAdmin(returnRequestId)));
    }

    @PostMapping("/returns/{returnRequestId}/approve")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> approveReturnRequest(
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String adminNotes) {
        ReturnRequestDTO result = returnService.approveReturnRequest(returnRequestId, adminNotes);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.APPROVE,
                com.kalon.entity.AdminActivityLog.ResourceType.RETURN_REQUEST,
                returnRequestId, "Return #" + returnRequestId, adminNotes);
        return ResponseEntity.ok(ApiResponse.success("Return request approved", result));
    }

    @PostMapping("/returns/{returnRequestId}/reject")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> rejectReturnRequest(
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String adminNotes) {
        ReturnRequestDTO result = returnService.rejectReturnRequest(returnRequestId, adminNotes);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.REJECT,
                com.kalon.entity.AdminActivityLog.ResourceType.RETURN_REQUEST,
                returnRequestId, "Return #" + returnRequestId, adminNotes);
        return ResponseEntity.ok(ApiResponse.success("Return request rejected", result));
    }

    @PostMapping("/returns/{returnRequestId}/process")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> processReturnRequest(
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String adminNotes) {
        ReturnRequestDTO result = returnService.processReturnRequest(returnRequestId, adminNotes);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.PROCESS,
                com.kalon.entity.AdminActivityLog.ResourceType.RETURN_REQUEST,
                returnRequestId, "Return #" + returnRequestId, adminNotes != null ? adminNotes : "Processing initiated");
        return ResponseEntity.ok(ApiResponse.success("Return request is being processed", result));
    }

    @PostMapping("/returns/{returnRequestId}/complete")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> completeReturnRequest(
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String adminNotes) {
        ReturnRequestDTO result = returnService.completeReturnRequest(returnRequestId, adminNotes);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.COMPLETE,
                com.kalon.entity.AdminActivityLog.ResourceType.RETURN_REQUEST,
                returnRequestId, "Return #" + returnRequestId, adminNotes != null ? adminNotes : "Return completed");
        return ResponseEntity.ok(ApiResponse.success("Return request completed", result));
    }

    @PostMapping("/returns/{returnRequestId}/cancel")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> adminCancelReturnRequest(
            @PathVariable Long returnRequestId,
            @RequestParam(required = false) String adminNotes) {
        ReturnRequestDTO result = returnService.adminCancelReturnRequest(returnRequestId, adminNotes);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.REJECT,
                com.kalon.entity.AdminActivityLog.ResourceType.RETURN_REQUEST,
                returnRequestId, "Return #" + returnRequestId, adminNotes != null ? adminNotes : "Return cancelled by admin");
        return ResponseEntity.ok(ApiResponse.success("Return request cancelled", result));
    }

    @PostMapping("/returns/{returnRequestId}/retry-refund")
    public ResponseEntity<ApiResponse<ReturnRequestDTO>> retryRefund(@PathVariable Long returnRequestId) {
        ReturnRequestDTO result = returnService.retryRefund(returnRequestId);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.PROCESS,
                com.kalon.entity.AdminActivityLog.ResourceType.RETURN_REQUEST,
                returnRequestId, "Return #" + returnRequestId, "Refund retry initiated");
        return ResponseEntity.ok(ApiResponse.success("Refund retry initiated", result));
    }

    // Image Upload
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File is empty"));
            }
            if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(ApiResponse.error("File must be an image"));
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("File size exceeds the maximum limit of 10MB"));
            }
            String fileUrl = fileStorageService.storeFile(file);
            activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPLOAD,
                    com.kalon.entity.AdminActivityLog.ResourceType.FILE,
                    null, file.getOriginalFilename(), "File uploaded: " + fileUrl);
            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", fileUrl));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }

    // Review Management
    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<com.kalon.dto.ReviewDTO>>> getAllReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Boolean isApproved,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        boolean hasFilters = productId != null || userId != null || isApproved != null
                || minRating != null || maxRating != null;
        if (hasFilters) {
            return ResponseEntity.ok(ApiResponse.success(
                    reviewService.getReviewsFiltered(productId, userId, isApproved, minRating, maxRating, pageable)));
        }
        return ResponseEntity.ok(ApiResponse.success(reviewService.getAllReviews(pageable)));
    }

    @PostMapping("/reviews/{reviewId}/approve")
    public ResponseEntity<ApiResponse<com.kalon.dto.ReviewDTO>> approveReview(@PathVariable Long reviewId) {
        com.kalon.dto.ReviewDTO result = reviewService.approveReview(reviewId);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.APPROVE,
                com.kalon.entity.AdminActivityLog.ResourceType.REVIEW,
                reviewId, null, "Review approved");
        return ResponseEntity.ok(ApiResponse.success("Review approved", result));
    }

    @PostMapping("/reviews/{reviewId}/reject")
    public ResponseEntity<ApiResponse<com.kalon.dto.ReviewDTO>> rejectReview(@PathVariable Long reviewId) {
        com.kalon.dto.ReviewDTO result = reviewService.rejectReview(reviewId);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.REJECT,
                com.kalon.entity.AdminActivityLog.ResourceType.REVIEW,
                reviewId, null, "Review rejected");
        return ResponseEntity.ok(ApiResponse.success("Review rejected", result));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.adminDeleteReview(reviewId);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.DELETE,
                com.kalon.entity.AdminActivityLog.ResourceType.REVIEW,
                reviewId, null, "Review deleted");
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    // Activity Logs
    @GetMapping("/activity-logs")
    public ResponseEntity<ApiResponse<Page<com.kalon.dto.AdminActivityLogDTO>>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long adminId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // If search is provided, use search endpoint (ignores other filters)
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(ApiResponse.success(
                    activityLogService.searchActivityLogs(search.trim(), pageable)));
        }

        // Parse filters
        com.kalon.entity.AdminActivityLog.Action actionEnum = null;
        com.kalon.entity.AdminActivityLog.ResourceType resourceTypeEnum = null;
        java.time.LocalDateTime dateFromParsed = null;
        java.time.LocalDateTime dateToParsed = null;

        try {
            if (action != null && !action.isEmpty()) {
                actionEnum = com.kalon.entity.AdminActivityLog.Action.valueOf(action);
            }
            if (resourceType != null && !resourceType.isEmpty()) {
                resourceTypeEnum = com.kalon.entity.AdminActivityLog.ResourceType.valueOf(resourceType);
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid action or resource type"));
        }

        try {
            if (dateFrom != null && !dateFrom.isEmpty()) {
                dateFromParsed = java.time.LocalDate.parse(dateFrom).atStartOfDay();
            }
            if (dateTo != null && !dateTo.isEmpty()) {
                dateToParsed = java.time.LocalDate.parse(dateTo).atTime(23, 59, 59);
            }
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid date format. Use yyyy-MM-dd"));
        }

        boolean hasFilters = adminId != null || actionEnum != null || resourceTypeEnum != null
                || dateFromParsed != null || dateToParsed != null;

        if (hasFilters) {
            return ResponseEntity.ok(ApiResponse.success(
                    activityLogService.getActivityLogsWithFilters(
                            adminId, actionEnum, resourceTypeEnum, dateFromParsed, dateToParsed, pageable)));
        }

        return ResponseEntity.ok(ApiResponse.success(activityLogService.getActivityLogs(pageable)));
    }

    @GetMapping("/activity-logs/admins")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getActivityLogAdmins() {
        return ResponseEntity.ok(ApiResponse.success(activityLogService.getDistinctAdmins()));
    }

    // ─── Analytics & Reporting ───

    @GetMapping("/analytics/sales")
    public ResponseEntity<ApiResponse<SalesAnalyticsDTO>> getSalesAnalytics(
            @RequestParam(defaultValue = "DAILY") String period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getSalesAnalytics(period, dateFrom, dateTo)));
    }

    @GetMapping("/analytics/products")
    public ResponseEntity<ApiResponse<ProductAnalyticsDTO>> getProductAnalytics(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "revenue") String sortBy,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getProductAnalytics(page, size, sortBy, dateFrom, dateTo)));
    }

    @GetMapping("/analytics/customers")
    public ResponseEntity<ApiResponse<CustomerAnalyticsDTO>> getCustomerAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getCustomerAnalytics(dateFrom, dateTo, page, size)));
    }

    @GetMapping("/analytics/revenue")
    public ResponseEntity<ApiResponse<RevenueAnalyticsDTO>> getRevenueAnalytics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "DAY") String groupBy) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getRevenueAnalytics(dateFrom, dateTo, groupBy)));
    }

    @GetMapping("/analytics/overview")
    public ResponseEntity<ApiResponse<OverviewAnalyticsDTO>> getOverviewAnalytics() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getOverviewAnalytics()));
    }

    // ─── Stock Movement History ───

    @GetMapping("/stock-movements")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<StockMovementDTO>>> getStockMovements(
            @RequestParam(required = false) Long variantId,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String movementType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);

        com.kalon.entity.StockMovement.MovementType typeEnum = null;
        if (movementType != null && !movementType.isEmpty()) {
            try {
                typeEnum = com.kalon.entity.StockMovement.MovementType.valueOf(movementType);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Invalid movement type: " + movementType));
            }
        }

        java.time.LocalDateTime dateFromParsed = dateFrom != null ? dateFrom.atStartOfDay() : null;
        java.time.LocalDateTime dateToParsed = dateTo != null ? dateTo.atTime(23, 59, 59) : null;

        return ResponseEntity.ok(ApiResponse.success(
                stockMovementService.getMovementsWithFilters(variantId, productId, typeEnum, dateFromParsed, dateToParsed, pageable)));
    }

    @GetMapping("/stock-movements/variant/{variantId}")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<StockMovementDTO>>> getStockMovementsByVariant(
            @PathVariable Long variantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(stockMovementService.getMovementsByVariant(variantId, pageable)));
    }

    @GetMapping("/stock-movements/product/{productId}")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<StockMovementDTO>>> getStockMovementsByProduct(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(stockMovementService.getMovementsByProduct(productId, pageable)));
    }

    // ─── Coupon Management ───

    @GetMapping("/coupons")
    public ResponseEntity<ApiResponse<Page<CouponDTO>>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(couponService.getAllCoupons(pageable)));
    }

    @GetMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponDTO>> getCouponById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(couponService.getCouponById(id)));
    }

    @PostMapping("/coupons")
    public ResponseEntity<ApiResponse<CouponDTO>> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        CouponDTO created = couponService.createCoupon(request);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.CREATE,
                com.kalon.entity.AdminActivityLog.ResourceType.SITE_CONFIG,
                created.getId(), created.getCode(), "Coupon created");
        return ResponseEntity.ok(ApiResponse.success("Coupon created", created));
    }

    @PutMapping("/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponDTO>> updateCoupon(@PathVariable Long id,
            @Valid @RequestBody CreateCouponRequest request) {
        CouponDTO updated = couponService.updateCoupon(id, request);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.UPDATE,
                com.kalon.entity.AdminActivityLog.ResourceType.SITE_CONFIG,
                id, updated.getCode(), "Coupon updated");
        return ResponseEntity.ok(ApiResponse.success("Coupon updated", updated));
    }

    @PostMapping("/coupons/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleCouponStatus(@PathVariable Long id) {
        couponService.toggleCouponStatus(id);
        activityLogService.log(com.kalon.entity.AdminActivityLog.Action.STATUS_CHANGE,
                com.kalon.entity.AdminActivityLog.ResourceType.SITE_CONFIG,
                id, null, "Coupon status toggled");
        return ResponseEntity.ok(ApiResponse.success("Coupon status toggled", null));
    }
}
