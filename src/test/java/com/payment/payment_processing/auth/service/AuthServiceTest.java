package com.payment.payment_processing.auth.service;

import com.payment.payment_processing.auth.dto.LoginRequest;
import com.payment.payment_processing.user.entity.Role;
import com.payment.payment_processing.user.entity.User;
import com.payment.payment_processing.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private LoginRequest validLoginRequest;
    private String encodedPassword;
    private String testToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setPassword("encodedPassword123");
        testUser.setRole(Role.CUSTOMER);

        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword("rawPassword123");

        encodedPassword = "encodedPassword123";
        testToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }

    @Test
    void authenticate_ValidCredentials_ReturnsToken() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword123", encodedPassword)).thenReturn(true);
        when(jwtService.generateToken("test@example.com", "CUSTOMER")).thenReturn(testToken);

        String result = authService.authenticate(validLoginRequest);

        assertNotNull(result);
        assertEquals(testToken, result);

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("rawPassword123", encodedPassword);
        verify(jwtService).generateToken("test@example.com", "CUSTOMER");
    }

    @Test
    void authenticate_InvalidEmail_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(request);
        });

        verify(userRepository).findByEmail("nonexistent@example.com");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void authenticate_InvalidPassword_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("rawPassword123", encodedPassword)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(validLoginRequest);
        });

        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).matches("rawPassword123", encodedPassword);
        verify(jwtService, never()).generateToken(anyString(), anyString());
    }

    @Test
    void authenticate_AdminRole_GeneratesAdminToken() {
        User adminUser = new User();
        adminUser.setId(2L);
        adminUser.setEmail("admin@example.com");
        adminUser.setName("Admin User");
        adminUser.setPassword("encodedPassword123");
        adminUser.setRole(Role.ADMIN);

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("adminPassword", encodedPassword)).thenReturn(true);
        when(jwtService.generateToken("admin@example.com", "ADMIN")).thenReturn(testToken);

        LoginRequest adminRequest = new LoginRequest();
        adminRequest.setEmail("admin@example.com");
        adminRequest.setPassword("adminPassword");

        String result = authService.authenticate(adminRequest);

        assertNotNull(result);
        assertEquals(testToken, result);

        verify(jwtService).generateToken("admin@example.com", "ADMIN");
    }

    @Test
    void authenticate_CustomerRole_GeneratesCustomerToken() {
        User customerUser = new User();
        customerUser.setId(3L);
        customerUser.setEmail("customer@example.com");
        customerUser.setName("Customer User");
        customerUser.setPassword("encodedPassword123");
        customerUser.setRole(Role.CUSTOMER);

        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(customerUser));
        when(passwordEncoder.matches("customerPassword", encodedPassword)).thenReturn(true);
        when(jwtService.generateToken("customer@example.com", "CUSTOMER")).thenReturn(testToken);

        LoginRequest customerRequest = new LoginRequest();
        customerRequest.setEmail("customer@example.com");
        customerRequest.setPassword("customerPassword");

        String result = authService.authenticate(customerRequest);

        assertNotNull(result);
        assertEquals(testToken, result);

        verify(jwtService).generateToken("customer@example.com", "CUSTOMER");
    }

    @Test
    void authenticate_NullEmail_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPassword("password123");

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    void authenticate_NullPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword(null);

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    void authenticate_EmptyEmail_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("password123");

        when(userRepository.findByEmail("")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    void authenticate_EmptyPassword_ThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("", encodedPassword)).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(request);
        });
    }

    @Test
    void authenticate_PasswordEncoderReturnsFalse_ThrowsException() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(validLoginRequest);
        });

        verify(passwordEncoder).matches(anyString(), anyString());
    }

    @Test
    void authenticate_CaseSensitiveEmail_FailsForWrongCase() {
        when(userRepository.findByEmail("Test@Example.com")).thenReturn(Optional.empty());

        LoginRequest request = new LoginRequest();
        request.setEmail("Test@Example.com");
        request.setPassword("password123");

        assertThrows(RuntimeException.class, () -> {
            authService.authenticate(request);
        });
    }
}
