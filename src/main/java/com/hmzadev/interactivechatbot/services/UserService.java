package com.hmzadev.interactivechatbot.services;

import com.hmzadev.interactivechatbot.dao.User;
import com.hmzadev.interactivechatbot.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(userRepository.findByEmail(email));  // Assuming repository returns User or null
    }


    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    public User save(User user) {
        return userRepository.save(user);
    }
}
