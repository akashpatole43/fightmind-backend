package com.fightmind.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fightmind.auth.dto.LoginRequest;
import com.fightmind.auth.dto.RegisterRequest;
import com.fightmind.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Test for AuthController.
 * Ensures the register/login endpoints work properly with DB and JWT issuance.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Uses test properties if defined (we don't strictly need it to run against local DB)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDb() {
        // Clear the users table (except for the admin inserted by Flyway/Seeder if needed,
        // but since we are doing integration test we should make sure our test emails don't conflict)
        userRepository.deleteAll();
    }

    @Test
    void registerAndLogin_Success() throws Exception {
        
        // 1. Register a new user
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("testuser");
        registerReq.setEmail("test@example.com");
        registerReq.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        // 2. Login with the same user
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("test@example.com");
        loginReq.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void register_FailsWhenEmailMissing() throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("testuser");
        // Missing email
        registerReq.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.email").value("Email is required"));
    }

    @Test
    void login_FailsWithBadPassword() throws Exception {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail("fake@example.com");
        loginReq.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
