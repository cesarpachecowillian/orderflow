package com.orderflow.auth.service;

import com.orderflow.auth.dto.AuthResponse;
import com.orderflow.auth.dto.LoginRequest;
import com.orderflow.auth.dto.RegisterRequest;
import com.orderflow.auth.dto.UserResponse;
import com.orderflow.auth.entity.Role;
import com.orderflow.auth.entity.User;
import com.orderflow.auth.exception.EmailAlreadyExistsException;
import com.orderflow.auth.exception.InvalidCredentialsException;
import com.orderflow.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    private User existing;

    @BeforeEach
    void setup() {
        existing = new User("César", "cesar@email.com", "$2a$hashedpwd", Role.USER);
        existing.setId(1L);
    }

    @Test
    void register_success() {
        var req = new RegisterRequest("César", "cesar@email.com", "senha123");
        when(userRepository.existsByEmail("cesar@email.com")).thenReturn(false);
        when(passwordEncoder.encode("senha123")).thenReturn("$2a$hashedpwd");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        UserResponse resp = authService.register(req);

        assertThat(resp.id()).isEqualTo(42L);
        assertThat(resp.email()).isEqualTo("cesar@email.com");
        assertThat(resp.role()).isEqualTo(Role.USER);
        verify(passwordEncoder).encode("senha123");
    }

    @Test
    void register_duplicateEmail_throws() {
        var req = new RegisterRequest("César", "cesar@email.com", "senha123");
        when(userRepository.existsByEmail("cesar@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_success() {
        var req = new LoginRequest("cesar@email.com", "senha123");
        when(userRepository.findByEmail("cesar@email.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("senha123", "$2a$hashedpwd")).thenReturn(true);
        when(jwtService.generate(existing)).thenReturn("jwt.token.here");
        when(jwtService.getExpirationMs()).thenReturn(86400000L);

        AuthResponse resp = authService.login(req);

        assertThat(resp.token()).isEqualTo("jwt.token.here");
        assertThat(resp.type()).isEqualTo("Bearer");
        assertThat(resp.expiresInMs()).isEqualTo(86400000L);
    }

    @Test
    void login_userNotFound_throwsInvalidCredentials() {
        var req = new LoginRequest("nope@email.com", "senha123");
        when(userRepository.findByEmail("nope@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        var req = new LoginRequest("cesar@email.com", "errada");
        when(userRepository.findByEmail("cesar@email.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
