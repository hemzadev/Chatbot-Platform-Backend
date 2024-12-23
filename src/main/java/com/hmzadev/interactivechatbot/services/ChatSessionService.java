package com.hmzadev.interactivechatbot.services;

import com.hmzadev.interactivechatbot.dao.ChatSession;
import com.hmzadev.interactivechatbot.dao.Message;
import com.hmzadev.interactivechatbot.dao.User;
import com.hmzadev.interactivechatbot.repositories.ChatSessionRepository;
import com.hmzadev.interactivechatbot.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChatSessionService {
    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserService userService;

    public List<ChatSession> getChatSessionsByUserEmail(String email) {
        return chatSessionRepository.findByUserEmail(email);
    }

    public List<ChatSession> getChatSessionsByUserUsername(String username) {
        return chatSessionRepository.findByUserUsername(username);  // Uses the repository method
    }

    public Optional<ChatSession> getChatSessionById(Long id) {
        return chatSessionRepository.findById(id);
    }

    public List<Message> getMessagesByChatSession(ChatSession chatSession) {
        return messageRepository.findByChatSession(chatSession);
    }
    public ChatSession createChatSession(ChatSession chatSession, String username) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found with username: " + username));

        chatSession.setUser(user);
        return chatSessionRepository.save(chatSession);
    }
    public List<ChatSession> searchChatSessionsByQuery(String username, String query) {
        // Here you can extend this to search by more fields like content or date
        return chatSessionRepository.findByUserUsernameAndNameContainingIgnoreCase(username, query);
    }



    public Message addMessageToChatSession(ChatSession chatSession, String content) {
        Message message = new Message();
        message.setChatSession(chatSession);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        return messageRepository.save(message);
    }

    // Method to get the next session number by checking the highest session number used
    public int getNextSessionNumber() {
        Integer maxSessionNumber = chatSessionRepository.findMaxSessionNumber();
        if (maxSessionNumber == null) {
            return 1; // Start with 1 if no sessions exist
        } else {
            return maxSessionNumber + 1;
        }
    }
    public ChatSession createChatSessionIfNotExists(User user, String sessionName) {
        // Check if a session with the same name already exists
        if (chatSessionRepository.findByName(sessionName).isPresent()) {
            throw new RuntimeException("A chat session with this name already exists");
        }

        // Create a new session if none exists
        ChatSession newSession = new ChatSession();
        newSession.setUser(user);
        newSession.setName(sessionName);
        newSession.setCreatedAt(LocalDateTime.now());  // Set creation date if necessary

        // Save the new session to the repository
        return chatSessionRepository.save(newSession);
    }
}