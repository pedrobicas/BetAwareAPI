package com.example.betaware.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Profile("dev")
public class FlywayCleanConfig {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void cleanFlywayHistory() {
        try {
            jdbcTemplate.execute("DROP ALL OBJECTS");
            jdbcTemplate.execute("DROP TABLE IF EXISTS flyway_schema_history");
        } catch (Exception e) {
        }
    }
}
