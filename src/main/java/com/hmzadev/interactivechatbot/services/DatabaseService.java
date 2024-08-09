package com.hmzadev.interactivechatbot.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

@Service
public class DatabaseService {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> executeQuery(String query) {
        if (query.trim().toUpperCase().startsWith("SELECT")) {
            return jdbcTemplate.queryForList(query);
        } else {
            throw new IllegalArgumentException("Query must be a SELECT statement");
        }
    }

    public int executeUpdate(String query) {
        return jdbcTemplate.update(query);
    }
}
