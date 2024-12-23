package com.hmzadev.interactivechatbot.web;

import com.hmzadev.interactivechatbot.dao.ChatSession;
import com.hmzadev.interactivechatbot.dao.Message;
import com.hmzadev.interactivechatbot.dao.SenderType;
import com.hmzadev.interactivechatbot.dao.User;
import com.hmzadev.interactivechatbot.services.ChatSessionService;
import com.hmzadev.interactivechatbot.services.MessageService;
import com.hmzadev.interactivechatbot.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/chat")
public class ChatRestController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private UserService userService;

    private final String rasaUrl = "http://localhost:5005/webhooks/rest/webhook";


    @GetMapping("/ask")
    public ResponseEntity<String> askQuestion(@RequestParam String question, @RequestParam String email) {
        if (question.isEmpty()) {
            return ResponseEntity.badRequest().body("Question cannot be empty.");
        }

        User user = userService.findByEmail(email).orElse(null);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        ChatSession chatSession = getOrCreateChatSession(user, email);
        if (chatSession == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create or retrieve chat session.");
        }

        try {
            String botResponse = forwardQuestionToRasaBot(question);
            if (botResponse == null) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No response from Rasa bot.");
            }

            saveBotResponse(chatSession, botResponse);
            return ResponseEntity.ok(botResponse);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred: " + e.getMessage());
        }
    }


    private ChatSession getOrCreateChatSession(User user, String email) {
        Optional<ChatSession> existingSession = chatSessionService.getChatSessionsByUserEmail(email)
                .stream()
                .findFirst();

        return existingSession.orElseGet(() -> {
            ChatSession newSession = new ChatSession();
            newSession.setUser(user);
            newSession.setCreatedAt(LocalDateTime.now());
            return chatSessionService.createChatSession(newSession, email);
        });
    }

    private String forwardQuestionToRasaBot(String question) {
        String payload = "{\"sender\": \"user123\", \"message\":\"" + question + "\"}";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(rasaUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            return null;  // Return null to indicate a failure to communicate with the Rasa bot
        }
    }

    private void saveBotResponse(ChatSession chatSession, String botResponse) {
        Message message = new Message(chatSession, botResponse, LocalDateTime.now(), SenderType.BOT);
        messageService.saveMessage(message);
    }


}
