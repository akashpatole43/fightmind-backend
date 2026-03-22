package com.fightmind.chat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fightmind.chat.dto.ChatRequest;
import com.fightmind.auth.dto.RegisterRequest;
import com.fightmind.auth.AuthService;
import com.fightmind.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    // We mock the Python client so we don't actually hit FastAPI during tests
    @MockBean
    private PythonAiClient pythonAiClient;

    private String validJwtToken;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        // Register a test user to get a valid JWT
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setUsername("chattester");
        registerReq.setEmail("chat@example.com");
        registerReq.setPassword("Password123!");

        validJwtToken = authService.register(registerReq).getToken();
    }

    @Test
    void askQuestion_RequiresAuthentication() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setQuery("How do I throw a jab?");

        // No Authorization header -> Expect 403 Forbidden / 401 Unauthorized
        mockMvc.perform(post("/api/chat/send")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void askQuestion_SuccessWithMockedPython() throws Exception {
        // Prepare the mock Python response
        PythonAiClient.PythonResponse mockResponse = new PythonAiClient.PythonResponse();
        mockResponse.setAnswer("A jab is a fast, straight punch thrown with the lead hand.");
        mockResponse.setIntent("TECHNIQUE");
        mockResponse.setSport("Boxing");
        mockResponse.setConfidence(0.95);

        when(pythonAiClient.askPythonModel(any(PythonAiClient.PythonRequest.class)))
                .thenReturn(mockResponse);

        ChatRequest request = new ChatRequest();
        request.setQuery("How do I throw a perfect jab?");

        MvcResult mvcResult = mockMvc.perform(post("/api/chat/send")
                .header("Authorization", "Bearer " + validJwtToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                // We test async dispatch status
                .andReturn();

        // Using async behavior in MockMvc requires checking the async result
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("A jab is a fast, straight punch thrown with the lead hand."))
                .andExpect(jsonPath("$.sport").value("Boxing"))
                // Expect cached to equal false because it's generating fresh
                .andExpect(jsonPath("$.cached").value(false));

        // Verify that the mock Python service was actually called 
        verify(pythonAiClient, times(1)).askPythonModel(any());
    }
}
