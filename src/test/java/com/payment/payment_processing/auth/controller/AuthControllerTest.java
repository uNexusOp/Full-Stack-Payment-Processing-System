package com.payment.payment_processing.auth.controller;

import com.payment.payment_processing.auth.dto.LoginRequest;
import com.payment.payment_processing.auth.dto.LoginResponse;
import com.payment.payment_processing.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private LoginRequest validLoginRequest;
    private String validToken;

    @BeforeEach
    void setUp() {
        validLoginRequest = new LoginRequest();
        validLoginRequest.setEmail("test@example.com");
        validLoginRequest.setPassword("password123");

        validToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test.token";
    }

    @Test
    void login_Success() {
        when(authService.authenticate(any(LoginRequest.class))).thenReturn(validToken);

        ResponseEntity<LoginResponse> response = authController.login(validLoginRequest);

        assertNotNull(response);
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertNotNull(response.getBody());
        assertEquals(validToken, response.getBody().getToken());

        verify(authService).authenticate(any(LoginRequest.class));
    }

    @Test
    void login_InvalidCredentials_ThrowsException() {
        when(authService.authenticate(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid email or password"));

        assertThrows(RuntimeException.class, () -> {
            authController.login(validLoginRequest);
        });

        verify(authService).authenticate(any(LoginRequest.class));
    }
}
