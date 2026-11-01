
package com.paypulse.payment.auth;

import com.paypulse.payment.merchant.entity.Merchant;
import com.paypulse.payment.merchant.repository.MerchantRepository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JsonMapper jsonMapper;

    private final List<String> createdEmails = new ArrayList<>();

    private String newEmail() {
        String email = "test-" + UUID.randomUUID() + "@example.com";
        createdEmails.add(email);
        return email;
    }

    private String registerBody(String email) {
        return """
                {
                  "name": "Test Merchant",
                  "email": "%s",
                  "password": "DemoPass#123"
                }
                """.formatted(email);
    }

    private String loginBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password);
    }

    private String registerAndLogin(String email) throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginBody(email, "DemoPass#123"))
                )
                .andExpect(status().isOk())
                .andReturn();

        return jsonMapper
                .readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    @AfterEach
    void cleanup() {
        for (String email : createdEmails) {
            merchantRepository.findByEmail(email)
                    .ifPresent(merchantRepository::delete);
        }
    }

    @Test
    void shouldRegisterAndStoreHashedPassword() throws Exception {

        String email = newEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("MERCHANT"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Merchant merchant = merchantRepository
                .findByEmail(email)
                .orElseThrow();

        assertNotEquals("DemoPass#123", merchant.getPasswordHash());

        assertTrue(passwordEncoder.matches(
                "DemoPass#123",
                merchant.getPasswordHash()
        ));
    }

    @Test
    void shouldRejectDuplicateEmail() throws Exception {

        String email = newEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRejectIncorrectPassword() throws Exception {

        String email = newEmail();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody(email, "WrongPassword123")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldLoginAndAccessOwnProfile() throws Exception {

        String email = newEmail();

        String token = registerAndLogin(email);

        mockMvc.perform(get("/api/merchants/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("MERCHANT"));
    }

    @Test
    void shouldEnforceAuthenticationAndAdminRole() throws Exception {

        String email = newEmail();
        String token = registerAndLogin(email);

        mockMvc.perform(get("/api/merchants/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/merchants/me")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/admin/merchants/count")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectInvalidRegistration() throws Exception {

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "invalid-email",
                                  "password": "123"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
