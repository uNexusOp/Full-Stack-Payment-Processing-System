package com.payment.payment_processing.auth.controller;

import com.payment.payment_processing.auth.dto.LoginRequest;
import com.payment.payment_processing.auth.dto.LoginResponse;
import com.payment.payment_processing.auth.service.AuthService;
import com.payment.payment_processing.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        String token = authService.authenticate(request);
        return ResponseEntity.ok(new LoginResponse(token));
    }
}