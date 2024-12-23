package com.hmzadev.interactivechatbot.dao;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Data
public class    Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "chat_session_id", nullable = false)
    private ChatSession chatSession;

    @Column(nullable = false)
    private String content;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)  // Store enum as a string in the DB
    @Column(nullable = false)
    private SenderType sender;  // Enum for sender (USER or BOT)

    // Default constructor
    public Message() {}

    // Constructor for convenience
    public Message(ChatSession chatSession, String content, LocalDateTime timestamp, SenderType sender) {
        this.chatSession = chatSession;
        this.content = content;
        this.timestamp = timestamp;
        this.sender = sender;
    }
}
