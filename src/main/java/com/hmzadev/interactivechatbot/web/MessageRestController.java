    package com.hmzadev.interactivechatbot.web;

    import com.hmzadev.interactivechatbot.configuration.JwtService;
    import com.hmzadev.interactivechatbot.dao.ChatSession;
    import com.hmzadev.interactivechatbot.dao.Message;
    import com.hmzadev.interactivechatbot.dao.SenderType;
    import com.hmzadev.interactivechatbot.services.ChatSessionService;
    import com.hmzadev.interactivechatbot.services.MessageService;
    import jakarta.servlet.http.HttpServletRequest;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.Optional;

    @RestController
    @RequestMapping("/messages")
    public class MessageRestController {

        @Autowired
        private MessageService messageService;

        @Autowired
        private ChatSessionService chatSessionService;

        @Autowired
        private JwtService jwtService;

        @PostMapping("/add")
        public ResponseEntity<String> addMessage(
                @RequestParam Long chatSessionId,
                @RequestParam String content,
                HttpServletRequest request) {
            String username = jwtService.getUserUsernameFromToken(request);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }

            try {
                Optional<ChatSession> chatSessionOpt = chatSessionService.getChatSessionById(chatSessionId);
                if (chatSessionOpt.isEmpty()) {
                    return ResponseEntity.badRequest().body("Chat session not found");
                }

                ChatSession chatSession = chatSessionOpt.get();
                // Ensure the user has access to the chat session (optional step)
                if (!chatSession.getUser().getUsername().equals(username)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to access this chat session");
                }

                Message message = new Message(chatSession, content, LocalDateTime.now(), SenderType.USER);
                messageService.saveMessage(message);

                return ResponseEntity.ok("Message added successfully");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred: " + e.getMessage());
            }
        }


        // Endpoint to retrieve messages by chat session ID
        @GetMapping("/session/{sessionId}")
        public ResponseEntity<List<Message>> getMessagesBySession(@PathVariable Long sessionId) {
            try {
                List<Message> messages = messageService.getMessagesBySessionId(sessionId);
                if (messages.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }
                return ResponseEntity.ok(messages);
            } catch (Exception e) {
                return ResponseEntity.status(500).body(null);
            }
        }
    }
