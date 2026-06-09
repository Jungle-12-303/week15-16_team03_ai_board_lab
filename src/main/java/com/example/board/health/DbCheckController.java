package com.example.board.health;

import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DbCheckController {

    private final JdbcTemplate jdbcTemplate;

    public DbCheckController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // 개발용 DB 연결 확인 API
    @GetMapping("/db-check")
    public Map<String, String> dbCheck() {
        String version = jdbcTemplate.queryForObject("select version()", String.class);

        return Map.of(
                "status", "connected",
                "database", "PostgreSQL",
                "version", version
        );
    }
}
