package com.fightmind.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fightmind.auth.AuthService;
import com.fightmind.auth.dto.RegisterRequest;
import com.fightmind.chat.ChatMessage;
import com.fightmind.chat.ChatMessageRepository;
import com.fightmind.user.User;
import com.fightmind.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatMessageRepository chatRepository;

    @Autowired
    private AuthService authService;

    private String userToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        chatRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create a standard user
        RegisterRequest userReq = new RegisterRequest();
        userReq.setUsername("normaluser");
        userReq.setEmail("user@example.com");
        userReq.setPassword("Password123!");
        userToken = authService.register(userReq).getToken();

        // 2. Create an admin user (normally seeded by Flyway, but we do it manually in tests)
        RegisterRequest adminReq = new RegisterRequest();
        adminReq.setUsername("theadmin");
        adminReq.setEmail("admin@example.com");
        adminReq.setPassword("Password123!");
        adminToken = authService.register(adminReq).getToken();
        
        // Elevate the admin manually in the database
        User adminUser = userRepository.findByEmail("admin@example.com").orElseThrow();
        adminUser.setRole("ROLE_ADMIN");
        userRepository.save(adminUser);

        // Regenerate token so it actually has the ROLE_ADMIN claim
        adminToken = authService.login(new com.fightmind.auth.dto.LoginRequest() {{
            setEmail("admin@example.com");
            setPassword("Password123!");
        }}).getToken();
    }

    @Test
    void normalUser_isForbiddenFromAdminStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + userToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminUser_canAccessAdminStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(2))
                .andExpect(jsonPath("$.totalMessages").value(0));
    }

    @Test
    void unauthenticatedUser_isUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
