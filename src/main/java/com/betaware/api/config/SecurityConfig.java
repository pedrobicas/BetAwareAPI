package com.betaware.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.header.writers.XXssProtectionHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Security Configuration for BetAware API
 * 
 * Implements comprehensive security controls following SSDLC practices:
 * - Authentication and authorization
 * - Security headers
 * - CORS configuration
 * - Session management
 * - Password encoding
 * - LGPD compliance controls
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Configure HTTP security with comprehensive security controls
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF for API endpoints (using JWT tokens)
            .csrf(csrf -> csrf.disable())
            
            // Configure CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // Configure session management
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                       .maximumSessions(1)
                       .maxSessionsPreventsLogin(false)
                       .sessionFixation().migrateSession()
            )
            
            // Configure authorization rules
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers("/api/health", "/api/auth/login", "/api/auth/register").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                
                // User endpoints
                .requestMatchers("/api/users/**").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/bets/**").hasAnyRole("USER", "ADMIN")
                
                // LGPD compliance endpoints
                .requestMatchers("/api/users/export-data").hasAnyRole("USER", "ADMIN")
                .requestMatchers("/api/users/delete-account").hasAnyRole("USER", "ADMIN")
                
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            
            // Configure security headers
            .headers(headers -> headers
                // X-Content-Type-Options
                .contentTypeOptions(contentType -> contentType.and())
                
                // X-Frame-Options
                .frameOptions(frame -> frame.deny())
                
                // X-XSS-Protection
                .addHeaderWriter(new XXssProtectionHeaderWriter())
                
                // Strict-Transport-Security (HSTS)
                .httpStrictTransportSecurity(hstsConfig -> hstsConfig
                    .maxAgeInSeconds(31536000) // 1 year
                    .includeSubdomains(true)
                    .preload(true)
                )
                
                // Content-Security-Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                                    "script-src 'self' 'unsafe-inline'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "img-src 'self' data: https:; " +
                                    "font-src 'self'; " +
                                    "connect-src 'self'; " +
                                    "frame-ancestors 'none'; " +
                                    "base-uri 'self'; " +
                                    "form-action 'self'")
                )
                
                // Referrer Policy
                .addHeaderWriter(new ReferrerPolicyHeaderWriter(
                    ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                
                // Permissions Policy (Feature Policy)
                .addHeaderWriter((request, response) -> {
                    response.setHeader("Permissions-Policy", 
                        "geolocation=(), " +
                        "microphone=(), " +
                        "camera=(), " +
                        "payment=(), " +
                        "usb=(), " +
                        "magnetometer=(), " +
                        "gyroscope=(), " +
                        "speaker=()");
                })
                
                // Cache Control for sensitive endpoints
                .cacheControl(cache -> cache.and())
            )
            
            // Configure exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                        "\"status\":401," +
                        "\"error\":\"Unauthorized\"," +
                        "\"message\":\"Authentication required\"," +
                        "\"path\":\"" + request.getRequestURI() + "\"}"
                    );
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(403);
                    response.setContentType("application/json");
                    response.getWriter().write(
                        "{\"timestamp\":\"" + java.time.Instant.now() + "\"," +
                        "\"status\":403," +
                        "\"error\":\"Forbidden\"," +
                        "\"message\":\"Access denied\"," +
                        "\"path\":\"" + request.getRequestURI() + "\"}"
                    );
                })
            );

        return http.build();
    }

    /**
     * Configure CORS for secure cross-origin requests
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Allowed origins (configure based on environment)
        configuration.setAllowedOriginPatterns(Arrays.asList(
            "http://localhost:3000",  // React development server
            "http://localhost:8080",  // Local API server
            "https://*.betaware.com", // Production domains
            "https://*.betaware.com.br"
        ));
        
        // Allowed methods
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"
        ));
        
        // Allowed headers
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", 
            "Content-Type", 
            "X-Requested-With",
            "Accept",
            "Origin",
            "Access-Control-Request-Method",
            "Access-Control-Request-Headers"
        ));
        
        // Exposed headers
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        
        // Allow credentials
        configuration.setAllowCredentials(true);
        
        // Max age for preflight requests
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    /**
     * Password encoder using BCrypt with strength 12
     * Higher strength for better security against brute force attacks
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Security event listener for audit logging
     */
    @Bean
    public SecurityEventListener securityEventListener() {
        return new SecurityEventListener();
    }
}

/**
 * Security Event Listener for LGPD compliance and audit logging
 */
class SecurityEventListener {
    
    private static final org.slf4j.Logger auditLogger = 
        org.slf4j.LoggerFactory.getLogger("SECURITY_AUDIT");
    
    @org.springframework.context.event.EventListener
    public void handleAuthenticationSuccess(
            org.springframework.security.authentication.event.AuthenticationSuccessEvent event) {
        
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress();
        
        auditLogger.info("Authentication successful - User: {}, IP: {}, Timestamp: {}", 
            username, ipAddress, java.time.Instant.now());
    }
    
    @org.springframework.context.event.EventListener
    public void handleAuthenticationFailure(
            org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent event) {
        
        String username = event.getAuthentication().getName();
        String ipAddress = getClientIpAddress();
        String reason = event.getException().getMessage();
        
        auditLogger.warn("Authentication failed - User: {}, IP: {}, Reason: {}, Timestamp: {}", 
            username, ipAddress, reason, java.time.Instant.now());
    }
    
    @org.springframework.context.event.EventListener
    public void handleAccessDenied(
            org.springframework.security.access.event.AuthorizationFailureEvent event) {
        
        String username = event.getAuthentication().getName();
        String resource = event.getSource().toString();
        String ipAddress = getClientIpAddress();
        
        auditLogger.warn("Access denied - User: {}, Resource: {}, IP: {}, Timestamp: {}", 
            username, resource, ipAddress, java.time.Instant.now());
    }
    
    private String getClientIpAddress() {
        try {
            org.springframework.web.context.request.RequestAttributes requestAttributes = 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            
            if (requestAttributes instanceof org.springframework.web.context.request.ServletRequestAttributes) {
                javax.servlet.http.HttpServletRequest request = 
                    ((org.springframework.web.context.request.ServletRequestAttributes) requestAttributes).getRequest();
                
                String xForwardedFor = request.getHeader("X-Forwarded-For");
                if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                    return xForwardedFor.split(",")[0].trim();
                }
                
                String xRealIp = request.getHeader("X-Real-IP");
                if (xRealIp != null && !xRealIp.isEmpty()) {
                    return xRealIp;
                }
                
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            auditLogger.debug("Could not determine client IP address", e);
        }
        
        return "unknown";
    }
}