package com.kalon.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kalon.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Map<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${rate-limit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Value("${rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!rateLimitEnabled) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIP(request);
        String requestPath = request.getRequestURI();

        // Determine rate limit based on endpoint sensitivity
        int limit = isAuthEndpoint(requestPath) ? authRequestsPerMinute : requestsPerMinute;
        String bucketKey = clientIp + ":" + (isAuthEndpoint(requestPath) ? "auth" : "general");

        RateLimitBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new RateLimitBucket(limit));

        if (!bucket.tryConsume()) {
            log.warn("Rate limit exceeded for IP: {} on path: {}", clientIp, requestPath);
            sendRateLimitResponse(response, bucket.getSecondsUntilReset());
            return;
        }

        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(bucket.getRemaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(bucket.getResetTime()));

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String path) {
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/register") ||
               path.startsWith("/api/auth/forgot-password");
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private void sendRateLimitResponse(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        ApiResponse<?> apiResponse = ApiResponse.error(
                "Rate limit exceeded. Please try again in " + retryAfterSeconds + " seconds.");
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private static class RateLimitBucket {
        private final int limit;
        private final AtomicInteger tokens;
        private volatile long windowStart;
        private static final long WINDOW_SIZE_MS = 60_000; // 1 minute window

        public RateLimitBucket(int limit) {
            this.limit = limit;
            this.tokens = new AtomicInteger(limit);
            this.windowStart = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            long now = System.currentTimeMillis();
            if (now - windowStart >= WINDOW_SIZE_MS) {
                // Reset window
                windowStart = now;
                tokens.set(limit);
            }
            return tokens.getAndDecrement() > 0;
        }

        public int getRemaining() {
            return Math.max(0, tokens.get());
        }

        public long getResetTime() {
            return (windowStart + WINDOW_SIZE_MS) / 1000;
        }

        public long getSecondsUntilReset() {
            long remaining = (windowStart + WINDOW_SIZE_MS) - System.currentTimeMillis();
            return Math.max(1, remaining / 1000);
        }
    }
}
