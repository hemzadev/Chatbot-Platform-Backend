package com.hmzadev.interactivechatbot;

import com.hmzadev.interactivechatbot.services.DatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class InteractiveChatbotApplication implements CommandLineRunner {

    @Autowired
    private DatabaseService databaseService;
    public static void main(String[] args) {
        SpringApplication.run(InteractiveChatbotApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        String query = "SELECT * FROM data";
        List<Map<String, Object>> results = databaseService.executeQuery(query);
        for (Map<String, Object> row : results) {
            System.out.println(row);
        }
    }
}
