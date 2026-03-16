package com.kalon.service;

import com.kalon.repository.PasswordResetTokenRepository;
import com.kalon.repository.RefreshTokenRepository;
import com.kalon.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();

        tokenBlacklistRepository.deleteExpired(now);
        refreshTokenRepository.deleteExpired(now);
        passwordResetTokenRepository.deleteExpiredTokens(now);

        log.info("Expired tokens cleanup completed at {}", now);
    }
}
