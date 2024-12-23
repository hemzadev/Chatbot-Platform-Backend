package com.hmzadev.interactivechatbot.repositories;

import com.hmzadev.interactivechatbot.dao.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByUserEmail(String email);
    List<ChatSession> findByUserUsername(String username);
    Optional<ChatSession> findByName(String name);
    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(c.name, LENGTH('ChatSession: generated ') + 1) AS int)), 0) " +
            "FROM ChatSession c WHERE c.name LIKE 'ChatSession: generated %'")
    Integer findMaxSessionNumber();

    List<ChatSession> findByUserUsernameAndNameContainingIgnoreCase(String username, String name);

}
