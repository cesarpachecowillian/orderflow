package com.orderflow.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orderflow.auth.dto.LoginRequest;
import com.orderflow.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void register_then_login_flow() throws Exception {
        var reg = new RegisterRequest("César", "flow@email.com", "senha123");

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("flow@email.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        var login = new LoginRequest("flow@email.com", "senha123");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.type").value("Bearer"));
    }

    @Test
    void register_duplicateEmail_returns409() throws Exception {
        var reg = new RegisterRequest("A", "dup@email.com", "senha123");
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        var reg = new RegisterRequest("X", "wp@email.com", "senha123");
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        var login = new LoginRequest("wp@email.com", "errada");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_userNotFound_returns401() throws Exception {
        var login = new LoginRequest("ghost@email.com", "senha123");
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_invalidPayload_returns400() throws Exception {
        String bad = "{\"name\":\"\",\"email\":\"not-an-email\",\"password\":\"123\"}";
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bad))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.details").isArray());
    }
}
