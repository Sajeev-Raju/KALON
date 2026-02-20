package com.kalon.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.kalon.dto.AuthRequest;
import com.kalon.dto.AuthResponse;
import com.kalon.dto.RegisterRequest;
import com.kalon.entity.Cart;
import com.kalon.entity.PasswordResetToken;
import com.kalon.entity.RefreshToken;
import com.kalon.entity.TokenBlacklist;
import com.kalon.entity.User;
import com.kalon.exception.AccountDisabledException;
import com.kalon.exception.InvalidCredentialsException;
import com.kalon.exception.InvalidTokenException;
import com.kalon.repository.CartRepository;
import com.kalon.repository.PasswordResetTokenRepository;
import com.kalon.repository.RefreshTokenRepository;
import com.kalon.repository.TokenBlacklistRepository;
import com.kalon.repository.UserRepository;
import com.kalon.security.GoogleTokenVerifier;
import com.kalon.security.JwtTokenProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final EmailService emailService;
    private final long refreshTokenExpiration;
    private final long passwordResetTokenExpiryMs;

    public AuthService(UserRepository userRepository,
                       CartRepository cartRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       TokenBlacklistRepository tokenBlacklistRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager,
                       GoogleTokenVerifier googleTokenVerifier,
                       EmailService emailService,
                       @Value("${jwt.refresh-expiration}") long refreshTokenExpiration,
                       @Value("${app.password-reset-token-expiry-ms}") long passwordResetTokenExpiryMs) {
        this.userRepository = userRepository;
        this.cartRepository = cartRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
        this.googleTokenVerifier = googleTokenVerifier;
        this.emailService = emailService;
        this.refreshTokenExpiration = refreshTokenExpiration;
        this.passwordResetTokenExpiryMs = passwordResetTokenExpiryMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new com.kalon.exception.EmailAlreadyExistsException(
                    "Email already exists. Please use a different email or login with existing account.");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .role(User.Role.USER)
                .isActive(true)
                .build();

        user = userRepository.save(user);

        Cart cart = Cart.builder().user(user).build();
        cartRepository.save(cart);

        log.info("User registered: userId={}, email={}", user.getId(), user.getEmail());
        emailService.sendWelcomeEmail(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Check if user exists and is an OAuth-only account
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (user.getPassword() == null) {
            log.warn("Login attempt on Google-only account: email={}", request.getEmail());
            throw new InvalidCredentialsException(
                    "This account uses Google sign-in. Please use the Google button to login.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        log.info("User logged in: userId={}, email={}", user.getId(), user.getEmail());
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse loginWithGoogle(com.kalon.dto.GoogleAuthRequest request) {
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(request.getIdToken());
        if (payload == null) {
            log.warn("Invalid Google ID token received");
            throw new InvalidCredentialsException("Invalid Google ID token");
        }

        String verifiedEmail = payload.getEmail();
        if (verifiedEmail == null || !Boolean.TRUE.equals(payload.getEmailVerified())) {
            log.warn("Google account email not verified: email={}", verifiedEmail);
            throw new InvalidCredentialsException("Google account email not verified");
        }

        String firstName = (String) payload.get("given_name");
        String lastName = (String) payload.get("family_name");
        String googleUserId = payload.getSubject();

        User user = userRepository.findByEmail(verifiedEmail).orElse(null);

        if (user == null) {
            user = User.builder()
                    .email(verifiedEmail)
                    .firstName(firstName != null ? firstName : "")
                    .lastName(lastName != null ? lastName : "")
                    .provider("google")
                    .providerId(googleUserId)
                    .password(null)
                    .role(User.Role.USER)
                    .isActive(true)
                    .build();
            user = userRepository.save(user);

            Cart cart = Cart.builder().user(user).build();
            cartRepository.save(cart);

            log.info("New Google user registered: userId={}, email={}", user.getId(), verifiedEmail);
            emailService.sendWelcomeEmail(user);
        } else {
            if (!user.isActive()) {
                log.warn("Google login attempt on deactivated account: userId={}, email={}", user.getId(), verifiedEmail);
                throw new AccountDisabledException("Your account has been deactivated. Please contact support.");
            }

            if (user.getProvider() == null || !"google".equals(user.getProvider())) {
                user.setProvider("google");
                user.setProviderId(googleUserId);
                user = userRepository.save(user);
                log.info("Linked Google provider to existing account: userId={}, email={}", user.getId(), verifiedEmail);
            }

            log.info("Google user logged in: userId={}, email={}", user.getId(), verifiedEmail);
        }

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new InvalidCredentialsException("Refresh token expired. Please login again.");
        }

        User user = refreshToken.getUser();

        // Delete old refresh token and create new one (rotation)
        refreshTokenRepository.delete(refreshToken);

        log.debug("Token refreshed: userId={}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public void logout(String accessToken) {
        // Blacklist the access token
        if (accessToken != null) {
            try {
                LocalDateTime expiresAt = jwtTokenProvider.getExpirationAsLocalDateTime(accessToken);
                String hash = jwtTokenProvider.hashToken(accessToken);

                TokenBlacklist blacklisted = TokenBlacklist.builder()
                        .tokenHash(hash)
                        .expiresAt(expiresAt)
                        .build();
                tokenBlacklistRepository.save(blacklisted);
                log.debug("Access token blacklisted");
            } catch (Exception e) {
                // Token might already be invalid/expired, that's fine
                log.debug("Could not blacklist access token (may already be expired)");
            }
        }
    }

    @Transactional
    public void logoutByRefreshToken(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(rt -> {
                    // Delete all refresh tokens for this user
                    refreshTokenRepository.deleteByUserId(rt.getUser().getId());
                    log.info("User logged out: userId={}", rt.getUser().getId());
                });
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email).orElse(null);

        // Always return success to prevent email enumeration
        if (user == null) {
            log.debug("Forgot password request for non-existent email: {}", email);
            return;
        }

        // Google-only accounts cannot reset password
        if (user.getPassword() == null && "google".equals(user.getProvider())) {
            log.debug("Forgot password request for Google-only account: {}", email);
            return;
        }

        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());

        // Generate new token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(passwordResetTokenExpiryMs / 1000);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Send email (async)
        emailService.sendPasswordResetEmail(user, token);

        log.info("Password reset token generated: userId={}, email={}", user.getId(), email);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset link."));

        if (resetToken.isExpired()) {
            passwordResetTokenRepository.delete(resetToken);
            throw new InvalidTokenException("Password reset link has expired. Please request a new one.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Delete the used token (and any others for this user)
        passwordResetTokenRepository.deleteByUserId(user.getId());

        log.info("Password reset successful: userId={}, email={}", user.getId(), user.getEmail());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateToken(user.getEmail());
        String refreshTokenStr = createRefreshToken(user);

        return AuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshTokenStr)
                .type("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .build();
    }

    private String createRefreshToken(User user) {
        String token = UUID.randomUUID().toString();
        long refreshMs = refreshTokenExpiration;
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        return token;
    }
}
