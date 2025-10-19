package com.example.betaware.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndApostaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void fluxoCompleto_login_criaAposta_e_listaApostas() throws Exception {
        // Login
        String loginJson = "{\"username\":\"usuario1\",\"senha\":\"senha123\"}";
        String loginResponse = mockMvc.perform(post("/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode loginNode = objectMapper.readTree(loginResponse);
        String token = loginNode.get("token").asText();
        assertThat(token).isNotBlank();

        // Criar aposta
        String dataIso = LocalDateTime.now().withNano(0).toString();
        String apostaJson = String.format("{\"categoria\":\"Futebol\",\"jogo\":\"Teste FC x Demo FC\",\"valor\":100.0,\"resultado\":\"PENDENTE\",\"data\":\"%s\"}", dataIso);

        mockMvc.perform(post("/v1/apostas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(apostaJson))
                .andExpect(status().isOk());

        // Listar apostas do usuário
        String listResponse = mockMvc.perform(get("/v1/apostas")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode listNode = objectMapper.readTree(listResponse);
        assertThat(listNode.isArray()).isTrue();
        assertThat(listNode.size()).isGreaterThan(0);
    }

    @Test
    void healthEndpoint_deveResponderOk() throws Exception {
        String response = mockMvc.perform(get("/v1/health"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(response).contains("API está online");
    }
}