package com.hmzadev.interactivechatbot.repositories;

import com.hmzadev.interactivechatbot.dao.ChatSession;
import com.hmzadev.interactivechatbot.dao.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByChatSessionId(Long sessionId);
    List<Message> findByChatSession(ChatSession chatSession);
}
