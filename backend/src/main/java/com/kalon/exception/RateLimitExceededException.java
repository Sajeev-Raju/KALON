package com.kalon.exception;

import lombok.Getter;

/**
 * Exception thrown when a rate limit is exceeded.
 * Reserved for future rate limiting implementation on sensitive endpoints
 * (e.g., login attempts, password reset requests, API calls).
 */
@Getter
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message) {
        this(message, 60);
    }

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
