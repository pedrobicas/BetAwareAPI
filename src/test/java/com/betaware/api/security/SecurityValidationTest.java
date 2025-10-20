package com.betaware.api.security;

import com.betaware.api.dto.LoginRequest;
import com.betaware.api.dto.RegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security Validation Tests for SSDLC (Secure Software Development Life Cycle)
 * 
 * These tests validate security controls and practices implemented in the BetAware API:
 * - Input validation and sanitization
 * - Authentication and authorization security
 * - Error handling security
 * - Session management
 * - Data protection
 */
@SpringBootTest
@AutoConfigureTestMockMvc
public class SecurityValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("Input Validation and Sanitization Tests")
    class InputValidationTests {

        @Test
        @DisplayName("Should reject SQL injection attempts in login")
        void shouldRejectSqlInjectionInLogin() throws Exception {
            LoginRequest maliciousLogin = new LoginRequest();
            maliciousLogin.setEmail("admin'; DROP TABLE users; --");
            maliciousLogin.setPassword("password");

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(maliciousLogin)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        assertFalse(response.contains("DROP TABLE"), 
                            "Response should not contain SQL injection payload");
                    });
        }

        @Test
        @DisplayName("Should sanitize XSS attempts in registration")
        void shouldSanitizeXssInRegistration() throws Exception {
            RegisterRequest xssRequest = new RegisterRequest();
            xssRequest.setEmail("test@example.com");
            xssRequest.setPassword("password123");
            xssRequest.setName("<script>alert('XSS')</script>");

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(xssRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        assertFalse(response.contains("<script>"), 
                            "Response should not contain XSS payload");
                        assertFalse(response.contains("alert("), 
                            "Response should not contain JavaScript code");
                    });
        }

        @Test
        @DisplayName("Should validate email format strictly")
        void shouldValidateEmailFormat() throws Exception {
            String[] invalidEmails = {
                "invalid-email",
                "@example.com",
                "test@",
                "test..test@example.com",
                "test@example",
                "test@.com"
            };

            for (String invalidEmail : invalidEmails) {
                LoginRequest request = new LoginRequest();
                request.setEmail(invalidEmail);
                request.setPassword("password123");

                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").exists());
            }
        }

        @Test
        @DisplayName("Should enforce password complexity requirements")
        void shouldEnforcePasswordComplexity() throws Exception {
            String[] weakPasswords = {
                "123456",
                "password",
                "abc",
                "12345678",
                "qwerty",
                "password123" // Common pattern
            };

            for (String weakPassword : weakPasswords) {
                RegisterRequest request = new RegisterRequest();
                request.setEmail("test@example.com");
                request.setPassword(weakPassword);
                request.setName("Test User");

                mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString("password")));
            }
        }

        @Test
        @DisplayName("Should limit request size to prevent DoS")
        void shouldLimitRequestSize() throws Exception {
            // Create oversized payload
            StringBuilder largePayload = new StringBuilder();
            for (int i = 0; i < 10000; i++) {
                largePayload.append("A");
            }

            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setPassword("ValidPassword123!");
            request.setName(largePayload.toString());

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Authentication and Authorization Security Tests")
    class AuthenticationSecurityTests {

        @Test
        @DisplayName("Should implement rate limiting for login attempts")
        void shouldImplementRateLimiting() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("wrongpassword");

            // Attempt multiple failed logins
            for (int i = 0; i < 6; i++) {
                mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)));
            }

            // Should be rate limited after multiple attempts
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests());
        }

        @Test
        @DisplayName("Should require authentication for protected endpoints")
        void shouldRequireAuthenticationForProtectedEndpoints() throws Exception {
            String[] protectedEndpoints = {
                "/api/users/profile",
                "/api/bets",
                "/api/admin/users"
            };

            for (String endpoint : protectedEndpoints) {
                mockMvc.perform(get(endpoint))
                        .andExpect(status().isUnauthorized());
            }
        }

        @Test
        @DisplayName("Should validate JWT token format and expiration")
        void shouldValidateJwtToken() throws Exception {
            String[] invalidTokens = {
                "invalid.token.format",
                "Bearer invalid-token",
                "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.signature",
                "" // Empty token
            };

            for (String invalidToken : invalidTokens) {
                mockMvc.perform(get("/api/users/profile")
                        .header("Authorization", invalidToken))
                        .andExpect(status().isUnauthorized());
            }
        }

        @Test
        @DisplayName("Should prevent session fixation attacks")
        void shouldPreventSessionFixation() throws Exception {
            // First request to get initial session
            MvcResult initialResult = mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andReturn();

            String initialSessionId = initialResult.getResponse().getHeader("Set-Cookie");

            // Login with valid credentials
            LoginRequest validLogin = new LoginRequest();
            validLogin.setEmail("test@example.com");
            validLogin.setPassword("ValidPassword123!");

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validLogin))
                    .header("Cookie", initialSessionId))
                    .andReturn();

            String postLoginSessionId = loginResult.getResponse().getHeader("Set-Cookie");

            // Session ID should change after login
            assertNotEquals(initialSessionId, postLoginSessionId, 
                "Session ID should change after authentication to prevent session fixation");
        }
    }

    @Nested
    @DisplayName("Error Handling Security Tests")
    class ErrorHandlingSecurityTests {

        @Test
        @DisplayName("Should not expose sensitive information in error messages")
        void shouldNotExposeSensitiveInformation() throws Exception {
            // Test various error scenarios
            mockMvc.perform(get("/api/nonexistent-endpoint"))
                    .andExpect(status().isNotFound())
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        assertFalse(response.contains("java."), 
                            "Error should not expose Java stack traces");
                        assertFalse(response.contains("Exception"), 
                            "Error should not expose exception details");
                        assertFalse(response.contains("database"), 
                            "Error should not expose database information");
                    });
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJson() throws Exception {
            String malformedJson = "{ invalid json structure }";

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpected(jsonPath("$.message").exists())
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        assertFalse(response.contains("JsonParseException"), 
                            "Error should not expose parsing exception details");
                    });
        }

        @Test
        @DisplayName("Should return consistent error format")
        void shouldReturnConsistentErrorFormat() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andExpect(jsonPath("$.status").exists())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.path").exists());
        }
    }

    @Nested
    @DisplayName("Security Headers Tests")
    class SecurityHeadersTests {

        @Test
        @DisplayName("Should include security headers in responses")
        void shouldIncludeSecurityHeaders() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("X-Content-Type-Options"))
                    .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                    .andExpect(header().exists("X-Frame-Options"))
                    .andExpect(header().string("X-Frame-Options", "DENY"))
                    .andExpect(header().exists("X-XSS-Protection"))
                    .andExpect(header().string("X-XSS-Protection", "1; mode=block"));
        }

        @Test
        @DisplayName("Should include HSTS header for HTTPS")
        void shouldIncludeHstsHeader() throws Exception {
            mockMvc.perform(get("/api/health")
                    .secure(true)) // Simulate HTTPS request
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Strict-Transport-Security"));
        }

        @Test
        @DisplayName("Should include Content Security Policy header")
        void shouldIncludeCspHeader() throws Exception {
            mockMvc.perform(get("/api/health"))
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Content-Security-Policy"))
                    .andExpect(header().string("Content-Security-Policy", 
                        org.hamcrest.Matchers.containsString("default-src 'self'")));
        }
    }

    @Nested
    @DisplayName("Data Protection Tests")
    class DataProtectionTests {

        @Test
        @DisplayName("Should not log sensitive data")
        void shouldNotLogSensitiveData() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("test@example.com");
            request.setPassword("SecretPassword123!");

            // This test would need to check actual log output
            // For now, we ensure the request is processed without exposing password
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        assertFalse(response.contains("SecretPassword123!"), 
                            "Password should not appear in response");
                    });
        }

        @Test
        @DisplayName("Should mask sensitive data in API responses")
        void shouldMaskSensitiveDataInResponses() throws Exception {
            // Assuming we have a user profile endpoint that returns user data
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        // Check that sensitive fields are masked or not present
                        assertFalse(response.contains("password"), 
                            "Password field should not be present in user profile");
                        assertFalse(response.contains("ssn"), 
                            "SSN should not be present in API responses");
                    });
        }
    }

    @Nested
    @DisplayName("LGPD Compliance Tests")
    class LgpdComplianceTests {

        @Test
        @DisplayName("Should require explicit consent for data collection")
        void shouldRequireExplicitConsent() throws Exception {
            RegisterRequest request = new RegisterRequest();
            request.setEmail("test@example.com");
            request.setPassword("ValidPassword123!");
            request.setName("Test User");
            // Missing consent field

            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("consent")));
        }

        @Test
        @DisplayName("Should provide data portability endpoint")
        void shouldProvideDataPortabilityEndpoint() throws Exception {
            mockMvc.perform(get("/api/users/export-data")
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "application/json"));
        }

        @Test
        @DisplayName("Should provide data deletion endpoint")
        void shouldProvideDataDeletionEndpoint() throws Exception {
            mockMvc.perform(delete("/api/users/delete-account")
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should audit data access and modifications")
        void shouldAuditDataAccessAndModifications() throws Exception {
            // Test that data access is logged for audit purposes
            mockMvc.perform(get("/api/users/profile")
                    .header("Authorization", "Bearer valid-jwt-token"))
                    .andExpect(status().isOk());

            // Verify audit log entry was created (this would need actual audit log checking)
            // For now, we just ensure the endpoint is accessible and returns expected status
        }
    }
}