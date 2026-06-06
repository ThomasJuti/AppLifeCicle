package com.bank.customers.infrastructure.adapter.in.web;

import com.bank.customers.application.dto.LoginRequest;
import com.bank.customers.application.dto.LoginResponse;
import com.bank.customers.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "JWT authentication operations")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    @Operation(
        summary = "Authenticate and obtain a JWT",
        description = "Validates credentials and returns a signed Bearer token.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
        }
    )
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        String token = jwtService.generateToken(authentication.getName());
        return LoginResponse.bearer(token, jwtService.getExpirationSeconds(), authentication.getName());
    }

    @GetMapping("/me")
    @Operation(
        summary = "Current authenticated user",
        description = "Returns the username extracted from the Bearer token.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Authenticated"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token")
        }
    )
    public Map<String, String> me(@AuthenticationPrincipal UserDetails user) {
        return Map.of("username", user.getUsername());
    }
}
