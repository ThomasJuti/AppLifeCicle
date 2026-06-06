package com.bank.customers.application.dto;

public record LoginResponse(
    String token,
    String tokenType,
    long expiresIn,
    String username
) {
    public static LoginResponse bearer(String token, long expiresIn, String username) {
        return new LoginResponse(token, "Bearer", expiresIn, username);
    }
}
