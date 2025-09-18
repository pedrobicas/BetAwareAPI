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

        config.addAllowedOrigin("http://localhost:4200");
        config.addAllowedOrigin("http://localhost:8081");
        config.addAllowedOrigin("exp://localhost:8081");
        config.addAllowedOrigin("exp://192.168.0.*:8081");
        config.addAllowedOrigin("exp://10.0.2.2:8081");

        config.addAllowedMethod("*");

        config.addAllowedHeader("*");

        config.setAllowCredentials(true);

        source.registerCorsConfiguration("/**", config);
        
        return new CorsFilter(source);
    }
} 