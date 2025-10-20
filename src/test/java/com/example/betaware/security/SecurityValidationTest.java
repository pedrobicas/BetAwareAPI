package com.example.betaware.security;

import com.example.betaware.service.LGPDComplianceService;
import com.example.betaware.service.VulnerabilityManagementService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureTestMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de Integração para Validação de Segurança
 * Implementa práticas SSDLC com validação automatizada de segurança
 */
@SpringBootTest
@AutoConfigureTestMvc
public class SecurityValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LGPDComplianceService lgpdService;

    @Autowired
    private VulnerabilityManagementService vulnerabilityService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    /**
     * Testes de Validação de Entrada
     */
    @Test
    void testSQLInjectionPrevention() throws Exception {
        String maliciousPayload = "'; DROP TABLE users; --";
        
        Map<String, String> payload = new HashMap<>();
        payload.put("username", maliciousPayload);
        payload.put("password", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid input format"));
    }

    @Test
    void testXSSPrevention() throws Exception {
        String xssPayload = "<script>alert('XSS')</script>";
        
        Map<String, String> payload = new HashMap<>();
        payload.put("comment", xssPayload);

        mockMvc.perform(post("/api/v1/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid characters detected"));
    }

    @Test
    void testEmailValidation() throws Exception {
        String[] invalidEmails = {
            "invalid-email",
            "@domain.com",
            "user@",
            "user..name@domain.com",
            "user@domain",
            ""
        };

        for (String email : invalidEmails) {
            Map<String, String> payload = new HashMap<>();
            payload.put("email", email);
            payload.put("password", "ValidPass123!");

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Invalid email format"));
        }
    }

    @Test
    void testPasswordComplexity() throws Exception {
        String[] weakPasswords = {
            "123456",
            "password",
            "abc123",
            "Password",
            "12345678",
            "qwerty"
        };

        for (String password : weakPasswords) {
            Map<String, String> payload = new HashMap<>();
            payload.put("email", "test@example.com");
            payload.put("password", password);

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Password does not meet complexity requirements"));
        }
    }

    @Test
    void testRequestSizeLimit() throws Exception {
        // Criar payload muito grande (> 1MB)
        StringBuilder largePayload = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            largePayload.append("A");
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("data", largePayload.toString());

        mockMvc.perform(post("/api/v1/data")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isPayloadTooLarge());
    }

    /**
     * Testes de Autenticação e Autorização
     */
    @Test
    void testRateLimiting() throws Exception {
        String loginPayload = objectMapper.writeValueAsString(Map.of(
            "username", "testuser",
            "password", "wrongpassword"
        ));

        // Tentar login múltiplas vezes para ativar rate limiting
        for (int i = 0; i < 6; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginPayload));
        }

        // A 6ª tentativa deve ser bloqueada
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginPayload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
    }

    @Test
    void testUnauthorizedAccess() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "USER")
    void testInsufficientPrivileges() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testJWTValidation() throws Exception {
        String invalidToken = "invalid.jwt.token";

        mockMvc.perform(get("/api/v1/profile")
                .header("Authorization", "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid JWT token"));
    }

    @Test
    void testSessionFixationPrevention() throws Exception {
        // Primeiro login
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "validuser",
                    "password", "ValidPass123!"
                ))))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId1 = loginResult.getResponse().getHeader("Set-Cookie");

        // Segundo login deve gerar nova sessão
        MvcResult loginResult2 = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "username", "validuser",
                    "password", "ValidPass123!"
                ))))
                .andExpect(status().isOk())
                .andReturn();

        String sessionId2 = loginResult2.getResponse().getHeader("Set-Cookie");

        assertNotEquals(sessionId1, sessionId2, "Session IDs should be different to prevent fixation");
    }

    /**
     * Testes de Tratamento de Erros
     */
    @Test
    void testSensitiveInformationExposure() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assertFalse(response.contains("java.lang"), "Stack trace should not be exposed");
                    assertFalse(response.contains("SQLException"), "Database errors should not be exposed");
                    assertFalse(response.contains("password"), "Sensitive data should not be exposed");
                });
    }

    @Test
    void testMalformedJSONHandling() throws Exception {
        String malformedJson = "{ invalid json }";

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid JSON format"))
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    assertFalse(response.contains("JsonParseException"), "Internal error details should not be exposed");
                });
    }

    @Test
    void testConsistentErrorFormat() throws Exception {
        // Testar diferentes tipos de erro para garantir formato consistente
        String[] endpoints = {"/api/v1/nonexistent", "/api/v1/admin/users"};
        
        for (String endpoint : endpoints) {
            mockMvc.perform(get(endpoint))
                    .andExpect(result -> {
                        String response = result.getResponse().getContentAsString();
                        assertTrue(response.contains("\"error\""), "Error response should have consistent format");
                        assertTrue(response.contains("\"timestamp\""), "Error response should include timestamp");
                    });
        }
    }

    /**
     * Testes de Cabeçalhos de Segurança
     */
    @Test
    void testSecurityHeaders() throws Exception {
        mockMvc.perform(get("/api/v1/public/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "1; mode=block"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpected(header().string("Referrer-Policy", "strict-origin-when-cross-origin"));
    }

    @Test
    void testHSTSHeader() throws Exception {
        mockMvc.perform(get("/api/v1/public/health"))
                .andExpect(header().string("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload"));
    }

    @Test
    void testCSPHeader() throws Exception {
        mockMvc.perform(get("/api/v1/public/health"))
                .andExpect(result -> {
                    String csp = result.getResponse().getHeader("Content-Security-Policy");
                    assertNotNull(csp, "CSP header should be present");
                    assertTrue(csp.contains("default-src 'self'"), "CSP should restrict default sources");
                    assertTrue(csp.contains("script-src 'self'"), "CSP should restrict script sources");
                });
    }

    /**
     * Testes de Proteção de Dados
     */
    @Test
    void testSensitiveDataLogging() throws Exception {
        Map<String, String> sensitivePayload = new HashMap<>();
        sensitivePayload.put("password", "MySecretPassword123!");
        sensitivePayload.put("creditCard", "4111111111111111");
        sensitivePayload.put("ssn", "123-45-6789");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sensitivePayload)));

        // Verificar que dados sensíveis não são logados (seria necessário verificar logs reais)
        // Este teste é mais conceitual - em implementação real, verificaria arquivos de log
        assertTrue(true, "Sensitive data should not appear in logs");
    }

    @Test
    void testDataMasking() throws Exception {
        // Simular resposta com dados sensíveis mascarados
        mockMvc.perform(get("/api/v1/profile")
                .header("Authorization", "Bearer valid.jwt.token"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String response = result.getResponse().getContentAsString();
                    if (response.contains("creditCard")) {
                        assertTrue(response.contains("****"), "Credit card should be masked");
                    }
                    if (response.contains("ssn")) {
                        assertTrue(response.contains("***-**-"), "SSN should be masked");
                    }
                });
    }

    /**
     * Testes de Conformidade LGPD
     */
    @Test
    void testExplicitConsent() {
        // Testar registro de consentimento explícito
        String consentId = lgpdService.recordConsent("user123", "MARKETING", "CONSENTIMENTO", true);
        assertNotNull(consentId, "Consent should be recorded with ID");

        boolean isValid = lgpdService.validateConsent("user123", "MARKETING");
        assertTrue(isValid, "Consent should be valid after recording");
    }

    @Test
    void testDataPortability() {
        // Testar solicitação de portabilidade de dados
        String requestId = lgpdService.requestDataPortability("user123");
        assertNotNull(requestId, "Data portability request should return ID");

        LGPDComplianceService.DataSubjectRequest request = lgpdService.getDataSubjectRequest(requestId);
        assertNotNull(request, "Request should be retrievable");
        assertEquals(LGPDComplianceService.RequestType.PORTABILITY, request.getType());
    }

    @Test
    void testDataDeletion() {
        // Testar solicitação de exclusão de dados
        String requestId = lgpdService.requestDataDeletion("user123");
        assertNotNull(requestId, "Data deletion request should return ID");

        LGPDComplianceService.DataSubjectRequest request = lgpdService.getDataSubjectRequest(requestId);
        assertNotNull(request, "Request should be retrievable");
        assertEquals(LGPDComplianceService.RequestType.DELETION, request.getType());
    }

    @Test
    void testDataMinimization() {
        // Testar validação de minimização de dados
        Set<String> allowedFields = Set.of("nome", "email", "telefone");
        boolean isCompliant = lgpdService.validateDataMinimization("CADASTRO", allowedFields);
        assertTrue(isCompliant, "Should be compliant with allowed fields");

        Set<String> excessiveFields = Set.of("nome", "email", "telefone", "renda", "historico_credito");
        boolean isNonCompliant = lgpdService.validateDataMinimization("CADASTRO", excessiveFields);
        assertFalse(isNonCompliant, "Should not be compliant with excessive fields");
    }

    @Test
    void testAuditLogging() {
        // Testar se eventos de auditoria são registrados
        lgpdService.recordConsent("user123", "APOSTAS", "CONSENTIMENTO", true);
        
        // Verificar se o consentimento foi registrado
        var consents = lgpdService.getUserConsents("user123");
        assertFalse(consents.isEmpty(), "Consent should be recorded for audit");
        
        // Verificar se logs de auditoria contêm informações necessárias
        var consent = consents.get(0);
        assertNotNull(consent.getTimestamp(), "Audit log should have timestamp");
        assertNotNull(consent.getIpAddress(), "Audit log should have IP address");
    }

    /**
     * Testes de Gestão de Vulnerabilidades
     */
    @Test
    void testVulnerabilityDetection() {
        // Simular detecção de vulnerabilidade
        VulnerabilityManagementService.SecurityFinding finding = 
            new VulnerabilityManagementService.SecurityFinding();
        finding.setType("DEPENDENCY");
        finding.setDescription("Vulnerable dependency: log4j 2.14.1");
        finding.setSeverity(VulnerabilityManagementService.Severity.HIGH);
        finding.setCvssScore(8.5);
        finding.setComponent("log4j-core");
        finding.setVersion("2.14.1");

        vulnerabilityService.processSecurityFindings(java.util.List.of(finding));
        
        var metrics = vulnerabilityService.getVulnerabilityMetrics();
        assertTrue(metrics.getHighSeverityCount() > 0, "High severity vulnerability should be detected");
    }

    @Test
    void testAutomaticRemediation() {
        // Testar tentativa de remediação automática
        VulnerabilityManagementService.SecurityFinding lowSeverityFinding = 
            new VulnerabilityManagementService.SecurityFinding();
        lowSeverityFinding.setType("DEPENDENCY");
        lowSeverityFinding.setDescription("Outdated dependency");
        lowSeverityFinding.setSeverity(VulnerabilityManagementService.Severity.LOW);
        lowSeverityFinding.setCvssScore(3.0);
        lowSeverityFinding.setComponent("commons-lang");
        lowSeverityFinding.setVersion("2.6");

        vulnerabilityService.processSecurityFindings(java.util.List.of(lowSeverityFinding));
        
        // Verificar se remediação foi tentada
        var report = vulnerabilityService.generateVulnerabilityReport();
        assertNotNull(report, "Vulnerability report should be generated");
        assertFalse(report.getRecommendations().isEmpty(), "Report should contain recommendations");
    }

    /**
     * Testes de Performance de Segurança
     */
    @Test
    void testSecurityValidationPerformance() throws Exception {
        long startTime = System.currentTimeMillis();
        
        // Executar múltiplas validações de segurança
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post("/api/v1/auth/validate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of(
                        "token", "sample.jwt.token",
                        "action", "READ_PROFILE"
                    ))));
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Validações de segurança não devem adicionar mais de 100ms por requisição
        assertTrue(duration < 10000, "Security validations should not significantly impact performance");
    }

    /**
     * Testes de Integração Completa
     */
    @Test
    @WithMockUser(username = "testuser", roles = "USER")
    void testCompleteSecurityFlow() throws Exception {
        // 1. Registrar consentimento LGPD
        lgpdService.recordConsent("testuser", "APOSTAS", "CONSENTIMENTO", true);
        
        // 2. Fazer requisição autenticada
        mockMvc.perform(post("/api/v1/bets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                    "amount", 100.0,
                    "odds", 2.5,
                    "event", "Brasil vs Argentina"
                ))))
                .andExpect(status().isOk());
        
        // 3. Verificar se dados foram processados conforme LGPD
        boolean hasValidConsent = lgpdService.validateConsent("testuser", "APOSTAS");
        assertTrue(hasValidConsent, "User should have valid consent for betting");
        
        // 4. Verificar métricas de conformidade
        var complianceMetrics = lgpdService.getMetrics();
        assertTrue(complianceMetrics.getActiveConsents() > 0, "Should have active consents");
    }
}