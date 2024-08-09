package com.hmzadev.interactivechatbot.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/chat")
public class ChatRestController {
    private final RestTemplate restTemplate;

    public ChatRestController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/ask")
    public String askQuestion(@RequestParam String question) {
        String payload = "{\"message\":\"" + question + "\"}";
        return restTemplate.postForObject("http://localhost:5055/webhook", payload, String.class);
    }
}