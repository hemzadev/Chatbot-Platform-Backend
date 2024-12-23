package com.hmzadev.interactivechatbot.services;

import com.hmzadev.interactivechatbot.dao.Message;
import com.hmzadev.interactivechatbot.repositories.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class MessageService {
    @Autowired
    private MessageRepository messageRepository;

    public Message saveMessage(Message message) {
        return messageRepository.save(message);
    }

    public List<Message> getMessagesBySessionId(Long sessionId) {
        return messageRepository.findByChatSessionId(sessionId);
    }
}
