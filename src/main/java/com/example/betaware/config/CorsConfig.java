package com.example.betaware.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Permitir requisições do frontend e do app móvel
        config.addAllowedOrigin("http://localhost:4200"); // Frontend Angular
        config.addAllowedOrigin("http://localhost:8081"); // Expo Dev Server
        config.addAllowedOrigin("exp://localhost:8081"); // Expo
        config.addAllowedOrigin("exp://192.168.0.*:8081"); // Expo na rede local
        config.addAllowedOrigin("exp://10.0.2.2:8081"); // Expo no emulador Android
        
        // Permitir todos os métodos HTTP
        config.addAllowedMethod("*");
        
        // Permitir todos os headers
        config.addAllowedHeader("*");
        
        // Permitir credenciais (cookies, headers de autenticação)
        config.setAllowCredentials(true);
        
        // Aplicar configuração para todas as rotas
        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
} 