package com.payment.payment_processing.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private String testSecretKey;
    private long testExpirationTime;

    @BeforeEach
    void setUp() {
        testSecretKey = "test-secret-key-for-jwt-generation-must-be-long-enough-for-hs256-algorithm";
        testExpirationTime = 3600000L; // 1 hour in milliseconds

        ReflectionTestUtils.setField(jwtService, "secretKey", testSecretKey);
        ReflectionTestUtils.setField(jwtService, "expirationTime", testExpirationTime);
    }

    @Test
    void generateToken_ValidInput_ReturnsValidToken() {
        String email = "test@example.com";
        String role = "CUSTOMER";

        String token = jwtService.generateToken(email, role);

        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.contains("."));
    }

    @Test
    void extractEmail_ValidToken_ReturnsCorrectEmail() {
        String email = "test@example.com";
        String role = "CUSTOMER";
        String token = jwtService.generateToken(email, role);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void extractRole_ValidToken_ReturnsCorrectRole() {
        String email = "test@example.com";
        String role = "ADMIN";
        String token = jwtService.generateToken(email, role);

        String extractedRole = jwtService.extractRole(token);

        assertEquals(role, extractedRole);
    }

    @Test
    void extractEmail_InvalidToken_ThrowsException() {
        String invalidToken = "invalid.token.string";

        assertThrows(Exception.class, () -> {
            jwtService.extractEmail(invalidToken);
        });
    }

    @Test
    void extractRole_InvalidToken_ThrowsException() {
        String invalidToken = "invalid.token.string";

        assertThrows(Exception.class, () -> {
            jwtService.extractRole(invalidToken);
        });
    }

    @Test
    void generateToken_DifferentEmails_GeneratesDifferentTokens() {
        String email1 = "user1@example.com";
        String email2 = "user2@example.com";
        String role = "CUSTOMER";

        String token1 = jwtService.generateToken(email1, role);
        String token2 = jwtService.generateToken(email2, role);

        assertNotEquals(token1, token2);
    }

    @Test
    void generateToken_DifferentRoles_GeneratesDifferentTokens() {
        String email = "test@example.com";
        String role1 = "CUSTOMER";
        String role2 = "ADMIN";

        String token1 = jwtService.generateToken(email, role1);
        String token2 = jwtService.generateToken(email, role2);

        assertNotEquals(token1, token2);
    }

    @Test
    void extractEmail_ExtractsFromGeneratedTokenSuccessfully() {
        String email = "admin@example.com";
        String role = "ADMIN";
        String token = jwtService.generateToken(email, role);

        String extractedEmail = jwtService.extractEmail(token);
        String extractedRole = jwtService.extractRole(token);

        assertEquals(email, extractedEmail);
        assertEquals(role, extractedRole);
    }

    @Test
    void generateToken_NullEmail_GeneratesToken() {
        String token = jwtService.generateToken(null, "CUSTOMER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_NullRole_GeneratesToken() {
        String token = jwtService.generateToken("test@example.com", null);

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_EmptyEmail_GeneratesToken() {
        String token = jwtService.generateToken("", "CUSTOMER");

        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void generateToken_SpecialCharactersInEmail_GeneratesValidToken() {
        String email = "user+test@example.com";
        String role = "CUSTOMER";
        String token = jwtService.generateToken(email, role);

        String extractedEmail = jwtService.extractEmail(token);

        assertEquals(email, extractedEmail);
    }

    @Test
    void extractEmail_TamperedToken_ThrowsException() {
        String email = "test@example.com";
        String role = "CUSTOMER";
        String token = jwtService.generateToken(email, role);

        String tamperedToken = token + "tampered";

        assertThrows(Exception.class, () -> {
            jwtService.extractEmail(tamperedToken);
        });
    }
}
