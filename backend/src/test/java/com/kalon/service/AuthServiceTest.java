package com.kalon.service;

import com.kalon.dto.AuthResponse;
import com.kalon.dto.RegisterRequest;
import com.kalon.entity.User;
import com.kalon.exception.EmailAlreadyExistsException;
import com.kalon.repository.CartRepository;
import com.kalon.repository.PasswordResetTokenRepository;
import com.kalon.repository.RefreshTokenRepository;
import com.kalon.repository.TokenBlacklistRepository;
import com.kalon.repository.UserRepository;
import com.kalon.security.GoogleTokenVerifier;
import com.kalon.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CartRepository cartRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenBlacklistRepository tokenBlacklistRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private GoogleTokenVerifier googleTokenVerifier;
    @Mock private EmailService emailService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                userRepository, cartRepository, refreshTokenRepository,
                tokenBlacklistRepository, passwordResetTokenRepository,
                passwordEncoder, jwtTokenProvider, authenticationManager,
                googleTokenVerifier, emailService,
                604800000L, 3600000L
        );
    }

    @Test
    void register_shouldCreateUserAndReturnAuthResponse() {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setPassword("Password123");
        request.setPhoneNumber("+919876543210");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtTokenProvider.generateToken(anyString())).thenReturn("accessToken");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(cartRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getEmail()).isEqualTo("john@example.com");
        assertThat(response.getToken()).isEqualTo("accessToken");
        verify(userRepository).save(any(User.class));
        verify(emailService).sendWelcomeEmail(any(User.class));
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void forgotPassword_shouldNotRevealNonExistentEmail() {
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(java.util.Optional.empty());

        // Should not throw - prevents email enumeration
        authService.forgotPassword("nonexistent@example.com");

        verify(passwordResetTokenRepository, never()).save(any());
    }
}
