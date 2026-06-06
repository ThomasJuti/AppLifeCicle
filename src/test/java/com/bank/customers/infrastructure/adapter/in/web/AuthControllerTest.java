package com.bank.customers.infrastructure.adapter.in.web;

import com.bank.customers.infrastructure.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AuthController Integration Tests")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    @Test
    @DisplayName("POST /api/auth/login - should return a Bearer token on valid credentials")
    void shouldReturnTokenOnValidLogin() throws Exception {
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken("admin", null, java.util.List.of()));
        when(jwtService.generateToken("admin")).thenReturn("signed.jwt.token");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        var body = Map.of("username", "admin", "password", "admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").value("signed.jwt.token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("POST /api/auth/login - should return 401 on invalid credentials")
    void shouldReturn401OnInvalidLogin() throws Exception {
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("Bad credentials"));

        var body = Map.of("username", "admin", "password", "wrong");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    @Test
    @DisplayName("POST /api/auth/login - should return 400 when username is blank")
    void shouldReturn400OnBlankUsername() throws Exception {
        var body = Map.of("username", "", "password", "admin123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Validation Error"));
    }
}
