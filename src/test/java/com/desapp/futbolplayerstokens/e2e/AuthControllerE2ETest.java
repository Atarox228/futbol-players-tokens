package com.desapp.futbolplayerstokens.e2e;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerE2ETest extends AbstractE2ETest {

    @Test
    void registerAndLogin() throws Exception {
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "username", "e2euser",
                "password", "password123",
                "email", "e2e@example.com"
        ));
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        String loginBody = objectMapper.writeValueAsString(Map.of(
                "username", "e2euser",
                "password", "password123"
        ));
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.userId").isNumber());
    }

    @Test
    void register_duplicateUser_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "dupeuser",
                "password", "password123",
                "email", "dupe@example.com"
        ));
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON).content(body));
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_invalidCredentials_returnsBadRequest() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "nonexistent",
                "password", "wrong"
        ));
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_returnsSuccess() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful"));
    }
}
