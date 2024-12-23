    package com.hmzadev.interactivechatbot.web;
    
    import com.fasterxml.jackson.databind.JsonNode;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.hmzadev.interactivechatbot.configuration.JwtService;
    import com.hmzadev.interactivechatbot.dao.*;
    import com.hmzadev.interactivechatbot.services.ChatSessionService;
    import com.hmzadev.interactivechatbot.services.MessageService;
    import com.hmzadev.interactivechatbot.services.UserService;
    import jakarta.servlet.http.HttpServletRequest;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.http.*;
    import org.springframework.jdbc.core.JdbcTemplate;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.client.RestTemplate;
    
    import java.time.DayOfWeek;
    import java.time.LocalDate;
    import java.time.LocalDateTime;
    import java.time.format.DateTimeFormatter;
    import java.time.temporal.TemporalAdjusters;
    import java.util.*;
    import java.util.stream.Collectors;
    
    @RestController
    @RequestMapping("/chat-sessions")
    public class ChatSessionRestController {
        @Autowired
        private ChatSessionService chatSessionService;
    
        @Autowired
        private MessageService messageService;
    
        @Autowired
        private UserService userService;
    
        @Autowired
        private JwtService jwtService;
    
        @Autowired
        private RestTemplate restTemplate;
    
        @Autowired
        private JdbcTemplate jdbcTemplate;
    
        private final String rasaUrl = "http://localhost:5005/webhooks/rest/webhook";
    
        // Extract email from JWT token
        private String getUserEmailFromToken(HttpServletRequest request) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                return jwtService.extractUsername(token);
            }
            return null;
        }
    
        @PostMapping("/create")
        public ResponseEntity<String> createChatSession(HttpServletRequest request, @RequestBody CreateSessionRequest createSessionRequest) {
            String username = jwtService.getUserUsernameFromToken(request);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
    
            try {
                User user = userService.findByUsername(username).orElseThrow(() -> new RuntimeException("User not found"));
                // Use the updated service method to create the session
                ChatSession chatSession = chatSessionService.createChatSessionIfNotExists(user, createSessionRequest.getSessionName());
                return ResponseEntity.ok().body("{\"id\": " + chatSession.getId() + ", \"name\": \"" + chatSession.getName() + "\"}");
            } catch (RuntimeException e) {
                // Handle the case where a session with the same name already exists
                return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred: " + e.getMessage());
            }
        }
    
    
        @PostMapping("/messages/add")
        public ResponseEntity<String> addMessageToChatSession(HttpServletRequest request,
                                                              @RequestParam(required = false) Long sessionId,
                                                              @RequestParam String content) {
            String username = jwtService.getUserUsernameFromToken(request);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
    
            try {
                ChatSession chatSession;
    
                if (sessionId == null) {
                    // Create a new chat session if sessionId is not provided
                    int sessionNumber = chatSessionService.getNextSessionNumber();
                    String generatedSessionName = "ChatSession: generated " + sessionNumber;
    
                    User user = userService.findByUsername(username).orElseThrow(() ->
                            new RuntimeException("User not found"));
    
                    chatSession = new ChatSession();
                    chatSession.setUser(user);
                    chatSession.setCreatedAt(LocalDateTime.now());
                    chatSession.setName(generatedSessionName);
    
                    // Create the new chat session in the database
                    chatSession = chatSessionService.createChatSession(chatSession, username);
                } else {
                    // If sessionId is provided, look for the session
                    Optional<ChatSession> chatSessionOpt = chatSessionService.getChatSessionById(sessionId);
                    if (chatSessionOpt.isEmpty()) {
                        return ResponseEntity.badRequest().body("Chat session not found");
                    }
    
                    chatSession = chatSessionOpt.get();
                    if (!chatSession.getUser().getUsername().equals(username)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized to access this chat session");
                    }
                }
    
                // Add the message to the session
                Message message = new Message(chatSession, content, LocalDateTime.now(), SenderType.USER);
                messageService.saveMessage(message);
    
                // Forward the message to the Rasa bot
                String botResponse = forwardQuestionToRasaBot(content);
                if (botResponse == null) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No response from Rasa bot.");
                }
    
                // Save the bot response as a message
                saveBotResponse(chatSession, extractAnswerFromResponse(botResponse));
    
                return ResponseEntity.ok("Message added successfully. Bot response: " + extractAnswerFromResponse(botResponse));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred: " + e.getMessage());
            }
        }
    
    
    
    
    
        // Categorize chat sessions by date ranges
        @GetMapping("/categorized")
        public ResponseEntity<Map<String, List<ChatSession>>> getCategorizedChatSessions(HttpServletRequest request) {
            String username = jwtService.getUserUsernameFromToken(request);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
    
            try {
                List<ChatSession> chatSessions = chatSessionService.getChatSessionsByUserUsername(username);
    
                if (chatSessions.isEmpty()) {
                    return ResponseEntity.noContent().build();
                }
    
                LocalDate today = LocalDate.now();
                LocalDate yesterday = today.minusDays(1);
                LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate thirtyDaysAgo = today.minusDays(30);
    
                Map<String, List<ChatSession>> categorizedSessions = chatSessions.stream()
                        .collect(Collectors.groupingBy(chatSession -> {
                            LocalDate createdAt = chatSession.getCreatedAt().toLocalDate();
    
                            if (createdAt.equals(today)) {
                                return "Today";
                            } else if (createdAt.equals(yesterday)) {
                                return "Yesterday";
                            } else if (!createdAt.isBefore(startOfWeek)) {
                                return "This Week";
                            } else if (!createdAt.isBefore(thirtyDaysAgo)) {
                                return "Last 30 Days";
                            } else {
                                return "Older";
                            }
                        }));
    
                return ResponseEntity.ok(categorizedSessions);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }
        }
    
        // Search for chat sessions by name or other details
        @GetMapping("/search")
        public ResponseEntity<List<ChatSession>> searchChatSessions(HttpServletRequest request, @RequestParam String query) {
            String username = jwtService.getUserUsernameFromToken(request);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }
    
            try {
                List<ChatSession> chatSessions = chatSessionService.searchChatSessionsByQuery(username, query);
    
                if (chatSessions.isEmpty()) {
                    return ResponseEntity.noContent().build();  // No content found for the search
                }
    
                return ResponseEntity.ok(chatSessions);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(null);
            }
        }
    
    
        @GetMapping("/{sessionId}/messages")
        public ResponseEntity<Map<String, Object>> getMessagesByChatSession(HttpServletRequest request, @PathVariable Long sessionId) {
            String username = jwtService.getUserUsernameFromToken(request);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("status", "error", "message", "Unauthorized"));
            }
    
            try {
                Optional<ChatSession> chatSessionOpt = chatSessionService.getChatSessionById(sessionId);
                if (chatSessionOpt.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("status", "error", "message", "Chat session not found"));
                }
    
                ChatSession chatSession = chatSessionOpt.get();
                if (!chatSession.getUser().getUsername().equals(username)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN)
                            .body(Map.of("status", "error", "message", "You are not authorized to access this chat session"));
                }
    
                // Retrieve messages for the chat session
                List<Message> messages = messageService.getMessagesBySessionId(sessionId);
                if (messages.isEmpty()) {
                    return ResponseEntity.status(HttpStatus.NO_CONTENT)
                            .body(Map.of("status", "success", "message", "No messages found in this session"));
                }
    
                Map<LocalDate, List<Object>> messagesByDate = new HashMap<>();
    
                for (Message message : messages) {
                    LocalDate messageDate = message.getTimestamp().toLocalDate();
    
                    // Ensure messages are grouped by date
                    if (!messagesByDate.containsKey(messageDate)) {
                        messagesByDate.put(messageDate, new ArrayList<>());
                    }
    
                    // Add user or bot message
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("content", message.getContent());
                    messageMap.put("sender", message.getSender().name());
                    messageMap.put("timestamp", message.getTimestamp().toString());
    
                    // If it's a bot message, attempt to extract and execute the query
                    if (message.getSender() == SenderType.BOT) {
                        String sqlQuery = extractAnswerFromResponse(message.getContent());
                        if (sqlQuery != null && !sqlQuery.isEmpty()) {
                            List<Map<String, Object>> queryResult = executeQuery(sqlQuery);
                            messageMap.put("queryResult", queryResult != null ? queryResult : "No data returned from query");
                        }
                    }
    
                    // Add the message to the date group
                    messagesByDate.get(messageDate).add(messageMap);
                }
    
                // Prepare response structure with grouped messages
                List<Object> messageList = new ArrayList<>();
                for (Map.Entry<LocalDate, List<Object>> entry : messagesByDate.entrySet()) {
                    Map<String, Object> dateLabel = new HashMap<>();
                    dateLabel.put("date", entry.getKey().toString());
                    messageList.add(dateLabel);  // Add date label
    
                    messageList.addAll(entry.getValue());  // Add messages for this date
                }
    
                return ResponseEntity.ok(Map.of("status", "success", "messages", messageList));
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "error", "message", "An unexpected error occurred: " + e.getMessage()));
            }
        }
    
    
    
    
    
        @GetMapping("/ask")
        public ResponseEntity<?> askQuestion(HttpServletRequest request, @RequestParam String question,
                                             @RequestParam(required = false) Long sessionId) {
            String username = jwtService.getUserUsernameFromToken(request);
            if (username == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
            }
    
            if (question.isEmpty()) {
                return ResponseEntity.badRequest().body("Question cannot be empty.");
            }
    
            try {
                User user = userService.findByUsername(username).orElse(null);
                if (user == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
                }
    
                // Step 1: Retrieve the chat session by ID if provided
                ChatSession chatSession = null;
                if (sessionId != null) {
                    Optional<ChatSession> chatSessionOpt = chatSessionService.getChatSessionById(sessionId);
                    if (chatSessionOpt.isEmpty() || !chatSessionOpt.get().getUser().getUsername().equals(username)) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to access this chat session");
                    }
                    chatSession = chatSessionOpt.get();
                } else {
                    // If no sessionId is provided, you may want to create a new session or handle accordingly
                    chatSession = getOrCreateChatSession(user, username, null);
                }
    
                // Step 2: Save the user's question as a message in the chat session
                Message userMessage = new Message(chatSession, question, LocalDateTime.now(), SenderType.USER);
                messageService.saveMessage(userMessage);
    
                // Step 3: Forward the user's question to Rasa bot and get the SQL query
                String botResponse = forwardQuestionToRasaBot(question);
                if (botResponse == null) {
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("No response from Rasa bot.");
                }
    
                // Step 4: Extract the SQL query from the Rasa bot response
                String sqlQuery = extractAnswerFromResponse(botResponse);
                if (sqlQuery == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to extract SQL query from bot response.");
                }
    
                // Step 5: Execute the SQL query and retrieve the result
                List<Map<String, Object>> queryResult = executeQuery(sqlQuery);
                if (queryResult == null || queryResult.isEmpty()) {
                    return ResponseEntity.ok("Query executed successfully, but no data was returned.");
                }
    
                // Step 6: Save the bot's response as a message in the chat session
                saveBotResponse(chatSession, botResponse);
    
                // Step 7: Return the result to the frontend for table formatting
                return ResponseEntity.ok(queryResult); // Frontend will format the JSON result into a table
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("An unexpected error occurred: " + e.getMessage());
            }
        }

        public List<Map<String, Object>> executeQuery(String sqlQuery) {
            try {
                // Execute the query and return the result as a list of key-value pairs (column name -> value)
                return jdbcTemplate.queryForList(sqlQuery);
            } catch (Exception e) {
                e.printStackTrace(); // Log the exception for debugging
                return null; // Return null or handle error appropriately
            }
        }
    
        private String extractAnswerFromResponse(String botResponse) {
            // Parse the JSON response to extract the SQL query
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode responseNode = objectMapper.readTree(botResponse);
                if (responseNode.isArray() && responseNode.size() > 0) {
                    // Assuming the answer is in the 'text' field of the first object
                    JsonNode firstResponse = responseNode.get(0);
                    String fullResponseText = firstResponse.get("text").asText();
                    // Extract the SQL query from the response
                    // You can customize the regex as needed to match your specific response format
                    String sqlQuery = fullResponseText.replaceAll(".*?query: (.+)", "$1").trim();
                    return sqlQuery;  // Return just the SQL query
                }
            } catch (Exception e) {
                e.printStackTrace(); // Log the error for debugging
            }
            return null; // Return null if unable to extract answer
        }
    
    
    
        private ChatSession getOrCreateChatSession(User user, String username, String sessionName) {
            Optional<ChatSession> existingSession = chatSessionService.getChatSessionsByUserUsername(username)
                    .stream()
                    .findFirst();
    
            return existingSession.orElseGet(() -> {
                ChatSession newSession = new ChatSession();
                newSession.setUser(user);
                newSession.setCreatedAt(LocalDateTime.now());
                newSession.setName(sessionName != null ? sessionName : "New Session");
                return chatSessionService.createChatSession(newSession, username);
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